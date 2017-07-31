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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import java.util.Map;

import rx.functions.Func1;
import xyz.truenight.rxinapps.RxInApps;
import xyz.truenight.rxinapps.exception.InAppBillingException;
import xyz.truenight.rxinapps.exception.MerchantIdException;
import xyz.truenight.rxinapps.exception.PayloadException;
import xyz.truenight.rxinapps.exception.SignatureException;
import xyz.truenight.rxinapps.model.ProductType;
import xyz.truenight.rxinapps.model.Purchase;
import xyz.truenight.rxinapps.util.Constants;
import xyz.truenight.rxinapps.util.RxUtils;
import xyz.truenight.utils.Utils;

public class HiddenActivity extends Activity {

    private static final String TAG = RxInApps.TAG;

    private static final int PURCHASE_FLOW_REQUEST_CODE = 65535; // Can only use lower 16 bits for requestCode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PendingIntent pendingIntent = getIntent().getParcelableExtra(Constants.BUY_INTENT);
        try {
            startIntentSenderForResult(pendingIntent.getIntentSender(),
                    PURCHASE_FLOW_REQUEST_CODE, new Intent(), 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            RxInApps.with(this).deliverPurchaseError(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PURCHASE_FLOW_REQUEST_CODE) {
            return false;
        }
        if (data == null) {
            Log.e(TAG, "handleActivityResult: data is null!");
            return false;
        }

        RxInApps rxInApps = RxInApps.with(this);

        if (rxInApps.checkPurchaseSubscriber()) {
            Log.e(TAG, "", new InAppBillingException("Subscriber for purchase is unsubscribed or NULL"));
            return true;
        }

        int responseCode = data.getIntExtra(Constants.RESPONSE_CODE, Constants.RESULT_OK);
        Log.d(TAG, String.format("resultCode = %d, responseCode = %d", resultCode, responseCode));

        String purchasePayload = getIntent().getStringExtra(Constants.PURCHASE_PAYLOAD);

        if (resultCode == Activity.RESULT_OK
                && responseCode == Constants.RESULT_OK) {
            handleOkResponse(data, purchasePayload, rxInApps);
        } else if (responseCode == Constants.RESULT_ITEM_ALREADY_OWNED) {
            handleAlreadyOwnedResponse(data, purchasePayload, rxInApps);
        } else {
            rxInApps.deliverPurchaseError(new InAppBillingException("Failed to purchase: RESPONSE_CODE=" + responseCode));
        }
        return true;
    }

    private void handleOkResponse(Intent data, String purchasePayload, RxInApps rxInApps) {
        String purchaseData = data.getStringExtra(Constants.INAPP_PURCHASE_DATA);
        String dataSignature = data.getStringExtra(Constants.RESPONSE_INAPP_SIGNATURE);

        try {
            Purchase purchase = rxInApps.getParser().fromString(purchaseData, Purchase.class);
            purchase.setPurchaseSignature(dataSignature);
            purchase.setRawResponse(purchaseData);
            String productId = purchase.getProductId();
            String developerPayload = purchase.getDeveloperPayload();

            if (Utils.equal(purchasePayload, developerPayload)) {
                if (RxInApps.verifyPurchaseSignature(productId, purchaseData, dataSignature)) {
                    String productType = Utils.first(purchasePayload.split(":"));
                    rxInApps.putPurchaseToCache(purchase, productType);

                    rxInApps.deliverPurchaseResult(purchase);
                } else {
                    throw new SignatureException("Public key signature does NOT match");
                }
            } else {
                throw new PayloadException(String.format("Payload mismatch: %s != %s", purchasePayload, developerPayload));
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
            rxInApps.deliverPurchaseError(e);
        }
    }

    private void handleAlreadyOwnedResponse(Intent data, String purchasePayload, RxInApps rxInApps) {
        String purchaseData = data.getStringExtra(Constants.INAPP_PURCHASE_DATA);
        String dataSignature = data.getStringExtra(Constants.RESPONSE_INAPP_SIGNATURE);

        try {
            Purchase purchase = rxInApps.getParser().fromString(purchaseData, Purchase.class);
            purchase.setPurchaseSignature(dataSignature);
            purchase.setRawResponse(purchaseData);
            final String productId = purchase.getProductId();
            String developerPayload = purchase.getDeveloperPayload();

            if (Utils.equal(purchasePayload, developerPayload)) {
                String productType = Utils.first(purchasePayload.split(":"));

                Map<String, Purchase> map = rxInApps.getStorage().get(productType);

                if (!Utils.containsKey(map, productId)) {
                    rxInApps.putPurchaseToCache(purchase, productType);
                    // todo is it necessary???
//                        purchase = rxInApps.loadPurchasesByType(productType)
//                                .compose(rxInApps.toMapAndCache(productType))
//                                .map(new Func1<Map<String, Purchase>, Purchase>() {
//                                    @Override
//                                    public Purchase call(Map<String, Purchase> map) {
//                                        return map.get(productId);
//                                    }
//                                })
//                                .toBlocking()
//                                .first();
                } else {
                    // todo is it necessary???
//                    purchase = map.get(productId);
                }

                if (ProductType.isManaged(productType) && !RxInApps.checkMerchantTransactionDetails(purchase)) {
                    throw new MerchantIdException("Invalid or tampered merchant id!");
                }

                purchase.setRestored(true);
                rxInApps.deliverPurchaseResult(purchase);
            } else {
                throw new PayloadException(String.format("Payload mismatch: %s != %s", purchasePayload, developerPayload));
            }
        } catch (Exception e) {
            rxInApps.deliverPurchaseError(e);
        }
    }
}
