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
package org.tinystruct.mcp;

import org.tinystruct.ApplicationException;

/**
 * Exception thrown for MCP-related errors.
 * <p>
 * This exception is used to indicate errors that occur during MCP operations,
 * such as communication errors, invalid parameters, or resource not found errors.
 * </p>
 */
public class MCPException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new MCPException with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the getMessage() method)
     */
    public MCPException(String message) {
        super(message);
    }

    /**
     * Constructs a new MCPException with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with cause is not automatically
     * incorporated in this exception's detail message.
     * </p>
     *
     * @param message The detail message (which is saved for later retrieval by the getMessage() method)
     * @param cause The cause (which is saved for later retrieval by the getCause() method)
     */
    public MCPException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new MCPException with the specified cause and a detail message
     * of (cause==null ? null : cause.toString()).
     *
     * @param cause The cause (which is saved for later retrieval by the getCause() method)
     */
    public MCPException(Throwable cause) {
        super(cause);
    }
}
