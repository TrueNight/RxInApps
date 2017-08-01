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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;
import xyz.truenight.rxinapps.exception.InitializationException;
import xyz.truenight.rxinapps.util.Constants;

class ConnectionOnSubscribe implements Observable.OnSubscribe<IInAppBillingService> {

    private static final String TAG = RxInApps.TAG;

    private final RxInApps context;

    private AtomicInteger count = new AtomicInteger();
    private AtomicReference<IInAppBillingService> ref = new AtomicReference<>();
    private List<Runnable> publishers = new CopyOnWriteArrayList<>();


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected");
            ref.set(IInAppBillingService.Stub.asInterface(iBinder));
            for (Runnable publisher : publishers) {
                publisher.run();
                publishers.remove(publisher);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            ref.set(null);
        }
    };
    private final Subject<Context, Context> unbindPublisher = PublishSubject.<Context>create().toSerialized();
    private final Observable<Context> unbindObservable;
    private Subscription unbindSub;

    public static ConnectionOnSubscribe create(final RxInApps context, long timeout) {
        return new ConnectionOnSubscribe(context, timeout);
    }

    private ConnectionOnSubscribe(RxInApps context, long timeout) {
        this.context = context;
        unbindObservable = unbindPublisher
                .debounce(timeout, TimeUnit.MILLISECONDS)
                .filter(new Func1<Context, Boolean>() {
                    @Override
                    public Boolean call(Context context) {
                        return count.get() == 0;
                    }
                })
                .takeWhile(new Func1<Context, Boolean>() {
                    @Override
                    public Boolean call(Context context) {
                        return ref.get() != null;
                    }
                });
    }

    @Override
    public void call(final Subscriber<? super IInAppBillingService> subscriber) {

        count.incrementAndGet();
        Log.d(TAG, "Subscribed; count=" + count.get());
        if (ref.get() != null) {
            RxUtils.publishResult(subscriber, ref.get());
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    enqueueUnbindService();
                }
            }));
            return;
        }

        final boolean mainThread = isMainThread();

        final Semaphore semaphore = mainThread ? null : new Semaphore(0);

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mainThread) {
                    RxUtils.publishResult(subscriber, ref.get());
                } else {
                    semaphore.release();
                }
            }
        };
        publishers.add(runnable);

        if (count.get() == 1) {

            Log.d(TAG, "Created new service connection");
            try {
                context.getContext().bindService(new Intent(Constants.BINDING_INTENT_VALUE)
                        .setPackage(Constants.VENDING_INTENT_PACKAGE), serviceConnection, Context.BIND_AUTO_CREATE);
            } catch (Exception e) {
                subscriber.onError(new InitializationException("Can NOT initialize InAppBillingService", e));
                return;
            }

            unbindSub = unbindObservable.subscribe(new Action1<Context>() {
                @Override
                public void call(Context context) {
                    if (ref.get() != null) {
                        ref.set(null);
                        context.unbindService(serviceConnection);
                        Log.d(TAG, "Disconnected from service");
                        if (unbindSub != null) {
                            unbindSub.unsubscribe();
                            unbindSub = null;
                        }
                    }
                }
            });
        }

        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                enqueueUnbindService();
            }
        }));

        if (!mainThread) {
            semaphore.acquireUninterruptibly();
            RxUtils.publishResult(subscriber, ref.get());
        }
    }

    private void enqueueUnbindService() {
        count.decrementAndGet();
        Log.d(TAG, "Unsubscribed; count=" + count.get());
        unbindPublisher.onNext(context.getContext());
    }

    private boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}
