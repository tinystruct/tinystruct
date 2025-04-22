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

/**
 * Model Context Protocol (MCP) Specification
 *
 * This class defines the standard constants, methods, and structures
 * for the Model Context Protocol to ensure consistency between
 * client and server implementations.
 */
public final class MCPSpecification {

    /**
     * Protocol version
     */
    public static final String PROTOCOL_VERSION = "1.0.0";

    /**
     * Protocol identifier string
     */
    public static final String PROTOCOL_ID = "MCP/1.0";

    /**
     * Session state enumeration
     */
    public enum SessionState {
        DISCONNECTED,
        INITIALIZING,
        READY,
        ERROR
    }

    /**
     * Standard feature identifiers
     */
    public static final class Features {
        public static final String BASE = "base";
        public static final String LIFECYCLE = "lifecycle";
        public static final String RESOURCES = "resources";
        public static final String TOOLS = "tools";
        public static final String PROMPTS = "prompts";
        public static final String SSE = "sse";
        public static final String JSON_RPC = "json-rpc";

        /**
         * Core features supported by all MCP implementations
         */
        public static final String[] CORE_FEATURES = {
            BASE, LIFECYCLE, RESOURCES, TOOLS, PROMPTS, SSE, JSON_RPC
        };

        private Features() {} // Prevent instantiation
    }

    /**
     * Standard method names
     */
    public static final class Methods {
        public static final String INITIALIZE = "initialize";
        public static final String SHUTDOWN = "shutdown";
        public static final String GET_STATUS = "get-status";
        public static final String GET_CAPABILITIES = "get-capabilities";
        public static final String LIST_TOOLS = "list-tools";
        public static final String CALL_TOOL = "call-tool";
        public static final String LIST_RESOURCES = "list-resources";
        public static final String READ_RESOURCE = "read-resource";
        public static final String LIST_PROMPTS = "list-prompts";
        public static final String GET_PROMPT = "get-prompt";

        private Methods() {} // Prevent instantiation
    }

    /**
     * Standard error codes
     * Follows JSON-RPC 2.0 error code conventions with MCP-specific extensions
     */
    public static final class ErrorCodes {
        // JSON-RPC 2.0 standard error codes
        public static final int PARSE_ERROR = -32700;
        public static final int INVALID_REQUEST = -32600;
        public static final int METHOD_NOT_FOUND = -32601;
        public static final int INVALID_PARAMS = -32602;
        public static final int INTERNAL_ERROR = -32603;

        // MCP-specific error codes
        public static final int UNAUTHORIZED = -32001;
        public static final int ALREADY_INITIALIZED = -32002;
        public static final int NOT_INITIALIZED = -32003;
        public static final int FEATURE_NOT_SUPPORTED = -32004;
        public static final int RESOURCE_NOT_FOUND = -32005;

        private ErrorCodes() {} // Prevent instantiation
    }

    /**
     * SSE event types
     */
    public static final class Events {
        public static final String CONNECTED = "connected";
        public static final String STATE = "state";
        public static final String ERROR = "error";
        public static final String CLOSE = "close";
        public static final String NOTIFICATION = "notification";
        public static final String RESOURCES_CHANGED = "resources_changed";
        public static final String TOOLS_CHANGED = "tools_changed";
        public static final String PROMPTS_CHANGED = "prompts_changed";

        private Events() {} // Prevent instantiation
    }

    /**
     * HTTP header and parameter constants
     */
    public static final class Http {
        public static final String AUTH_HEADER = "Authorization";
        public static final String CONTENT_TYPE_JSON = "application/json";
        public static final String CONTENT_TYPE_SSE = "text/event-stream";
        public static final String TOKEN_PARAM = "token";

        private Http() {} // Prevent instantiation
    }

    /**
     * Configuration property keys
     */
    public static final class Config {
        public static final String AUTH_TOKEN = "mcp.auth.token";
        public static final String SERVER_PORT = "mcp.server.port";
        public static final String SERVER_HOST = "mcp.server.host";
        public static final String SESSION_TIMEOUT = "mcp.session.timeout";

        private Config() {} // Prevent instantiation
    }

    /**
     * Endpoint paths
     */
    public static final class Endpoints {
        public static final String INITIALIZE = "mcp/initialize";
        public static final String CAPABILITIES = "mcp/capabilities";
        public static final String SHUTDOWN = "mcp/shutdown";
        public static final String EVENTS = "mcp/events";
        public static final String RPC = "mcp/rpc";

        private Endpoints() {} // Prevent instantiation
    }

    /**
     * JSON-RPC related constants
     */
    public static final class JsonRpc {
        public static final String VERSION = "2.0";
        public static final String VERSION_FIELD = "jsonrpc";
        public static final String METHOD_FIELD = "method";
        public static final String PARAMS_FIELD = "params";
        public static final String ID_FIELD = "id";
        public static final String RESULT_FIELD = "result";
        public static final String ERROR_FIELD = "error";
        public static final String ERROR_CODE_FIELD = "code";
        public static final String ERROR_MESSAGE_FIELD = "message";
        public static final String ERROR_DATA_FIELD = "data";

        private JsonRpc() {} // Prevent instantiation
    }

    /**
     * Validates if a feature is a standard MCP feature
     *
     * @param feature Feature name to validate
     * @return true if the feature is a standard MCP feature
     */
    public static boolean isStandardFeature(String feature) {
        for (String standardFeature : Features.CORE_FEATURES) {
            if (standardFeature.equals(feature)) {
                return true;
            }
        }
        return false;
    }

    private MCPSpecification() {
        // Prevent instantiation of this class
    }
}