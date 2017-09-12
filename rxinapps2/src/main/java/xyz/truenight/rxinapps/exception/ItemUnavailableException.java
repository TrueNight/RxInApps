package xyz.truenight.rxinapps.exception;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class ItemUnavailableException extends PurchaseFailedException {
    public ItemUnavailableException() {
        super("Requested product is not available for purchase");
    }
}
