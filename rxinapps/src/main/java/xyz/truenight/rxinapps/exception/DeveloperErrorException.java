package xyz.truenight.rxinapps.exception;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class DeveloperErrorException extends PurchaseFailedException {
    public DeveloperErrorException() {
        super("Invalid arguments provided to the API. This error can also indicate that the application was not correctly signed or properly set up for In-app Billing in Google Play, or does not have the necessary permissions in its manifest");
    }
}
