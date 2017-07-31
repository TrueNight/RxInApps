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

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;
import xyz.truenight.rxinapps.exception.InAppBillingException;
import xyz.truenight.rxinapps.util.Constants;
import xyz.truenight.rxinapps.util.RxUtils;

class ConnectionOnSubscribe implements Observable.OnSubscribe<IInAppBillingService> {

    private static final String TAG = RxInApps.TAG;

    private Semaphore semaphore = new Semaphore(0);
    private final RxInApps context;

    private AtomicReference<IInAppBillingService> ref = new AtomicReference<>();

    public static ConnectionOnSubscribe create(final RxInApps context) {
        return new ConnectionOnSubscribe(context);
    }

    private ConnectionOnSubscribe(RxInApps context) {
        this.context = context;
    }

    @Override
    public void call(final Subscriber<? super IInAppBillingService> subscriber) {
        final boolean mainThread = isMainThread();
        Log.d(TAG, "Created new service connection");
        final ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(TAG, "onServiceConnected");
                ref.set(IInAppBillingService.Stub.asInterface(iBinder));
                if (mainThread) {
                    RxUtils.publishResult(subscriber, ref.get());
                } else {
                    semaphore.release();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d(TAG, "onServiceDisconnected");
            }
        };
        try {
            Intent iapIntent = new Intent(Constants.BINDING_INTENT_VALUE);
            iapIntent.setPackage(Constants.VENDING_INTENT_PACKAGE);

            context.getContext().bindService(iapIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            subscriber.onError(new InAppBillingException("Can NOT initialize InAppBillingService", e));
            return;
        }

        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                context.getContext().unbindService(serviceConnection);
                Log.d(TAG, "unbindService");
            }
        }));

        if (!mainThread) {
            semaphore.acquireUninterruptibly();
            RxUtils.publishResult(subscriber, ref.get());
        }


    }

    private boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}
