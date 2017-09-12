package xyz.truenight.rxinapps.exception;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class ConsumeFailedException extends InAppBillingException {
    public ConsumeFailedException() {
    }

    public ConsumeFailedException(String detailMessage) {
        super(detailMessage);
    }

    public ConsumeFailedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ConsumeFailedException(Throwable throwable) {
        super(throwable);
    }
}
