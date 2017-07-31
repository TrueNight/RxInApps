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

import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import xyz.truenight.utils.Utils;

/**
 * Copyright (C) 2017 Mikhail Frolov
 */

public class RxHawkFacade extends DefaultHawkFacade {

    private final Subject<Change, Change> subject = PublishSubject.<Change>create().toSerialized();

    public RxHawkFacade(HawkBuilder builder) {
        super(builder);
    }

    @Override
    public <T> boolean put(String key, T value) {
        subject.onNext(Change.create(key, value));
        return super.put(key, value);
    }

    public <T> Observable<T> observe(final String key) {
        return observe(key, null);
    }

    @SuppressWarnings("unchecked")
    public <T> Observable<T> observe(final String key, T defValue) {
        return Observable.merge(
                Observable.just(get(key, defValue)),
                subject.filter(new Func1<Change, Boolean>() {
                    @Override
                    public Boolean call(Change change) {
                        return change.same(key);
                    }
                }).map(new Func1<Change, T>() {
                    @Override
                    public T call(Change change) {
                        return (T) change.object;
                    }
                })
        );
    }

    private static class Change {
        private String key;
        private Object object;

        private static Change create(String key, Object t) {
            return new Change(key, t);
        }

        private Change(String key, Object object) {
            this.key = key;
            this.object = object;
        }

        private boolean same(String key) {
            return Utils.equal(this.key, key);
        }
    }
}
