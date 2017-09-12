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

package xyz.truenight.rxinapps.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;

import xyz.truenight.utils.Utils;

public class GsonParser implements Parser {

    private final Gson gson;

    public GsonParser() {
        this.gson = new Gson();
    }

    public GsonParser(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> T fromString(String content, Type type) throws JsonSyntaxException {
        if (Utils.isEmpty(content)) {
            return null;
        }
        return gson.fromJson(content, type);
    }

    @Override
    public String toString(Object body) {
        return gson.toJson(body);
    }
}
