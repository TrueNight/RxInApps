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

package xyz.truenight.rxinapps.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Purchase {

    public static final String FIELD_PRODUCT_ID = "productId";
    public static final String FIELD_DEVELOPER_PAYLOAD = "developerPayload";
    public static final String FIELD_MODEL_ORDER_ID = "orderId";
    public static final String FIELD_PACKAGE_NAME = "packageName";
    public static final String FIELD_PURCHASE_STATE = "purchaseState";
    public static final String FIELD_PURCHASE_TOKEN = "purchaseToken";
    public static final String FIELD_PURCHASE_TIME = "purchaseTime";


    @SerializedName(FIELD_PRODUCT_ID)
    private String productId;

    @SerializedName(FIELD_DEVELOPER_PAYLOAD)
    private String developerPayload;

    @SerializedName(FIELD_MODEL_ORDER_ID)
    private String orderId;

    @SerializedName(FIELD_PACKAGE_NAME)
    private String packageName;

    @SerializedName(FIELD_PURCHASE_STATE)
    private int purchaseState;

    @SerializedName(FIELD_PURCHASE_TOKEN)
    private String purchaseToken;

    @SerializedName(FIELD_PURCHASE_TIME)
    private long purchaseTimeMillis;

    private String purchaseSignature;
    private String rawResponse;
    private boolean restored;

    public Purchase() {
    }

    public Date getPurchaseTime() {
        return new Date(this.purchaseTimeMillis);
    }

    public String getPurchaseSignature() {
        return purchaseSignature;
    }

    public void setPurchaseSignature(String purchaseSignature) {
        this.purchaseSignature = purchaseSignature;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getPurchaseState() {
        return purchaseState;
    }

    public long getPurchaseTimeMillis() {
        return purchaseTimeMillis;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPurchaseToken() {
        return purchaseToken;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getDeveloperPayload() {
        return developerPayload;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public boolean isRestored() {
        return restored;
    }

    public void setRestored(boolean restored) {
        this.restored = restored;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
