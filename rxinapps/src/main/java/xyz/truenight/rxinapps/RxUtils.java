/**
 * Copyright (C) 2017 Mikhail Frolov
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.truenight.rxinapps;

import rx.Subscriber;
import rx.Subscription;

class RxUtils {
    private RxUtils() {
    }

    public static boolean isUnsubscribed(Subscriber subscriber) {
        return subscriber == null || subscriber.isUnsubscribed();
    }

    public static <T> boolean addOnUnsubscribe(Subscriber<? super T> subscriber, Subscription subscription) {
        if (!isUnsubscribed(subscriber)) {
            subscriber.add(subscription);
            return true;
        }
        return false;
    }

    public static <T> boolean publishResult(Subscriber<? super T> subscriber, T t) {
        if (!isUnsubscribed(subscriber)) {
            subscriber.onNext(t);
            subscriber.onCompleted();
            return true;
        }
        return false;
    }

    public static boolean publishError(Subscriber subscriber, Throwable e) {
        if (!isUnsubscribed(subscriber)) {
            subscriber.onError(e);
            return true;
        }
        return false;
    }
}
