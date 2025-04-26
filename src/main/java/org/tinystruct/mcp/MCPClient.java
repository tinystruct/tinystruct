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
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.tinystruct.mcp.MCPSpecification.*;

/**
 * Client implementation for the Model Context Protocol (MCP).
 * Provides a unified interface for interacting with MCP servers, treating tools as resources.
 * <p>
 * This client provides a Java interface for interacting with MCP servers. It handles
 * connection management, resource discovery, and resource execution.
 * </p>
 * <p>
 * The client uses a unified resource model where tools, data sources, and templates
 * are all treated as resources that can be discovered and executed.
 * </p>
 * <p>
 * Usage example:
 * <pre>
 * MCPClient client = new MCPClient("http://localhost:8080", "auth-token");
 * client.connect();
 *
 * // List available resources
 * List<MCPResource> resources = client.listResources();
 *
 * // Execute a resource
 * Map<String, Object> params = new HashMap<>();
 * params.put("param1", "value1");
 * Object result = client.executeResource("resource-name", params);
 *
 * client.disconnect();
 * </pre>
 * </p>
 */
public class MCPClient {
    //--------------------------------------------------------------------------
    // Constants and Fields
    //--------------------------------------------------------------------------
    private static final Logger LOGGER = Logger.getLogger(MCPClient.class.getName());
    private final String baseUrl;
    private final String authToken;
    private String clientId;
    private SessionState sessionState = SessionState.DISCONNECTED;

