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

package xyz.truenight.rxinapps.exception;

public class InAppBillingException extends RuntimeException {
    public InAppBillingException() {
    }

    public InAppBillingException(String detailMessage) {
        super(detailMessage);
    }

    public InAppBillingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public InAppBillingException(Throwable throwable) {
        super(throwable);
    }
}
