package xyz.truenight.rxinapps.exception;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class PurchaseCenceledException extends InAppBillingException {
    public PurchaseCenceledException() {
    }

    public PurchaseCenceledException(String detailMessage) {
        super(detailMessage);
    }

    public PurchaseCenceledException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PurchaseCenceledException(Throwable throwable) {
        super(throwable);
    }
}