    // Cache for discovered resources
    private final Map<String, MCPResource> resourceCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new MCPClient with the specified base URL and authentication token.
     * <p>
     * The base URL should point to the root of the MCP server, e.g., "http://localhost:8080".
     * The authentication token is optional and may be null if the server does not require authentication.
     * </p>
     *
     * @param baseUrl The base URL of the MCP server (must not be null)
     * @param authToken The authentication token (may be null)
     * @throws IllegalArgumentException If baseUrl is null
     */
    public MCPClient(String baseUrl, String authToken) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("Base URL must not be null");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.authToken = authToken;
        this.sessionState = SessionState.DISCONNECTED;
    }

    /**
     * Connects to the MCP server and initializes the session.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Initializes the connection with the server</li>
     *   <li>Gets the server capabilities</li>
     *   <li>Starts the event stream for server-sent events</li>
     *   <li>Discovers available resources (tools, data resources, prompts)</li>
     * </ol>
     * </p>
     * <p>
     * After a successful connection, the client is in the READY state and can be used
     * to execute resources.
     * </p>
     *
     * @throws IOException If an error occurs during connection
     * @throws IllegalStateException If the client is already connected
     */
    public void connect() throws IOException {
        if (sessionState != SessionState.DISCONNECTED) {
            throw new IllegalStateException("Client already connected (state: " + sessionState + ")");
        }

        try {
            LOGGER.info("Connecting to MCP server at " + baseUrl);
            sessionState = SessionState.INITIALIZING;

            // Initialize connection
            JsonRpcResponse initResponse = sendRequest(Methods.INITIALIZE, null);
            if (initResponse.hasError()) {
                throw new IOException("Initialization failed: " + initResponse.getError().getMessage());
            }

            Builder result = initResponse.getResult();
            if (result == null || result.get("serverId") == null) {
                throw new IOException("Invalid initialization response: missing serverId");
            }

            this.clientId = result.get("serverId").toString();
            LOGGER.info("Connected to server. Client ID: " + clientId);

            // Get capabilities
            JsonRpcResponse capabilitiesResponse = sendRequest(Methods.GET_CAPABILITIES, null);
            if (capabilitiesResponse.hasError()) {
                throw new IOException("Failed to get capabilities: " + capabilitiesResponse.getError().getMessage());
            }
            LOGGER.info("Server capabilities: " + capabilitiesResponse.getResult());

            // Start SSE connection
            startEventStream();

            // Discover resources (tools, data resources, prompts)
            discoverResources();

            sessionState = SessionState.READY;
            LOGGER.info("MCP client ready");
        } catch (Exception e) {
            sessionState = SessionState.ERROR;
            LOGGER.log(Level.SEVERE, "Connection failed", e);
            throw new IOException("Connection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Disconnects from the MCP server.
     * <p>
     * This method sends a shutdown request to the server and cleans up resources.
     * After a successful disconnection, the client is in the DISCONNECTED state.
     * </p>
     * <p>
     * If the client is not in the READY state, an IllegalStateException is thrown.
     * </p>
     *
     * @throws IOException If an error occurs during disconnection
     * @throws IllegalStateException If the client is not in the READY state
     */
    public void disconnect() throws IOException {
        if (sessionState != SessionState.READY) {
            throw new IllegalStateException("Client not in READY state (state: " + sessionState + ")");
        }

        try {
            LOGGER.info("Disconnecting from MCP server");
            JsonRpcResponse response = sendRequest(Methods.SHUTDOWN, null);
            if (response.hasError()) {
                throw new IOException("Shutdown failed: " + response.getError().getMessage());
            }

            // Clear the resource cache
            resourceCache.clear();

            sessionState = SessionState.DISCONNECTED;
            LOGGER.info("Disconnected from server");
        } catch (Exception e) {
            sessionState = SessionState.ERROR;
            LOGGER.log(Level.SEVERE, "Disconnect failed", e);
            throw new IOException("Disconnect failed: " + e.getMessage(), e);
        }
    }

    /**
     * Discovers all available resources from the server.
     * This includes tools, data resources, and prompts.
     *
     * @throws IOException If an error occurs during discovery
     */
    private void discoverResources() throws IOException {
        try {
            // Discover tools
            discoverTools();

            // Discover data resources
            discoverDataResources();

            // Discover prompts
            discoverPrompts();

            LOGGER.info("Discovered " + resourceCache.size() + " resources");
        } catch (Exception e) {
            throw new IOException("Resource discovery failed", e);
        }
    }

    /**
     * Discovers all available tools from the server.
     *
     * @throws IOException If an error occurs during discovery
     */
    private void discoverTools() throws IOException {
        try {
            JsonRpcResponse response = sendRequest(Methods.LIST_TOOLS, null);
            if (response.hasError()) {
                throw new IOException("Tool discovery failed: " + response.getError().getMessage());
            }

            Builder result = response.getResult();
            Builders tools = (Builders) result.get("tools");

            for (Builder toolData : tools) {
                String name = (String) toolData.get("name");
                String description = (String) toolData.get("description");
                Builder schema = (Builder) toolData.get("schema");

                MCPTool tool = new MCPTool(name, description, schema, this);
                resourceCache.put(name, tool);
            }

            LOGGER.info("Discovered " + tools.size() + " tools");
        } catch (Exception e) {
            throw new IOException("Tool discovery failed", e);
        }
    }

    /**
     * Discovers all available data resources from the server.
     *
     * @throws IOException If an error occurs during discovery
     */
    private void discoverDataResources() throws IOException {
        try {
            JsonRpcResponse response = sendRequest(Methods.LIST_RESOURCES, null);
            if (response.hasError()) {
                throw new IOException("Resource discovery failed: " + response.getError().getMessage());
            }

            Builder result = response.getResult();
            Builders resources = (Builders) result.get("resources");

            for (Builder resourceData : resources) {
                String name = (String) resourceData.get("name");
                String description = (String) resourceData.get("description");
                String uriTemplate = (String) resourceData.get("uriTemplate");

                MCPDataResource resource = new MCPDataResource(name, description, uriTemplate, this);
                resourceCache.put(name, resource);
            }

            LOGGER.info("Discovered " + resources.size() + " data resources");
        } catch (Exception e) {
            throw new IOException("Resource discovery failed", e);
        }
    }

    /**
     * Discovers all available prompts from the server.
     *
     * @throws IOException If an error occurs during discovery
     */
    private void discoverPrompts() throws IOException {
        try {
            JsonRpcResponse response = sendRequest(Methods.LIST_PROMPTS, null);
            if (response.hasError()) {
                throw new IOException("Prompt discovery failed: " + response.getError().getMessage());
            }

            Builder result = response.getResult();
            Builders prompts = (Builders) result.get("prompts");

            for (Builder promptData : prompts) {
                String name = (String) promptData.get("name");
                String description = (String) promptData.get("description");

                // Create a prompt resource (implementation not shown)
                // MCPPromptResource prompt = new MCPPromptResource(name, description, this);
                // resourceCache.put(name, prompt);
            }

            LOGGER.info("Discovered " + prompts.size() + " prompts");
        } catch (Exception e) {
            throw new IOException("Prompt discovery failed", e);
        }
    }

    private JsonRpcResponse sendRequest(String method, Builder params) throws IOException {
        URL url = new URL(baseUrl + "?q=" + Endpoints.RPC);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");  // Changed to POST for JSON-RPC
        conn.setRequestProperty("Content-Type", Http.CONTENT_TYPE_JSON);
        conn.setDoOutput(true);

        if (authToken != null) {
            conn.setRequestProperty(Http.AUTH_HEADER, authToken);
        }

        // Create JSON-RPC request
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod(method);
        request.setId(UUID.randomUUID().toString());
        if (params != null) {
            try {
                request.setParams(params);
            } catch (Exception e) {
                LOGGER.severe("Error setting params: " + params + ", error: " + e.getMessage());
                throw new IOException("Error setting params: " + e.getMessage(), e);
            }
        }

        // Log the full request
        String requestString = request.toString();
        LOGGER.info("Full JSON-RPC request: " + requestString);

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestString.getBytes(StandardCharsets.UTF_8));
        }

        try {
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Request failed with response code: " + responseCode);
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                String responseString = response.toString();
                LOGGER.info("Raw server response: " + responseString);

                JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
                try {
                    jsonRpcResponse.parse(responseString);
                } catch (ApplicationException e) {
                    LOGGER.severe("Error parsing response: " + responseString + ", error: " + e.getMessage());
                    throw new IOException("Error parsing response: " + e.getMessage(), e);
                }

                if (jsonRpcResponse.hasError()) {
                    LOGGER.warning("Server returned error: " + jsonRpcResponse.getError().getMessage() +
                                  " (code: " + jsonRpcResponse.getError().getCode() + ")");
                } else {
                    LOGGER.info("Server returned result: " + jsonRpcResponse.getResult());
                }

                return jsonRpcResponse;
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException("Failed to process response", e);
            }
        } finally {
            conn.disconnect();
        }
    }

    private void startEventStream() {
        Thread eventThread = new Thread(() -> {
            try {
                URL url = URI.create(baseUrl + "?q=" + Endpoints.EVENTS + "&" + Http.TOKEN_PARAM + "=" + authToken).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", Http.CONTENT_TYPE_SSE);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && sessionState == SessionState.READY) {
                        if (line.startsWith("event: ")) {
                            String event = line.substring(7);
                            String data = reader.readLine();
                            if (data != null && data.startsWith("data: ")) {
                                data = data.substring(6);
                                handleEvent(event, data);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error in SSE connection", e);
                sessionState = SessionState.ERROR;
            }
        });
        eventThread.setDaemon(true);
        eventThread.start();
    }

    private void handleEvent(String event, String data) {
        switch (event) {
            case Events.STATE:
                handleStateEvent(data);
                break;
            case Events.ERROR:
                handleErrorEvent(data);
                break;
            case Events.CLOSE:
                handleCloseEvent(data);
                break;
            case Events.RESOURCES_CHANGED:
                handleResourcesChangedEvent(data);
                break;
            case Events.TOOLS_CHANGED:
                handleToolsChangedEvent(data);
                break;
            case Events.PROMPTS_CHANGED:
                handlePromptsChangedEvent(data);
                break;
            default:
                LOGGER.info("Received event: " + event + ", data: " + data);
        }
    }

    private void handleStateEvent(String data) {
        try {
            Builder stateData = new Builder(data);
            String newState = stateData.get("state").toString();
            LOGGER.info("Server state changed to: " + newState);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse state event", e);
        }
    }

    private void handleErrorEvent(String data) {
        try {
            Builder errorData = new Builder(data);
            LOGGER.warning("Server error: " + errorData.get("message"));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse error event", e);
        }
    }

    private void handleCloseEvent(String data) {
        sessionState = SessionState.DISCONNECTED;
        LOGGER.info("Server closed connection: " + data);
    }

    /**
     * Handles a resources changed event from the server.
     *
     * @param data The event data
     */
    private void handleResourcesChangedEvent(String data) {
        try {
            // Rediscover data resources
            discoverDataResources();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to handle resources changed event", e);
        }
    }

    /**
     * Handles a tools changed event from the server.
     *
     * @param data The event data
     */
    private void handleToolsChangedEvent(String data) {
        try {
            // Rediscover tools
            discoverTools();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to handle tools changed event", e);
        }
    }

    /**
     * Handles a prompts changed event from the server.
     *
     * @param data The event data
     */
    private void handlePromptsChangedEvent(String data) {
        try {
            // Rediscover prompts
            discoverPrompts();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to handle prompts changed event", e);
        }
    }

    private void handleError(JsonRpcError error) throws IOException {
        LOGGER.warning("Server returned error: " + error.getMessage() + " (code: " + error.getCode() + ")");

        switch (error.getCode()) {
            case ErrorCodes.UNAUTHORIZED:
                throw new SecurityException("Unauthorized: " + error.getMessage());
            case ErrorCodes.NOT_INITIALIZED:
                synchronized(this) {
                    sessionState = SessionState.DISCONNECTED;
                }
                throw new IllegalStateException(error.getMessage());
            case ErrorCodes.ALREADY_INITIALIZED:
                throw new IllegalStateException(error.getMessage());
            default:
                throw new IOException("Server error: " + error.getMessage());
        }
    }

    /**
     * Lists all available resources.
     *
     * @return A list of all resources
     * @throws MCPException If an error occurs
     */
    public List<MCPResource> listResources() throws MCPException {
        if (sessionState != SessionState.READY) {
            throw new MCPException("Client not in READY state");
        }

        return new ArrayList<>(resourceCache.values());
    }

    /**
     * Lists resources of a specific type.
     *
     * @param type The resource type to filter by
     * @return A list of resources of the specified type
     * @throws MCPException If an error occurs
     */
    public List<MCPResource> listResourcesByType(MCPResource.ResourceType type) throws MCPException {
        if (sessionState != SessionState.READY) {
            throw new MCPException("Client not in READY state");
        }

        return resourceCache.values().stream()
                .filter(resource -> resource.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Gets a resource by name.
     *
     * @param name The name of the resource
     * @return The resource, or null if not found
     * @throws MCPException If an error occurs
     */
    public MCPResource getResource(String name) throws MCPException {
        if (sessionState != SessionState.READY) {
            throw new MCPException("Client not in READY state");
        }

        return resourceCache.get(name);
    }

    /**
     * Executes a resource with the given parameters.
     *
     * @param name The name of the resource
     * @param parameters The parameters to use for execution
     * @return The result of the execution
     * @throws MCPException If an error occurs
     */
    public Object executeResource(String name, Builder parameters) throws MCPException {
        if (sessionState != SessionState.READY) {
            throw new MCPException("Client not in READY state");
        }

        MCPResource resource = resourceCache.get(name);
        if (resource == null) {
            throw new MCPException("Resource not found: " + name);
        }

        return resource.execute(parameters);
    }

    /**
     * Calls a tool with the given parameters.
     *
     * @param name The name of the tool
     * @param parameters The parameters to use for execution
     * @return The result of the tool execution
     * @throws IOException If an error occurs
     */
    public Object callTool(String name, Builder parameters) throws IOException {
        if (sessionState != SessionState.READY) {
            throw new IllegalStateException("Client not in READY state");
        }

        try {
            Builder params = new Builder();
            params.put("name", name);
            params.put("parameters", parameters);

            LOGGER.info("Sending JSON-RPC request: method=" + Methods.CALL_TOOL + ", params=" + params);

            JsonRpcResponse response = sendRequest(Methods.CALL_TOOL, params);
            if (response.hasError()) {
                throw new IOException("Tool execution failed: " + response.getError().getMessage());
            }

            return response.getResult();
        } catch (Exception e) {
            throw new IOException("Tool execution failed", e);
        }
    }

    /**
     * Reads a resource with the given URI.
     *
     * @param uri The URI of the resource
     * @return The resource content
     * @throws IOException If an error occurs
     */
    public Object readResource(String uri) throws IOException {
        if (sessionState != SessionState.READY) {
            throw new IllegalStateException("Client not in READY state");
        }

        try {
            Builder params = new Builder();
            params.put("uri", uri);

            JsonRpcResponse response = sendRequest(Methods.READ_RESOURCE, params);
            if (response.hasError()) {
                throw new IOException("Resource read failed: " + response.getError().getMessage());
            }

            return response.getResult();
        } catch (Exception e) {
            throw new IOException("Resource read failed", e);
        }
    }

    /**
     * Gets the current session state.
     *
     * @return The session state
     */
    public SessionState getSessionState() {
        return sessionState;
    }
}