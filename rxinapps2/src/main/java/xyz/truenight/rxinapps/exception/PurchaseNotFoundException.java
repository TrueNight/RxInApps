package xyz.truenight.rxinapps.exception;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class PurchaseNotFoundException extends ConsumeFailedException {
    public PurchaseNotFoundException() {
    }

    public PurchaseNotFoundException(String detailMessage) {
        super(detailMessage);
    }

    public PurchaseNotFoundException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PurchaseNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
