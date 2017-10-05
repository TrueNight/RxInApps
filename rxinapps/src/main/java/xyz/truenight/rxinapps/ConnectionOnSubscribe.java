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

import rx.Emitter;
import rx.functions.Action1;
import rx.functions.Cancellable;
import xyz.truenight.rxinapps.exception.InitializationException;
import xyz.truenight.rxinapps.util.Constants;

class ConnectionOnSubscribe implements Action1<Emitter<IInAppBillingService>> {

    private static final String TAG = RxInApps.TAG;

    private final RxInApps context;
    private IInAppBillingService service;

    public static ConnectionOnSubscribe create(final RxInApps context) {
        return new ConnectionOnSubscribe(context);
    }

    private ConnectionOnSubscribe(RxInApps context) {
        this.context = context;
    }

    @Override
    public void call(final Emitter<IInAppBillingService> emitter) {

        final boolean mainThread = isMainThread();

        final Semaphore semaphore = mainThread ? null : new Semaphore(0);

        final ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(TAG, "onServiceConnected");
                service = IInAppBillingService.Stub.asInterface(iBinder);
                if (mainThread) {
                    emitter.onNext(service);
                    emitter.onCompleted();
                } else {
                    semaphore.release();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d(TAG, "onServiceDisconnected");
                service = null;
            }
        };
        try {
            context.getContext().bindService(new Intent(Constants.BINDING_INTENT_VALUE)
                    .setPackage(Constants.VENDING_INTENT_PACKAGE), serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Created new service connection");
        } catch (Exception e) {
            emitter.onError(new InitializationException("Can NOT initialize InAppBillingService", e));
            return;
        }

        emitter.setCancellation(new Cancellable() {
            @Override
            public void cancel() throws Exception {
                Log.d(TAG, "onServiceUnbind");
                context.getContext().unbindService(serviceConnection);
            }
        });

        if (!mainThread) {
            semaphore.acquireUninterruptibly();
            emitter.onNext(service);
            emitter.onCompleted();
        }
    }

    private boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}
