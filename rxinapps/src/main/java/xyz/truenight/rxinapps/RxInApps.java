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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import rx.Emitter;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
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
    private final long timeout; // timeout seconds
    private final long cacheLifetime;
    private final Parser parser;
    private final ConnectionOnSubscribe connection;

    private final AtomicReference<Subscriber<? super Purchase>> purchaseSubscriber = new AtomicReference<>();

    private RxInApps(Builder builder) {
        super(builder.getContext());
        this.timeout = builder.getTimeout();
        this.cacheLifetime = builder.getCacheLifetime();
        this.parser = builder.getParser();
        this.packageName = getContext().getApplicationContext().getPackageName();

        this.connection = ConnectionOnSubscribe.create(RxInApps.this, timeout);
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
     *
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
    public Observable<IInAppBillingService> initialization() {
        // cache instance during timeout
        return Observable.create(connection, Emitter.BackpressureMode.NONE)
                .take(timeout, TimeUnit.MILLISECONDS)
                .first();
    }

    Observable<List<Purchase>> loadPurchasesByType(final String productType) {
        return initialization()
                .flatMap(new Func1<IInAppBillingService, Observable<List<Purchase>>>() {
                    @Override
                    public Observable<List<Purchase>> call(final IInAppBillingService billingService) {
                        return loadPurchasesByType(billingService, productType);
                    }
                });
    }

    Observable<List<Purchase>> loadPurchasesByType(IInAppBillingService billingService, String productType) {

        return Observable.create(PurchasedOnSubscribe.create(billingService, packageName, parser, productType),
                Emitter.BackpressureMode.NONE);
    }

    private Observable<Map<String, Purchase>> purchasesByTypeMap(final String productType) {
        return Observable.defer(new Func0<Observable<Map<String, Purchase>>>() {
            @Override
            public Observable<Map<String, Purchase>> call() {
                Long lastLoad = storage.get(productType + LAST_LOAD);
                long delta = Utils.safe(lastLoad) + cacheLifetime - System.currentTimeMillis();
                if (delta >= 0 && delta <= cacheLifetime) {
                    Map<String, Purchase> map = storage.get(productType);
                    if (map != null) {
                        return Observable.just(map);
                    }
                }
                return loadPurchasesByType(productType)
                        .compose(toMapAndCache(productType));

            }
        });
    }

    @NonNull
    Observable.Transformer<List<Purchase>, Map<String, Purchase>> toMapAndCache(final String productType) {
        return new Observable.Transformer<List<Purchase>, Map<String, Purchase>>() {
            @Override
            public Observable<Map<String, Purchase>> call(Observable<List<Purchase>> listObservable) {
                return listObservable
                        .flatMap(new Func1<List<Purchase>, Observable<Purchase>>() {
                            @Override
                            public Observable<Purchase> call(List<Purchase> purchases) {
                                return Observable.from(purchases);
                            }
                        })
                        .toMap(new Func1<Purchase, String>() {
                            @Override
                            public String call(Purchase purchases) {
                                return purchases.getProductId();
                            }
                        })
                        .doOnNext(new Action1<Map<String, Purchase>>() {
                            @Override
                            public void call(Map<String, Purchase> map) {
                                storage.put(productType, map);
                                storage.put(productType + LAST_LOAD, System.currentTimeMillis());
                            }
                        });
            }
        };
    }

    void putPurchaseToCache(Purchase purchase, String productType) {
        Map<String, Purchase> map = storage.get(productType);
        map.put(purchase.getProductId(), purchase);
        storage.put(productType, map);
    }

    void removePurchaseFromCache(String productId, String productType) {
        Map<String, Purchase> map = storage.get(productType);
        map.remove(productId);
        storage.put(productType, map);
    }

    boolean checkPurchaseSubscriber() {
        return RxUtils.isUnsubscribed(Utils.unwrap(purchaseSubscriber));
    }

    private Observable<Purchase> purchase(final String productId, final String productType) {
        return Observable.defer(new Func0<Observable<Purchase>>() {
            @Override
            public Observable<Purchase> call() {
                if (Utils.isEmpty(productId)) {
                    return Observable.error(new IllegalArgumentException("Product id can't be empty"));
                }
                return initialization()
                        .flatMap(new Func1<IInAppBillingService, Observable<Purchase>>() {
                            @Override
                            public Observable<Purchase> call(final IInAppBillingService billingService) {
                                return Observable.unsafeCreate(
                                        PurchaseOnSubscribe.create(RxInApps.this,
                                                billingService,
                                                packageName,
                                                productId,
                                                productType,
                                                purchaseSubscriber)
                                );
                            }
                        })
                        .doOnUnsubscribe(new Action0() {
                            @Override
                            public void call() {
                                purchaseSubscriber.set(null);
                            }
                        });
            }
        });
    }

    boolean bindPurchaseUnsubscribe(Subscription subscription) {
        return RxUtils.addOnUnsubscribe(Utils.unwrap(purchaseSubscriber), subscription);
    }

    boolean deliverPurchaseResult(Purchase purchase) {
        return RxUtils.publishResult(Utils.unwrap(purchaseSubscriber), purchase);
    }

    boolean deliverPurchaseError(Throwable th) {
        return RxUtils.publishError(Utils.unwrap(purchaseSubscriber), th);
    }

    /**
     * Returns {@link Observable} which emits {@link SkuDetails} of specified type
     */
    private Observable<SkuDetails> getSkuDetails(final String productId, final String productType) {
        return getSkuDetails(Collections.singletonList(productId), productType)
                .map(new Func1<List<SkuDetails>, SkuDetails>() {
                    @Override
                    public SkuDetails call(List<SkuDetails> data) {
                        return Utils.first(data);
                    }
                });
    }

    /**
     * Returns {@link Observable} which emits list of {@link SkuDetails} of specified type
     */
    private Observable<List<SkuDetails>> getSkuDetails(final List<String> productIdList, final String productType) {
        return Observable.defer(new Func0<Observable<List<SkuDetails>>>() {
            @Override
            public Observable<List<SkuDetails>> call() {
                if (Utils.isEmpty(productIdList)) {
                    return Observable.error(new NullPointerException("Product id list can't be empty"));
                }
                return initialization()
                        .flatMap(new Func1<IInAppBillingService, Observable<List<SkuDetails>>>() {
                            @Override
                            public Observable<List<SkuDetails>> call(final IInAppBillingService billingService) {
                                return Observable.create(new SkuDetailsOnSubscribe(billingService, packageName, parser, productIdList, productType),
                                        Emitter.BackpressureMode.NONE);
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
    public Observable<List<Purchase>> loadPurchasedProducts() {
        return loadPurchasesByType(ProductType.MANAGED);
    }

    /**
     * Loads or takes from cache purchased products and maps them by product id
     */
    public Observable<Map<String, Purchase>> purchasedProductsMap() {
        return purchasesByTypeMap(ProductType.MANAGED);
    }

    /**
     * Loads or takes from cache purchased products and emits them as {@link List}
     */
    public Observable<List<Purchase>> purchasedProducts() {
        return purchasedProductsMap()
                .map(new Func1<Map<String, Purchase>, List<Purchase>>() {
                    @Override
                    public List<Purchase> call(Map<String, Purchase> map) {
                        return new ArrayList<>(map.values());
                    }
                });
    }

    /**
     * Loads or takes from cache purchased product's ids and emits them
     */
    public Observable<List<String>> purchasedProductIds() {
        return purchasedProductsMap()
                .map(new Func1<Map<String, Purchase>, List<String>>() {
                    @Override
                    public List<String> call(Map<String, Purchase> map) {
                        return new ArrayList<>(map.keySet());
                    }
                });
    }

    /**
     * Emits whether product with specified id purchased or not
     */
    public Observable<Boolean> isPurchased(final String productId) {
        return purchasedProductsMap()
                .map(new Func1<Map<String, Purchase>, Boolean>() {
                    @Override
                    public Boolean call(Map<String, Purchase> data) {
                        return data.containsKey(productId);
                    }
                });
    }

    /**
     * Emits purchased product with specified id OR error if purchase not found
     */
    public Observable<Purchase> getPurchasedProduct(final String productId) {
        return purchasedProductsMap()
                .map(new Func1<Map<String, Purchase>, Purchase>() {
                    @Override
                    public Purchase call(Map<String, Purchase> data) {
                        if (data.containsKey(productId)) {
                            return data.get(productId);
                        } else {
                            throw new ProductNotFoundException(String.format("Product \"%s\" not found!", productId));
                        }
                    }
                });
    }

    /**
     * Starts purchase managed product flow
     */
    public Observable<Purchase> purchase(String productId) {
        return purchase(productId, ProductType.MANAGED);
    }

    /**
     * Consume purchased product
     */
    public Observable<Purchase> consume(final String productId) {
        return Observable.defer(new Func0<Observable<Purchase>>() {
            @Override
            public Observable<Purchase> call() {
                if (Utils.isEmpty(productId)) {
                    return Observable.error(new IllegalArgumentException("Product id can't be empty"));
                }
                return initialization()
                        .flatMap(new Func1<IInAppBillingService, Observable<Purchase>>() {
                            @Override
                            public Observable<Purchase> call(final IInAppBillingService billingService) {
                                return loadPurchasesByType(billingService, ProductType.MANAGED)
                                        .compose(toMapAndCache(ProductType.MANAGED))
                                        .flatMap(new Func1<Map<String, Purchase>, Observable<Purchase>>() {
                                            @Override
                                            public Observable<Purchase> call(final Map<String, Purchase> map) {
                                                return Observable.create(
                                                        ConsumePurchaseOnSubscribe.create(
                                                                RxInApps.this,
                                                                billingService,
                                                                packageName,
                                                                map,
                                                                productId
                                                        ),
                                                        Emitter.BackpressureMode.NONE
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
    public Observable<SkuDetails> getProduct(String productId) {
        return getSkuDetails(productId, ProductType.MANAGED);
    }

    /**
     * Emits list {@link SkuDetails} of managed products for specified product ids
     */
    public Observable<List<SkuDetails>> getProducts(List<String> productIdList) {
        return getSkuDetails(productIdList, ProductType.MANAGED);
    }

    /*
     * --------------------------------------- SUBSCRIPTIONS ---------------------------------------
     */

    /**
     * Simply loads purchased subscriptions and emits them
     */
    public Observable<List<Purchase>> loadPurchasedSubscriptions() {
        return loadPurchasesByType(ProductType.SUBSCRIPTION);
    }

    /**
     * Loads or takes from cache purchased subscriptions and maps them by product id
     */
    public Observable<Map<String, Purchase>> purchasedSubscriptionsMap() {
        return purchasesByTypeMap(ProductType.SUBSCRIPTION);
    }

    /**
     * Loads or takes from cache purchased subscriptions and emits them as {@link List}
     */
    public Observable<List<Purchase>> purchasedSubscriptions() {
        return purchasedSubscriptionsMap()
                .map(new Func1<Map<String, Purchase>, List<Purchase>>() {
                    @Override
                    public List<Purchase> call(Map<String, Purchase> map) {
                        return new ArrayList<>(map.values());
                    }
                });
    }

    /**
     * Loads or takes from cache purchased subscription's ids and emits them
     */
    public Observable<List<String>> purchasedSubscriptionIds() {
        return purchasedSubscriptionsMap()
                .map(new Func1<Map<String, Purchase>, List<String>>() {
                    @Override
                    public List<String> call(Map<String, Purchase> map) {
                        return new ArrayList<>(map.keySet());
                    }
                });
    }

    /**
     * Emits whether subscription with specified id purchased or not
     */
    public Observable<Boolean> isSubscribed(final String productId) {
        return purchasedSubscriptionsMap()
                .map(new Func1<Map<String, Purchase>, Boolean>() {
                    @Override
                    public Boolean call(Map<String, Purchase> data) {
                        return data.containsKey(productId);
                    }
                });
    }

    /**
     * Emits purchased subscription with specified id OR error if purchase not found
     */
    public Observable<Purchase> getPurchasedSubscription(final String productId) {
        return purchasedSubscriptionsMap()
                .map(new Func1<Map<String, Purchase>, Purchase>() {
                    @Override
                    public Purchase call(Map<String, Purchase> data) {
                        if (data.containsKey(productId)) {
                            return data.get(productId);
                        } else {
                            throw new ProductNotFoundException(String.format("Subscription \"%s\" not found!", productId));
                        }
                    }
                });
    }

    /**
     * Starts purchase subscription flow
     */
    public Observable<Purchase> subscribe(String productId) {
        return purchase(productId, ProductType.SUBSCRIPTION);
    }

    /**
     * Emits {@link SkuDetails} of subscription for specified product id
     */
    public Observable<SkuDetails> getSubscription(String productId) {
        return getSkuDetails(productId, ProductType.SUBSCRIPTION);
    }

    /**
     * Emits list {@link SkuDetails} of subscription for specified product's ids
     */
    public Observable<List<SkuDetails>> getSubscriptions(List<String> productIdList) {
        return getSkuDetails(productIdList, ProductType.SUBSCRIPTION);
    }


    public static class Builder {
        private Context context;
        private Parser parser;
        private String licenseKey;
        private String merchantId;

        private Long timeout;
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

        Long getTimeout() {
            if (timeout == null) {
                return TimeUnit.SECONDS.toMillis(10);
            } else {
                return timeout;
            }
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
         * Timeout for InAppBillingService initialization
         */
        public Builder timeout(long value, TimeUnit timeUnit) {
            this.timeout = timeUnit.toMillis(value);
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
