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
 * <p>
 * This class defines the standard constants, methods, and structures
 * for the Model Context Protocol to ensure consistency between
 * client and server implementations.
 * </p>
 * <p>
 * The Model Context Protocol (MCP) is a protocol for interacting with AI models
 * and related services. It provides a unified interface for discovering and
 * executing tools, accessing data resources, and managing prompts.
 * </p>
 * <p>
 * This specification includes:
 * <ul>
 *   <li>Protocol constants (version, ID)</li>
 *   <li>Feature identifiers</li>
 *   <li>Method names</li>
 *   <li>Event types</li>
 *   <li>HTTP constants</li>
 *   <li>Error codes</li>
 *   <li>Session state enum</li>
 *   <li>JSON-RPC constants</li>
 * </ul>
 * </p>
 */
public final class MCPSpecification {
    // Private constructor to prevent instantiation
    private MCPSpecification() {
        throw new AssertionError("MCPSpecification is a utility class and should not be instantiated");
    }

    //--------------------------------------------------------------------------
    // Protocol Constants
    //--------------------------------------------------------------------------

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

    //--------------------------------------------------------------------------
    // Feature Identifiers
    //--------------------------------------------------------------------------

    /**
     * Standard feature identifiers for MCP capabilities.
     * <p>
     * These identifiers are used to advertise and negotiate capabilities
     * between clients and servers.
     * </p>
     */
    public static final class Features {
        /**
         * Base protocol support (required)
         */
        public static final String BASE = "base";

        /**
         * Lifecycle management (initialize, shutdown)
         */
        public static final String LIFECYCLE = "lifecycle";

        /**
         * Resource discovery and access
         */
        public static final String RESOURCES = "resources";

        /**
         * Tool discovery and execution
         */
        public static final String TOOLS = "tools";

        /**
         * Prompt template discovery and rendering
         */
        public static final String PROMPTS = "prompts";

        /**
         * Server-Sent Events support
         */
        public static final String SSE = "sse";

        /**
         * JSON-RPC protocol support
         */
        public static final String JSON_RPC = "json-rpc";

        /**
         * Core features supported by all MCP implementations.
         * <p>
         * These features are required for basic MCP functionality.
         * </p>
         */
        public static final String[] CORE_FEATURES = {
            BASE, LIFECYCLE, RESOURCES, TOOLS, PROMPTS, SSE, JSON_RPC
        };

        private Features() {} // Prevent instantiation
    }

    //--------------------------------------------------------------------------
    // Method Names
    //--------------------------------------------------------------------------

    /**
     * Standard method names for MCP JSON-RPC requests.
     * <p>
     * These method names are used in JSON-RPC requests to invoke
     * specific operations on the MCP server.
     * </p>
     */
    public static final class Methods {
        /**
         * Initialize the connection with the server.
         * <p>
         * This method is called when a client connects to the server.
         * It establishes the session and returns server information.
         * </p>
         */
        public static final String INITIALIZE = "initialize";

        /**
         * Shutdown the connection with the server.
         * <p>
         * This method is called when a client disconnects from the server.
         * It cleans up resources and terminates the session.
         * </p>
         */
        public static final String SHUTDOWN = "shutdown";

        /**
         * Get the current status of the server.
         * <p>
         * This method returns information about the server's current state,
         * including uptime, load, and other metrics.
         * </p>
         */
        public static final String GET_STATUS = "get-status";

        /**
         * Get the capabilities of the server.
         * <p>
         * This method returns information about the server's capabilities,
         * including supported features and versions.
         * </p>
         */
        public static final String GET_CAPABILITIES = "get-capabilities";

        /**
         * List available tools.
         * <p>
         * This method returns a list of tools available on the server,
         * including their names, descriptions, and schemas.
         * </p>
         */
        public static final String LIST_TOOLS = "list-tools";

        /**
         * Call a tool with parameters.
         * <p>
         * This method executes a tool with the specified parameters
         * and returns the result.
         * </p>
         */
        public static final String CALL_TOOL = "call-tool";

        /**
         * List available resources.
         * <p>
         * This method returns a list of resources available on the server,
         * including their names, descriptions, and URI templates.
         * </p>
         */
        public static final String LIST_RESOURCES = "list-resources";

        /**
         * Read a resource with the specified URI.
         * <p>
         * This method retrieves the content of a resource identified by
         * the specified URI.
         * </p>
         */
        public static final String READ_RESOURCE = "read-resource";

        /**
         * List available prompts.
         * <p>
         * This method returns a list of prompts available on the server,
         * including their names and descriptions.
         * </p>
         */
        public static final String LIST_PROMPTS = "list-prompts";

