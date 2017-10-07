/**
 * Copyright (C) 2017 Mikhail Frolov
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.truenight.rxinapps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;

import com.android.vending.billing.IInAppBillingService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.functions.Action;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import xyz.truenight.rxinapps.exception.ProductNotFoundException;
import xyz.truenight.rxinapps.model.ProductType;
import xyz.truenight.rxinapps.model.Purchase;
import xyz.truenight.rxinapps.model.SkuDetails;
import xyz.truenight.rxinapps.util.Constants;
import xyz.truenight.rxinapps.util.GsonParser;
import xyz.truenight.rxinapps.util.HawkStorage;
import xyz.truenight.rxinapps.util.Parser;
import xyz.truenight.rxinapps.util.Security;
import xyz.truenight.rxinapps.util.Storage;
import xyz.truenight.utils.Utils;

public class RxInApps extends ContextHolder {

    public static final String TAG = RxInApps.class.getSimpleName();
    public static final String VERSION = "v1";
    private static final String LAST_LOAD = ":LAST_LOAD";

    private static final Date DATE_MERCHANT_LIMIT_1 = new Date(2012, 12, 5); //5th December 2012
    private static final Date DATE_MERCHANT_LIMIT_2 = new Date(2015, 7, 20); //21st July 2015

    private static String licenseKey;
    private static String merchantId;
    private static RxInApps instance;

    public static void init(Builder builder) {
        instance = new RxInApps(builder);
        licenseKey = builder.getLicenseKey();
        merchantId = builder.getMerchantId();
    }

    public static synchronized RxInApps with(Context context) {
        if (instance == null) {
            instance = new RxInApps(new Builder(context));
        }
        return instance;
    }

    private final String packageName;
    private final Storage storage;
    private final long cacheLifetime;
    private final Parser parser;

    private final AtomicReference<SingleEmitter<Purchase>> purchaseSubscriber = new AtomicReference<>();
    private final Observable<IInAppBillingService> connection = Observable.create(ConnectionOnSubscribe.create(RxInApps.this))
            .timeout(10, TimeUnit.SECONDS)
            .retry(new Predicate<Throwable>() {
                @Override
                public boolean test(Throwable throwable) throws Exception {
                    return throwable instanceof TimeoutException;
                }
            }).share();/*RxJavaPlugins.onAssembly(
            new ObservableCacheRefCount<>(
                    Observable.create(ConnectionOnSubscribe.create(RxInApps.this))
                            .timeout(10, TimeUnit.SECONDS)
                            .retry()
                            .publish()
            )
    );*/

    private RxInApps(Builder builder) {
        super(builder.getContext());
        this.cacheLifetime = builder.getCacheLifetime();
        this.parser = builder.getParser();
        this.packageName = getContext().getApplicationContext().getPackageName();

        this.storage = builder.getStorage();
    }

    Parser getParser() {
        return parser;
    }

    Storage getStorage() {
        return storage;
    }

    /**
     * Checks availability of InAppBillingService
     */
    public static boolean isIabServiceAvailable(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(Constants.BINDING_INTENT_VALUE);
        List<ResolveInfo> list = packageManager.queryIntentServices(intent, 0);
        return list.size() > 0;
    }

    /**
     * Verifies purchase by license key and merchant id
     */
    public static boolean isValid(Purchase purchase) {
        return verifyPurchaseSignature(purchase.getProductId(),
                purchase.getRawResponse(),
                purchase.getPurchaseSignature()) && checkMerchantTransactionDetails(purchase);
    }

    /**
     * Checks license key validity
     * <p>
     * If license key was not supplied function checks nothing
     */
    static boolean verifyPurchaseSignature(String productId, String purchaseData, String dataSignature) {
        try {
            /*
             * Skip the signature check if the provided License Key is NULL and return true in order to
             * continue the purchase flow
             */
            return Utils.isEmpty(licenseKey) || Security.verifyPurchase(productId, licenseKey, purchaseData, dataSignature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks merchant's id validity. If purchase was generated by Freedom alike program it doesn't know
     * real merchant id, unless publisher GoogleId was hacked
     * <p>
     * If merchantId was not supplied function checks nothing
     */
    static boolean checkMerchantTransactionDetails(Purchase details) {
        if (RxInApps.merchantId == null) {//omit merchant id checking
            return true;
        }
        if (details.getPurchaseTime().before(DATE_MERCHANT_LIMIT_1)) {//new format [merchantId].[orderId] applied or not?
            return true;
        }
        if (details.getPurchaseTime().after(DATE_MERCHANT_LIMIT_2)) {//newest format applied
            return true;
        }
        if (details.getOrderId() == null || details.getOrderId().trim().length() == 0) {
            return false;
        }
        int index = details.getOrderId().indexOf('.');
        if (index <= 0) {
            return false; //protect on missing merchant id
        }
        //extract merchant id
        String merchantId = details.getOrderId().substring(0, index);
        return merchantId.compareTo(RxInApps.merchantId) == 0;
    }

    /**
     * Observable which emits InAppBillingService while this service is bound
     */
    public Single<IInAppBillingService> initialization() {
        // cache instance during timeout
        return connection
                .firstOrError();
    }

    Single<List<Purchase>> loadPurchasesByType(final String productType) {
        return initialization()
                .flatMap(new Function<IInAppBillingService, SingleSource<? extends List<Purchase>>>() {
                    @Override
                    public SingleSource<? extends List<Purchase>> apply(IInAppBillingService billingService) throws Exception {
                        return loadPurchasesByType(billingService, productType);
                    }
                });
    }

    Single<List<Purchase>> loadPurchasesByType(IInAppBillingService billingService, String productType) {
        return Single.create(PurchasedOnSubscribe.create(billingService, packageName, parser, productType));
    }

    private Single<Map<String, Purchase>> purchasesByTypeMap(final String productType) {
        return Single.defer(new Callable<SingleSource<? extends Map<String, Purchase>>>() {
            @Override
            public SingleSource<? extends Map<String, Purchase>> call() throws Exception {
                Long lastLoad = storage.get(productType + LAST_LOAD);
                long delta = Utils.safe(lastLoad) + cacheLifetime - System.currentTimeMillis();
                if (delta >= 0 && delta <= cacheLifetime) {
                    Map<String, Purchase> map = storage.get(productType);
                    if (map != null) {
                        return Single.just(map);
                    }
                }
                return loadPurchasesByType(productType)
                        .compose(toMapAndCache(productType));
            }
        });
    }

    @NonNull
    SingleTransformer<List<Purchase>, Map<String, Purchase>> toMapAndCache(final String productType) {
        return new SingleTransformer<List<Purchase>, Map<String, Purchase>>() {
            @Override
            public SingleSource<Map<String, Purchase>> apply(Single<List<Purchase>> upstream) {
                return upstream
                        .flatMapObservable(new Function<List<Purchase>, ObservableSource<Purchase>>() {
                            @Override
                            public ObservableSource<Purchase> apply(List<Purchase> purchases) throws Exception {
                                return Observable.fromIterable(purchases);
                            }
                        })
                        .toMap(new Function<Purchase, String>() {
                            @Override
                            public String apply(Purchase purchase) throws Exception {
                                return purchase.getProductId();
                            }
                        })
                        .doOnSuccess(new Consumer<Map<String, Purchase>>() {
                            @Override
                            public void accept(Map<String, Purchase> map) throws Exception {
                                storage.put(productType, map);
                                storage.put(productType + LAST_LOAD, System.currentTimeMillis());
                            }
                        });
            }
        };
    }

    void putPurchaseToCache(Purchase purchase, String productType) {
        Map<String, Purchase> map = storage.get(productType);
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(purchase.getProductId(), purchase);
        storage.put(productType, map);
    }

    void removePurchaseFromCache(String productId, String productType) {
        Map<String, Purchase> map = storage.get(productType);
        if (map != null) {
            map.remove(productId);
            storage.put(productType, map);
        }
    }

    boolean checkPurchaseSubscriber() {
        return RxUtils.isDisposed(Utils.unwrap(purchaseSubscriber));
    }

    private Single<Purchase> purchase(final String productId, final String productType) {
        return Single.defer(new Callable<SingleSource<? extends Purchase>>() {
            @Override
            public SingleSource<? extends Purchase> call() throws Exception {
                if (Utils.isEmpty(productId)) {
                    return Single.error(new IllegalArgumentException("Product id can't be empty"));
                }
                return initialization()
                        .flatMap(new Function<IInAppBillingService, SingleSource<? extends Purchase>>() {
                            @Override
                            public SingleSource<? extends Purchase> apply(IInAppBillingService billingService) throws Exception {
                                return Single.create(
                                        PurchaseOnSubscribe.create(RxInApps.this,
                                                billingService,
                                                packageName,
                                                productId,
                                                productType,
                                                purchaseSubscriber)
                                );
                            }
                        })
                        .doOnDispose(new Action() {
                            @Override
                            public void run() throws Exception {
                                purchaseSubscriber.set(null);
                            }
                        });
            }
        });
    }

    boolean bindPurchaseUnsubscribe(Cancellable cancellable) {
        return RxUtils.setOnDispose(Utils.unwrap(purchaseSubscriber), cancellable);
    }

    boolean deliverPurchaseResult(Purchase purchase) {
        return RxUtils.onSuccess(Utils.unwrap(purchaseSubscriber), purchase);
    }

    boolean deliverPurchaseError(Throwable th) {
        return RxUtils.onError(Utils.unwrap(purchaseSubscriber), th);
    }

    /**
     * Returns {@link Observable} which emits {@link SkuDetails} of specified type
     */
    private Single<SkuDetails> getSkuDetails(final String productId, final String productType) {
        return getSkuDetails(Collections.singletonList(productId), productType)
                .map(new Function<List<SkuDetails>, SkuDetails>() {
                    @Override
                    public SkuDetails apply(List<SkuDetails> skuDetails) throws Exception {
                        return Utils.first(skuDetails);
                    }
                });
    }

    /**
     * Returns {@link Observable} which emits list of {@link SkuDetails} of specified type
     */
    private Single<List<SkuDetails>> getSkuDetails(final List<String> productIdList, final String productType) {
        return Single.defer(new Callable<SingleSource<? extends List<SkuDetails>>>() {
            @Override
            public SingleSource<? extends List<SkuDetails>> call() throws Exception {
                if (Utils.isEmpty(productIdList)) {
                    return Single.error(new NullPointerException("Product id list can't be empty"));
                }
                return initialization()
                        .flatMap(new Function<IInAppBillingService, SingleSource<List<SkuDetails>>>() {
                            @Override
                            public SingleSource<List<SkuDetails>> apply(IInAppBillingService billingService) throws Exception {
                                return Single.create(new SkuDetailsOnSubscribe(billingService, packageName, parser, productIdList, productType));
                            }
                        });
            }
        });
    }

    /*
     * ----------------------------------------- PRODUCTS ------------------------------------------
     */

    /**
     * Simply loads purchased products and emits them
     */
    public Single<List<Purchase>> loadPurchasedProducts() {
        return loadPurchasesByType(ProductType.MANAGED);
    }

    /**
     * Loads or takes from cache purchased products and maps them by product id
     */
    public Single<Map<String, Purchase>> purchasedProductsMap() {
        return purchasesByTypeMap(ProductType.MANAGED);
    }

    /**
     * Loads or takes from cache purchased products and emits them as {@link List}
     */
    public Single<List<Purchase>> purchasedProducts() {
        return purchasedProductsMap()
                .map(new Function<Map<String, Purchase>, List<Purchase>>() {
                    @Override
                    public List<Purchase> apply(Map<String, Purchase> map) throws Exception {
                        return new ArrayList<>(map.values());
                    }
                });
    }

    /**
     * Loads or takes from cache purchased product's ids and emits them
     */
    public Single<List<String>> purchasedProductIds() {
        return purchasedProductsMap()
                .map(new Function<Map<String, Purchase>, List<String>>() {
                    @Override
                    public List<String> apply(Map<String, Purchase> map) throws Exception {
                        return new ArrayList<>(map.keySet());
                    }
                });
    }

    /**
     * Emits whether product with specified id purchased or not
     */
    public Single<Boolean> isPurchased(final String productId) {
        return purchasedProductsMap()
                .map(new Function<Map<String, Purchase>, Boolean>() {
                    @Override
                    public Boolean apply(Map<String, Purchase> map) throws Exception {
                        return map.containsKey(productId);
                    }
                });
    }

    /**
     * Emits purchased product with specified id OR error if purchase not found
     */
    public Single<Purchase> getPurchasedProduct(final String productId) {
        return purchasedProductsMap()
                .map(new Function<Map<String, Purchase>, Purchase>() {
                    @Override
                    public Purchase apply(Map<String, Purchase> map) throws Exception {
                        if (map.containsKey(productId)) {
                            return map.get(productId);
                        } else {
                            throw new ProductNotFoundException(String.format("Product \"%s\" not found!", productId));
                        }
                    }
                });
    }

    /**
     * Starts purchase managed product flow
     */
    public Single<Purchase> purchase(String productId) {
        return purchase(productId, ProductType.MANAGED);
    }

    /**
     * Consume purchased product
     */
    public Single<Purchase> consume(final String productId) {
        return Single.defer(new Callable<SingleSource<? extends Purchase>>() {
            @Override
            public SingleSource<? extends Purchase> call() throws Exception {
                if (Utils.isEmpty(productId)) {
                    return Single.error(new IllegalArgumentException("Product id can't be empty"));
                }
                return initialization()
                        .flatMap(new Function<IInAppBillingService, SingleSource<Purchase>>() {
                            @Override
                            public SingleSource<Purchase> apply(final IInAppBillingService billingService) throws Exception {
                                return loadPurchasesByType(billingService, ProductType.MANAGED)
                                        .compose(toMapAndCache(ProductType.MANAGED))
                                        .flatMap(new Function<Map<String, Purchase>, SingleSource<Purchase>>() {
                                            @Override
                                            public SingleSource<Purchase> apply(Map<String, Purchase> map) throws Exception {
                                                return Single.create(
                                                        ConsumePurchaseOnSubscribe.create(
                                                                RxInApps.this,
                                                                billingService,
                                                                packageName,
                                                                map,
                                                                productId
                                                        )
                                                );
                                            }
                                        });
                            }
                        });
            }
        });
    }

    /**
     * Emits {@link SkuDetails} of managed product for specified product id
     */
    public Single<SkuDetails> getProduct(String productId) {
        return getSkuDetails(productId, ProductType.MANAGED);
    }

    /**
     * Emits list {@link SkuDetails} of managed products for specified product ids
     */
    public Single<List<SkuDetails>> getProducts(List<String> productIdList) {
        return getSkuDetails(productIdList, ProductType.MANAGED);
    }

    /*
     * --------------------------------------- SUBSCRIPTIONS ---------------------------------------
     */

    /**
     * Simply loads purchased subscriptions and emits them
     */
    public Single<List<Purchase>> loadPurchasedSubscriptions() {
        return loadPurchasesByType(ProductType.SUBSCRIPTION);
    }

    /**
     * Loads or takes from cache purchased subscriptions and maps them by product id
     */
    public Single<Map<String, Purchase>> purchasedSubscriptionsMap() {
        return purchasesByTypeMap(ProductType.SUBSCRIPTION);
    }

    /**
     * Loads or takes from cache purchased subscriptions and emits them as {@link List}
     */
    public Single<List<Purchase>> purchasedSubscriptions() {
        return purchasedSubscriptionsMap()
                .map(new Function<Map<String, Purchase>, List<Purchase>>() {
                    @Override
                    public List<Purchase> apply(Map<String, Purchase> map) throws Exception {
                        return new ArrayList<>(map.values());
                    }
                });
    }

    /**
     * Loads or takes from cache purchased subscription's ids and emits them
     */
    public Single<List<String>> purchasedSubscriptionIds() {
        return purchasedSubscriptionsMap()
                .map(new Function<Map<String, Purchase>, List<String>>() {
                    @Override
                    public List<String> apply(Map<String, Purchase> map) throws Exception {
                        return new ArrayList<>(map.keySet());
                    }
                });
    }

    /**
     * Emits whether subscription with specified id purchased or not
     */
    public Single<Boolean> isSubscribed(final String productId) {
        return purchasedSubscriptionsMap()
                .map(new Function<Map<String, Purchase>, Boolean>() {
                    @Override
                    public Boolean apply(Map<String, Purchase> map) throws Exception {
                        return map.containsKey(productId);
                    }
                });
    }

    /**
     * Emits purchased subscription with specified id OR error if purchase not found
     */
    public Single<Purchase> getPurchasedSubscription(final String productId) {
        return purchasedSubscriptionsMap()
                .map(new Function<Map<String, Purchase>, Purchase>() {
                    @Override
                    public Purchase apply(Map<String, Purchase> map) throws Exception {
                        if (map.containsKey(productId)) {
                            return map.get(productId);
                        } else {
                            throw new ProductNotFoundException(String.format("Subscription \"%s\" not found!", productId));
                        }
                    }
                });
    }

    /**
     * Starts purchase subscription flow
     */
    public Single<Purchase> subscribe(String productId) {
        return purchase(productId, ProductType.SUBSCRIPTION);
    }

    /**
     * Emits {@link SkuDetails} of subscription for specified product id
     */
    public Single<SkuDetails> getSubscription(String productId) {
        return getSkuDetails(productId, ProductType.SUBSCRIPTION);
    }

    /**
     * Emits list {@link SkuDetails} of subscription for specified product's ids
     */
    public Single<List<SkuDetails>> getSubscriptions(List<String> productIdList) {
        return getSkuDetails(productIdList, ProductType.SUBSCRIPTION);
    }

    public static class Builder {
        private Context context;
        private Parser parser;
        private String licenseKey;
        private String merchantId;

        private Long cacheLifetime;
        private Storage storage;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        Context getContext() {
            return context;
        }

        Parser getParser() {
            if (parser == null) {
                return new GsonParser();
            } else {
                return parser;
            }
        }

        Storage getStorage() {
            if (storage == null) {
                return new HawkStorage(context, parser);
            } else {
                return storage;
            }
        }

        String getLicenseKey() {
            return licenseKey;
        }

        String getMerchantId() {
            return merchantId;
        }

        Long getCacheLifetime() {
            if (cacheLifetime == null) {
                return TimeUnit.MINUTES.toMillis(30);
            } else {
                return cacheLifetime;
            }
        }

        /**
         * Parser for {@link SkuDetails} and {@link Purchase} data models
         */
        public Builder parser(Parser parser) {
            this.parser = parser;
            return this;
        }

        /**
         *
         */
        public Builder storage(Storage storage) {
            this.storage = storage;
            return this;
        }

        // TODO: add log interceptor

        /**
         * Necessary for simple validation of purchases
         *
         * @param licenseKey license key
         */
        public Builder licenseKey(String licenseKey) {
            this.licenseKey = licenseKey;
            return this;
        }

        /**
         * Necessary for simple validation of purchases
         *
         * @param merchantId google merchant id
         */
        public Builder merchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        /**
         * Lifetime of purchases cache
         */
        public Builder cacheLifetime(long value, TimeUnit timeUnit) {
            this.cacheLifetime = timeUnit.toMillis(value);
            return this;
        }
    }
}
