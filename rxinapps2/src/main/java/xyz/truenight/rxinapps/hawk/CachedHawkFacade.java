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

package xyz.truenight.rxinapps.hawk;

import com.orhanobut.hawk.DefaultHawkFacade;
import com.orhanobut.hawk.HawkBuilder;

import xyz.truenight.utils.MemoryCache;

public class CachedHawkFacade extends DefaultHawkFacade {

    private final MemoryCache<String, Object> mCache = new MemoryCache<>();

    public CachedHawkFacade(HawkBuilder builder) {
        super(builder);
    }

    @Override
    public <T> boolean put(String key, T value) {
        mCache.put(key, value);
        return super.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key) {
        T t = (T) mCache.get(key);
        if (t == null) {
            t = super.get(key);
            if (t != null) {
                mCache.put(key, t);
            }
        }
        return t;
    }

    @Override
    public long count() {
        return super.count();
    }

    @Override
    public boolean deleteAll() {
        mCache.clear();
        return super.deleteAll();
    }

    @Override
    public boolean delete(String key) {
        mCache.remove(key);
        return super.delete(key);
    }

    @Override
    public boolean contains(String key) {
        return super.contains(key);
    }

    @Override
    public void destroy() {
        mCache.clear();
    }
}
