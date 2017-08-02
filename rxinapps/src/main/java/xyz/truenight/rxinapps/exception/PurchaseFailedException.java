package xyz.truenight.rxinapps.exception;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class PurchaseFailedException extends InAppBillingException {
    public PurchaseFailedException() {
    }

    public PurchaseFailedException(String detailMessage) {
        super(detailMessage);
    }

    public PurchaseFailedException(int responseCode) {
        super("Failed to purchase: RESPONSE_CODE=" + responseCode);
    }

    public PurchaseFailedException(String detailMessage, int responseCode) {
        super(detailMessage + "\nFailed to purchase: RESPONSE_CODE=" + responseCode);
    }

    public PurchaseFailedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PurchaseFailedException(Throwable throwable) {
        super(throwable);
    }
}
