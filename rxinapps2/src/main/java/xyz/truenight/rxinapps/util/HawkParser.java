package xyz.truenight.rxinapps.util;

import com.orhanobut.hawk.Parser;

import java.lang.reflect.Type;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

class HawkParser implements Parser {

    private final xyz.truenight.rxinapps.util.Parser parser;

    public HawkParser(xyz.truenight.rxinapps.util.Parser parser) {
        this.parser = parser;
    }

    @Override
    public <T> T fromJson(String content, Type type) throws Exception {
        return parser.fromString(content, type);
    }

    @Override
    public String toJson(Object body) {
        return parser.toString(body);
    }
}
