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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;

import xyz.truenight.utils.Utils;

public class SkuDetails {

    public static final String FIELD_PRODUCT_ID = "productId";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PRICE_CURRENCY_CODE = "price_currency_code";
    public static final String FIELD_PRICE_AMOUNT_MICROS = "price_amount_micros";
    public static final String FIELD_PRICE = "price";

    private static final BigDecimal MILLION = BigDecimal.valueOf(1000000);

    @SerializedName(FIELD_PRODUCT_ID)
    private String productId;

    @SerializedName(FIELD_TITLE)
    private String title;

    @SerializedName(FIELD_DESCRIPTION)
    private String description;

    @SerializedName(FIELD_TYPE)
    private String type;

    @SerializedName(FIELD_PRICE_CURRENCY_CODE)
    private String currency;

    @SerializedName(FIELD_PRICE_AMOUNT_MICROS)
    private BigDecimal priceValueMicros;

    @SerializedName(FIELD_PRICE)
    private String priceText;

    public SkuDetails() {

    }

    public boolean isSubscription() {
        return ProductType.isSubscription(type);
    }

    public String getProductId() {
        return productId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @ProductType.Annotation
    public String getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getPriceValue() {
        return priceValueMicros.divide(MILLION, RoundingMode.HALF_UP);
    }

    public String getLocalizedPrice() {
        if (Utils.equal(currency, "RUB")) {
            NumberFormat formatter = NumberFormat.getNumberInstance();
            formatter.setMinimumFractionDigits(0);
            formatter.setMaximumFractionDigits(2);
            return formatter.format(getPriceValue()) + " \u20BD"; // "₽" symbol
        } else {
            NumberFormat formatter = NumberFormat.getCurrencyInstance();
            formatter.setCurrency(Currency.getInstance(currency));
            return formatter.format(getPriceValue());
        }
    }

    BigDecimal getPriceValueMicros() {
        return priceValueMicros;
    }

    public String getPriceText() {
        return priceText;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SkuDetails that = (SkuDetails) o;

        return isSubscription() == that.isSubscription()
                && !(productId != null ? !productId.equals(that.productId) : that.productId != null);

    }

    @Override
    public int hashCode() {
        int result = productId != null ? productId.hashCode() : 0;
        result = 31 * result + (isSubscription() ? 1 : 0);
        return result;
    }
}
