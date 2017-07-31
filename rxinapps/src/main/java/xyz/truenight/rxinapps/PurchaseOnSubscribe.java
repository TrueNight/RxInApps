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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.android.vending.billing.IInAppBillingService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscriber;
import xyz.truenight.rxinapps.exception.InAppBillingException;
import xyz.truenight.rxinapps.exception.MerchantIdException;
import xyz.truenight.rxinapps.model.ProductType;
import xyz.truenight.rxinapps.model.Purchase;
import xyz.truenight.rxinapps.util.Constants;
import xyz.truenight.rxinapps.util.RxUtils;
import xyz.truenight.utils.Utils;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

class PurchaseOnSubscribe implements Observable.OnSubscribe<Purchase> {

    private RxInApps context;
    private IInAppBillingService billingService;
    private String packageName;
    private String productId;
    private String productType;
    private AtomicReference<Subscriber<? super Purchase>> purchaseSubscriber;

    public static Observable.OnSubscribe<Purchase> create(RxInApps context,
                                                          IInAppBillingService billingService,
                                                          String packageName,
                                                          String productId,
                                                          String productType,
                                                          AtomicReference<Subscriber<? super Purchase>> purchaseSubscriber) {

        return new PurchaseOnSubscribe(context, billingService, packageName, productId, productType, purchaseSubscriber);
    }

    public PurchaseOnSubscribe(RxInApps context, IInAppBillingService billingService, String packageName, String productId, String productType, AtomicReference<Subscriber<? super Purchase>> purchaseSubscriber) {
        this.context = context;
        this.billingService = billingService;
        this.packageName = packageName;
        this.productId = productId;
        this.productType = productType;
        this.purchaseSubscriber = purchaseSubscriber;
    }

    @Override
    public void call(Subscriber<? super Purchase > subscriber) {

        try {
            String purchasePayload = productType + ":" + UUID.randomUUID();

            purchaseSubscriber.set(subscriber);

            Bundle bundle =
                    billingService.getBuyIntent(Constants.API_VERSION, packageName,
                            productId, productType, purchasePayload);

            if (bundle != null) {
                int response = bundle.getInt(Constants.RESPONSE_CODE);

                if (response == Constants.RESULT_OK) {

                    PendingIntent pendingIntent = bundle.getParcelable(Constants.BUY_INTENT);

                    if (pendingIntent != null) {
                        // todo start hidden flow
                        Context context = this.context.getContext();
                        context.startActivity(new Intent(context, HiddenActivity.class)
                                .putExtra(Constants.BUY_INTENT, pendingIntent)
                                .putExtra(Constants.PURCHASE_PAYLOAD, purchasePayload));
//                                                        activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
//                                                                PURCHASE_FLOW_REQUEST_CODE, new Intent(), 0, 0, 0);
                    } else {
                        throw new InAppBillingException(new NullPointerException("Buy intent is NULL"));
                    }
                } else if (response == Constants.RESULT_ITEM_ALREADY_OWNED) {

                    Map<String, Purchase> map = context.getStorage().get(productType);
                    Purchase purchase;

                    if (Utils.containsKey(map, productId)) {
                        purchase = map.get(productId);
                    } else {
                        context.loadPurchasesByType(billingService, productType)
                                .compose(context.toMapAndCache(productType))
                                .subscribe();
                        map = context.getStorage().get(productType);
                        purchase = map.get(productId);
                    }

                    if (ProductType.isManaged(productType) && !RxInApps.checkMerchantTransactionDetails(purchase)) {
                        throw new MerchantIdException("Invalid or tampered merchant id!");
                    }

                    purchase.setRestored(true);
                    RxUtils.publishResult(subscriber, purchase);
                } else {
                    throw new InAppBillingException("Failed to purchase: RESPONSE_CODE=" + response);
                }
            } else {
                throw new InAppBillingException("Bundle is NULL");
            }
        } catch (Exception e) {
            RxUtils.publishError(subscriber, e);
        }
    }
}
