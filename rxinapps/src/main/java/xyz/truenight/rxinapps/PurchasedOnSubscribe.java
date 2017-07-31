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

import android.os.Bundle;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import xyz.truenight.rxinapps.exception.InAppBillingException;
import xyz.truenight.rxinapps.model.Purchase;
import xyz.truenight.rxinapps.util.Constants;
import xyz.truenight.rxinapps.util.Parser;
import xyz.truenight.rxinapps.util.RxUtils;
import xyz.truenight.utils.Utils;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

class PurchasedOnSubscribe implements Observable.OnSubscribe<List<Purchase>> {

    private static final String TAG = RxInApps.TAG;

    private IInAppBillingService billingService;
    private String packageName;
    private Parser parser;
    private String type;

    public static PurchasedOnSubscribe create(IInAppBillingService billingService, String packageName, Parser parser, String type) {
        return new PurchasedOnSubscribe(billingService, packageName, parser, type);
    }

    public PurchasedOnSubscribe(IInAppBillingService billingService, String packageName, Parser parser, String type) {
        this.billingService = billingService;
        this.packageName = packageName;
        this.parser = parser;
        this.type = type;
    }

    @Override
    public void call(Subscriber<? super List<Purchase>> subscriber) {
        try {
            Bundle bundle = billingService.getPurchases(Constants.API_VERSION, packageName, type, null);
            if (bundle.getInt(Constants.RESPONSE_CODE) == Constants.RESULT_OK) {
                ArrayList<String> purchaseList = bundle.getStringArrayList(Constants.INAPP_PURCHASE_DATA_LIST);
                ArrayList<String> signatureList = bundle.getStringArrayList(Constants.INAPP_DATA_SIGNATURE_LIST);

                List<Purchase> list = new ArrayList<>(Utils.sizeOf(purchaseList));
                if (purchaseList != null) {
                    for (int i = 0; i < purchaseList.size(); i++) {
                        String jsonData = purchaseList.get(i);
                        Purchase purchase = parser.fromString(jsonData, Purchase.class);
                        String signature = signatureList != null && signatureList.size() > i ? signatureList.get(i) : null;
                        purchase.setPurchaseSignature(signature);
                        purchase.setRawResponse(jsonData);
                        list.add(purchase);
                    }
                }
                RxUtils.publishResult(subscriber, list);
            } else {
                throw new InAppBillingException("Failed to load purchases: RESPONSE_CODE=" + bundle.getInt(Constants.RESPONSE_CODE));
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
            RxUtils.publishError(subscriber, e);
        }
    }
}
