package xyz.truenight.rxinapps.exception;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class LoadFailedException extends InAppBillingException {
    public LoadFailedException() {
    }

    public LoadFailedException(String detailMessage) {
        super(detailMessage);
    }

    public LoadFailedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public LoadFailedException(Throwable throwable) {
        super(throwable);
    }
}
