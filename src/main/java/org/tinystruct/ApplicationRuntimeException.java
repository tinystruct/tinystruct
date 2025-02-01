/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct;


public class ApplicationRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -495731838035883308L;
    private String status;

    public ApplicationRuntimeException() {
        super();
    }

    public ApplicationRuntimeException(String message) {
        super(message);
    }

    public ApplicationRuntimeException(String message, String status) {
        super(message);
        this.status = status;
    }

    public ApplicationRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationRuntimeException(Throwable cause) {
        super(cause);
    }

    public String getStatus() {
        if (this.status == null) return "1111";
        return this.status;
    }

    public Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause != null) {
            return getRootCause(cause);
        }

        return throwable;
    }

    public Throwable getRootCause() {
        return this.getRootCause(this);
    }

}
