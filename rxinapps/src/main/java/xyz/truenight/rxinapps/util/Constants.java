/**
 * Copyright 2015 Pavlos-Petros Tournaris
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
package xyz.truenight.rxinapps.util;

public class Constants {
    public static final int API_VERSION = 3;

    public static final String BINDING_INTENT_VALUE = "com.android.vending.billing.InAppBillingService.BIND";
    public static final String VENDING_INTENT_PACKAGE = "com.android.vending";

    public static final int RESULT_OK = 0;                      //Success
    public static final int RESULT_USER_CANCELED = 1;           //User pressed back or canceled a dialog
    public static final int RESULT_BILLING_UNAVAILABLE = 3;     //Billing API version is not supported for the type requested
    public static final int RESULT_ITEM_UNAVAILABLE = 4;        //Requested product is not available for purchase
    public static final int RESULT_DEVELOPER_ERROR = 5;         //Invalid arguments provided to the API. This error can also indicate that the application was not correctly signed or properly set up for In-app Billing in Google Play, or does not have the necessary permissions in its manifest
    public static final int RESULT_ERROR = 6;                   //Fatal error during the API action
    public static final int RESULT_ITEM_ALREADY_OWNED = 7;      //Failure to purchase since item is already owned
    public static final int RESULT_ITEM_NOT_OWNED = 8;          //Failure to consume since item is not owned

    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    public static final String DETAILS_LIST = "DETAILS_LIST";
    public static final String PRODUCTS_LIST = "ITEM_ID_LIST";
    public static final String BUY_INTENT = "BUY_INTENT";
    public static final String INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    public static final String INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    public static final String RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE";
    public static final String INAPP_DATA_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";

    public static final String PURCHASE_PAYLOAD = "PURCHASE_PAYLOAD";
}
