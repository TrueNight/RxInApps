package xyz.truenight.rxinapps.exception;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class BillingUnavailableException extends PurchaseFailedException {
    public BillingUnavailableException() {
        super("Billing API version is not supported for the type requested");
    }
}
