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

import io.reactivex.SingleEmitter;
import io.reactivex.functions.Cancellable;

class RxUtils {
    private RxUtils() {
    }

    public static boolean isDisposed(SingleEmitter emitter) {
        return emitter == null || emitter.isDisposed();
    }

    public static <T> boolean setOnDispose(SingleEmitter<T> emitter, Cancellable cancellable) {
        if (emitter != null && !emitter.isDisposed()) {
            emitter.setCancellable(cancellable);
            return true;
        }
        return false;
    }

    public static <T> boolean onSuccess(SingleEmitter<T> emitter, T item) {
        if (emitter != null && !emitter.isDisposed()) {
            emitter.onSuccess(item);
            return true;
        }
        return false;
    }

    public static <T> boolean onError(SingleEmitter<T> emitter, Throwable th) {
        if (emitter != null && !emitter.isDisposed()) {
            emitter.onError(th);
            return true;
        }
        return false;
    }

//    public static <T> boolean publishResult(Subscriber<? super T> subscriber, T t) {
//        if (!isDisposed(subscriber)) {
//            subscriber.onNext(t);
//            subscriber.onCompleted();
//            return true;
//        }
//        return false;
//    }
//
//    public static boolean publishError(Subscriber subscriber, Throwable e) {
//        if (!isDisposed(subscriber)) {
//            subscriber.onError(e);
//            return true;
//        }
//        return false;
//    }
}