        /**
         * Get a prompt template by name.
         * <p>
         * This method retrieves a prompt template identified by the
         * specified name.
         * </p>
         */
        public static final String GET_PROMPT = "get-prompt";

        private Methods() {} // Prevent instantiation
    }

    //--------------------------------------------------------------------------
    // Error Codes
    //--------------------------------------------------------------------------

    /**
     * Standard error codes for MCP error responses.
     * <p>
     * Follows JSON-RPC 2.0 error code conventions with MCP-specific extensions.
     * </p>
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

    //--------------------------------------------------------------------------
    // Event Types
    //--------------------------------------------------------------------------

    /**
     * Server-Sent Events (SSE) event types.
     * <p>
     * These event types are used in SSE messages to notify clients
     * of server-side events and state changes.
     * </p>
     */
    public static final class Events {
        /**
         * Connection established event.
         * <p>
         * Sent when a client successfully connects to the server's SSE endpoint.
         * </p>
         */
        public static final String CONNECTED = "connected";

        /**
         * Server state change event.
         * <p>
         * Sent when the server's state changes, such as when it transitions
         * between different operational modes.
         * </p>
         */
        public static final String STATE = "state";

        /**
         * Error event.
         * <p>
         * Sent when an error occurs on the server that the client should
         * be aware of.
         * </p>
         */
        public static final String ERROR = "error";

        /**
         * Connection close event.
         * <p>
         * Sent when the server is about to close the SSE connection.
         * </p>
         */
        public static final String CLOSE = "close";

        /**
         * General notification event.
         * <p>
         * Sent for general notifications that don't fit into other categories.
         * </p>
         */
        public static final String NOTIFICATION = "notification";

        /**
         * Resources changed event.
         * <p>
         * Sent when the available resources on the server change, such as
         * when resources are added, removed, or updated.
         * </p>
         */
        public static final String RESOURCES_CHANGED = "resources_changed";

        /**
         * Tools changed event.
         * <p>
         * Sent when the available tools on the server change, such as
         * when tools are added, removed, or updated.
         * </p>
         */
        public static final String TOOLS_CHANGED = "tools_changed";

        /**
         * Prompts changed event.
         * <p>
         * Sent when the available prompts on the server change, such as
         * when prompts are added, removed, or updated.
         * </p>
         */
        public static final String PROMPTS_CHANGED = "prompts_changed";

        private Events() {} // Prevent instantiation
    }

    //--------------------------------------------------------------------------
    // HTTP Constants
    //--------------------------------------------------------------------------

    /**
     * HTTP header and parameter constants for MCP communication.
     */
    public static final class Http {
        public static final String AUTH_HEADER = "Authorization";
        public static final String CONTENT_TYPE_JSON = "application/json";
        public static final String CONTENT_TYPE_SSE = "text/event-stream";
        public static final String TOKEN_PARAM = "token";

        private Http() {} // Prevent instantiation
    }

    //--------------------------------------------------------------------------
    // Configuration Properties
    //--------------------------------------------------------------------------

    /**
     * Configuration property keys for MCP applications.
     */
    public static final class Config {
        public static final String AUTH_TOKEN = "mcp.auth.token";
        public static final String SERVER_PORT = "mcp.server.port";
        public static final String SERVER_HOST = "mcp.server.host";
        public static final String SESSION_TIMEOUT = "mcp.session.timeout";

        private Config() {} // Prevent instantiation
    }

    //--------------------------------------------------------------------------
    // Endpoint Paths
    //--------------------------------------------------------------------------

    /**
     * Endpoint paths for MCP server API.
     */
    public static final class Endpoints {
        public static final String INITIALIZE = "mcp/initialize";
        public static final String CAPABILITIES = "mcp/capabilities";
        public static final String SHUTDOWN = "mcp/shutdown";
        public static final String EVENTS = "mcp/events";
        public static final String SSE = "sse";

        private Endpoints() {} // Prevent instantiation
    }

    //--------------------------------------------------------------------------
    // JSON-RPC Constants
    //--------------------------------------------------------------------------

    /**
     * JSON-RPC related constants for MCP communication.
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

    //--------------------------------------------------------------------------
    // Utility Methods
    //--------------------------------------------------------------------------

    /**
     * Validates if a feature is a standard MCP feature.
     * <p>
     * This method checks if the given feature name is one of the standard
     * MCP features defined in the Features.CORE_FEATURES array.
     * </p>
     *
     * @param feature Feature name to validate
     * @return true if the feature is a standard MCP feature, false otherwise
     */
    public static boolean isStandardFeature(String feature) {
        for (String standardFeature : Features.CORE_FEATURES) {
            if (standardFeature.equals(feature)) {
                return true;
            }
        }
        return false;
    }
}