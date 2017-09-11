package xyz.truenight.rxinapps.util;

import android.content.Context;

import com.orhanobut.hawk.HawkBuilder;
import com.orhanobut.hawk.HawkFacade;

import xyz.truenight.rxinapps.RxInApps;
import xyz.truenight.rxinapps.hawk.CachedHawkFacade;
import xyz.truenight.rxinapps.hawk.SharedPreferencesStorage;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

/**
 * Default storage for cache
 */
public class HawkStorage implements Storage {

    private static final String STORAGE_TAG = RxInApps.TAG + RxInApps.VERSION;

    private final HawkFacade hawk;

    public HawkStorage(Context context, Parser parser) {
        HawkBuilder builder = new HawkBuilder(context);
        if (parser != null) {
            builder.setParser(new HawkParser(parser));
        }
        hawk = new CachedHawkFacade(builder
                .setStorage(new SharedPreferencesStorage(context, STORAGE_TAG)));
    }

    @Override
    public <T> T get(String key) {
        return hawk.get(key);
    }

    @Override
    public void put(String key, Object value) {
        hawk.put(key, value);
    }
}
