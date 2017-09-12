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

import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import java.util.Locale;
import java.util.Map;

import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import xyz.truenight.rxinapps.exception.ConsumeFailedException;
import xyz.truenight.rxinapps.exception.PurchaseNotFoundException;
import xyz.truenight.rxinapps.model.ProductType;
import xyz.truenight.rxinapps.model.Purchase;
import xyz.truenight.rxinapps.util.Constants;
import xyz.truenight.utils.Utils;

class ConsumePurchaseOnSubscribe implements SingleOnSubscribe<Purchase> {

    private static final String TAG = RxInApps.TAG;

    private RxInApps context;
    private IInAppBillingService billingService;
    private String packageName;
    private Map<String, Purchase> map;
    private String productId;


    public static ConsumePurchaseOnSubscribe create(RxInApps context, IInAppBillingService billingService, String packageName, Map<String, Purchase> map, String productId) {
        return new ConsumePurchaseOnSubscribe(context, billingService, packageName, map, productId);
    }

    public ConsumePurchaseOnSubscribe(RxInApps context, IInAppBillingService billingService, String packageName, Map<String, Purchase> map, String productId) {
        this.context = context;
        this.billingService = billingService;
        this.packageName = packageName;
        this.map = map;
        this.productId = productId;
    }

    @Override
    public void subscribe(SingleEmitter<Purchase> emitter) throws Exception {
        try {
            Purchase purchase = map.get(productId);

            Log.d(TAG, "Transaction details is:" + (purchase == null ? "null" : "not null"));

            if (purchase != null && !Utils.isEmpty(purchase.getPurchaseToken())) {
                int response = billingService
                        .consumePurchase(
                                Constants.API_VERSION,
                                packageName,
                                purchase.getPurchaseToken());
                Log.d(TAG, "Consume response code:" + response);
                if (response == Constants.RESULT_OK) {
                    context.removePurchaseFromCache(productId, ProductType.MANAGED);
                    Log.d(TAG, "Successfully consumed " + productId + " purchase.");
                    emitter.onSuccess(purchase);
                } else {
                    throw new ConsumeFailedException(String.format(Locale.getDefault(), "Failed to consume %s: RESPONSE_CODE=%d", productId, response));
                }
            } else {
                throw new PurchaseNotFoundException("Purchase for consuming not found");
            }
        } catch (Exception e) {
            emitter.onError(e);
        }
    }
}
