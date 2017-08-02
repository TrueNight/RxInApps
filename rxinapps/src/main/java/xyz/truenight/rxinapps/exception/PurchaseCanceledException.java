package xyz.truenight.rxinapps.exception;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class PurchaseCanceledException extends PurchaseFailedException {
    public PurchaseCanceledException() {
        super("User pressed back or canceled a dialog");
    }
}
