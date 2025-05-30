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


public class ApplicationException extends Exception {

    private static final long serialVersionUID = -495731838035883308L;
    private int status;
    private String source;
    private int line;

    public ApplicationException() {
        super();
    }

    public ApplicationException(Throwable ex) {
        super(ex);
    }

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, int status) {
        super(message);
        this.status = status;
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationException(String message, Object source, int line) {
        super(message);
        this.source = source.getClass().getName().replace(".", "/") + ".java";
        this.line = line;
    }

    public String getSource() {
        return this.source;
    }

    public int getLine() {
        return this.line;
    }

    public int getStatus() {
        if (this.status == 0) return 500;
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
