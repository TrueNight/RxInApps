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

import android.content.Context;

import java.lang.ref.WeakReference;

abstract class ContextHolder {

    private WeakReference<Context> contextReference;

    public ContextHolder(Context context) {
        contextReference = new WeakReference<>(context);
    }

    public Context getContext() {
        Context context = contextReference.get();
        if (context == null) {
            throw new IllegalStateException("Context was recycled by gc");
        }
        return context;
    }

    public void release() {
        contextReference.clear();
    }
}