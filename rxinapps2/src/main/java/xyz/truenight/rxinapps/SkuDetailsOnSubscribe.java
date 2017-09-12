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

import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import xyz.truenight.rxinapps.exception.LoadFailedException;
import xyz.truenight.rxinapps.model.SkuDetails;
import xyz.truenight.rxinapps.util.Constants;
import xyz.truenight.rxinapps.util.Parser;
import xyz.truenight.utils.Utils;

class SkuDetailsOnSubscribe implements SingleOnSubscribe<List<SkuDetails>> {

    private static final String TAG = RxInApps.TAG;

    private IInAppBillingService billingService;
    private String packageName;
    private Parser parser;
    private List<String> productIdList;
    private String productType;

    public SkuDetailsOnSubscribe(IInAppBillingService billingService, String packageName, Parser parser, List<String> productIdList, String productType) {
        this.billingService = billingService;
        this.packageName = packageName;
        this.parser = parser;
        this.productIdList = productIdList;
        this.productType = productType;
    }

    private static <T> ArrayList<T> toArrayList(List<T> list) {
        if (list == null) return null;
        if (list instanceof ArrayList) {
            return (ArrayList<T>) list;
        } else {
            return new ArrayList<>(list);
        }
    }

    @Override
    public void subscribe(SingleEmitter<List<SkuDetails>> emitter) throws Exception {
        try {
            List<List<String>> batches = Utils.chop(productIdList, 20);
            List<SkuDetails> skuDetails = new ArrayList<>();
            for (List<String> batch : batches) {
                Bundle products = new Bundle();
                products.putStringArrayList(Constants.PRODUCTS_LIST, toArrayList(batch));
                Bundle skuBundle =
                        billingService.getSkuDetails(Constants.API_VERSION, packageName, productType, products);
                int response = skuBundle.getInt(Constants.RESPONSE_CODE);

                if (response == Constants.RESULT_OK) {
                    List<String> detailsList = skuBundle.getStringArrayList(Constants.DETAILS_LIST);
                    if (detailsList != null) {
                        for (String responseLine : detailsList) {
                            SkuDetails product = parser.fromString(responseLine, SkuDetails.class);
                            skuDetails.add(product);
                        }
                    }
                } else {
                    throw new LoadFailedException("Failed to get sku details: RESPONSE_CODE=" + response);
                }
            }
            emitter.onSuccess(skuDetails);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call getSkuDetails", e);
            emitter.onError(e);
        }
    }
}
