# RxInApps
[![Download](https://api.bintray.com/packages/truenight/maven/rxinapps2/images/download.svg)](https://bintray.com/truenight/maven/rxinapps2/_latestVersion)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/xyz.truenight.rxinapps/rxinapps2/badge.svg)](https://maven-badges.herokuapp.com/maven-central/xyz.truenight.rxinapps/rxinapps2)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/xyz.truenight.rxinapps/rxinapps2/badge.svg)](http://www.javadoc.io/doc/xyz.truenight.rxinapps/rxinapps2)

Support for serializing RealmObject with Gson

# Overview

RxJava2 Observable wrapper for interface of In App Billing Library v3 with simple caching

# Installation

Add dependency to your `build.gradle` file:

```groovy
dependencies {
    compile 'xyz.truenight.rxinapps:rxinapps2:2.0.0'
}
```

or to your `pom.xml` if you're using Maven:

```xml
<dependency>
  <groupId>xyz.truenight.rxinapps2</groupId>
  <artifactId>rxinapps</artifactId>
  <version>2.0.0</version>
  <type>pom</type>
</dependency>
```
# Usage

## Instance

```java

  RxInApps.init(new RxInApps.Builder(context)
      .licenseKey(KEY) // by default validation of license key is disabled to enable specify it
      .merchantId(MERCHANT_ID) // by default validation of merchant id is disabled to enable specify it
      .timeout(30, TimeUnit.SECONDS) // timeout for getting service connection
      .cacheLifetime(30, TimeUnit.MINUTES) // duration when cache will be valid
      .storage() // storage for caching by default it is Hawk
      .parser()); // json parser for deserializing data from google and caching by default it is Gson
    
```
## Get ``SkuDetails``

To get info about product or subcription use either ``getProduct`` or ``getSubscription``

```java

    RxInApps.with(context).get[Product|Subscription](productId)
        .subscribe(skuDetails -> {
            priceTextView.setText(skuDetails.getLocalizedPrice());
        }, th -> {
            if (th instanceof LoadFailedException) {
                // load failed see stacktrace
                th.printStackTrace();
            }
        });
    
```

## Purchase

To purchase product or subcription use ``purchase`` or ``subscribe`` methods

```java

    RxInApps.with(context).[purchase|subscribe](productId)
        .subscribe(purchase -> {
            // send token to server and etc.
        }, th -> {
            if (th instanceof PurchaseCanceledException) {
                // purchase canceled by user
            } else if (th instanceof BillingUnavailableException) {
                // billing API version is not supported
            } else if (th instanceof ItemUnavailableException) {
                // product is not available for purchase
            } else if (th instanceof DeveloperErrorException) {
                // invalid arguments provided to the API
            } else if (th instanceof PurchaseFailedException) {
                // purchase failed see stacktrace
                th.printStackTrace();
            }
        });
    
```

## Consume

To consume purchased product ``consume``

```java

    RxInApps.with(context).consume(productId)
        .subscribe(purchase -> {
            // purchase consumed and removed from cache
        }, th -> {
            if (th instanceof PurchaseNotFoundException) {
                // purchase not found
            } else if (th instanceof ConsumeFailedException) {
                // consume failed see stacktrace
                th.printStackTrace();
            }
        });

```

## Get purchased

To get purchased products or subscriptions use ``purchasedProducts()``, ``purchasedSubscriptions()`` or if you want get purchase of specified product use ``getPurchasedProduct(productId)``, ``getPurchasedSubscription(subscriptionId)``

```java

    RxInApps.with(context).purchased[Products|Subscriptions]()
        .subscribe(purchaseList -> {
            // do something
        }, th -> {
            if (th instanceof LoadFailedException) {
                // load failed see stacktrace
                th.printStackTrace();
            }
        });
        
        
    RxInApps.with(context).getPurchased[Product|Subscription](productId)
        .subscribe(purchase -> {
            // do something
        }, th -> {
            if (th instanceof ProductNotFoundException) {
                // purchased product not found or product is not purchased
            } else if (th instanceof LoadFailedException) {
                // load failed see stacktrace
                th.printStackTrace();
            }
        });

```
