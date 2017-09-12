package xyz.truenight.rxinapps.exception;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class InitializationException extends InAppBillingException {
    public InitializationException() {
    }

    public InitializationException(String detailMessage) {
        super(detailMessage);
    }

    public InitializationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public InitializationException(Throwable throwable) {
        super(throwable);
    }
}
