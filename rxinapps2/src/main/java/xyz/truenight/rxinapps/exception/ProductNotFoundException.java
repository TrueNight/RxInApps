package xyz.truenight.rxinapps.exception;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class ProductNotFoundException extends InAppBillingException {
    public ProductNotFoundException() {
    }

    public ProductNotFoundException(String detailMessage) {
        super(detailMessage);
    }

    public ProductNotFoundException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ProductNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
