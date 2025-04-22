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
 */
public class MCPException extends ApplicationException {
    
    /**
     * Constructs a new MCPException with the specified detail message.
     *
     * @param message The detail message
     */
    public MCPException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new MCPException with the specified detail message and cause.
     *
     * @param message The detail message
     * @param cause The cause
     */
    public MCPException(String message, Throwable cause) {
        super(message, cause);
    }
}
