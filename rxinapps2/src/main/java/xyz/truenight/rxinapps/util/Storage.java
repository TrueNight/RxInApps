package xyz.truenight.rxinapps.util;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public interface Storage {

    <T> T get(String key);

    void put(String key, Object value);
}
