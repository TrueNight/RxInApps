package xyz.truenight.rxinapps.util;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

/**
 * Use this storage to disable caching
 */
public class EmptyStorage implements Storage {
    @Override
    public <T> T get(String key) {
        return null;
    }

    @Override
    public void put(String key, Object value) {

    }
}
