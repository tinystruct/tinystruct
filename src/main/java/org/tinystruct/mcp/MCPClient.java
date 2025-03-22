package org.tinystruct.mcp;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.tinystruct.mcp.MCPSpecification.*;

public class MCPClient {
    private static final Logger LOGGER = Logger.getLogger(MCPClient.class.getName());
    private final String baseUrl;
    private final String authToken;
    private String clientId;
    private SessionState sessionState = SessionState.DISCONNECTED;
    
    // Add client-side session state tracking
    public enum SessionState {
        DISCONNECTED,
        INITIALIZING,
        READY,
        ERROR
    }

    // Add protocol constants
    private static final class Methods {
        static final String INITIALIZE = "initialize";
        static final String SHUTDOWN = "shutdown";
        static final String GET_STATUS = "getStatus";
        static final String GET_CAPABILITIES = "getCapabilities";
    }

    // Add missing protocol constants
    private static final class Features {
        static final String BASE = "base";
        static final String LIFECYCLE = "lifecycle";
        static final String RESOURCES = "resources";
        static final String SSE = "sse";
        static final String JSON_RPC = "json-rpc";
    }

    private static final class ErrorCodes {
        static final int PARSE_ERROR = -32700;
        static final int INVALID_REQUEST = -32600;
        static final int METHOD_NOT_FOUND = -32601;
        static final int INVALID_PARAMS = -32602;
        static final int INTERNAL_ERROR = -32603;
        static final int UNAUTHORIZED = -32001;
        static final int ALREADY_INITIALIZED = -32002;
        static final int NOT_INITIALIZED = -32003;
    }

    public MCPClient(String baseUrl, String authToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.authToken = authToken;
        this.sessionState = SessionState.DISCONNECTED;
    }

    public void connect() throws IOException {
        if (sessionState != SessionState.DISCONNECTED) {
            throw new IllegalStateException("Client already connected");
        }

        try {
            sessionState = SessionState.INITIALIZING;
            
            // Initialize connection
            JsonRpcResponse initResponse = sendRequest(Methods.INITIALIZE, null);
            if (initResponse.hasError()) {
                throw new IOException("Initialization failed: " + initResponse.getError().getMessage());
            }
            
            Builder result = initResponse.getResult();
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
            
            sessionState = SessionState.READY;
        } catch (Exception e) {
            sessionState = SessionState.ERROR;
            throw new IOException("Connection failed", e);
        }
    }

    public void disconnect() throws IOException {
        if (sessionState != SessionState.READY) {
            throw new IllegalStateException("Client not in READY state");
        }

        try {
            JsonRpcResponse response = sendRequest(Methods.SHUTDOWN, null);
            if (response.hasError()) {
                throw new IOException("Shutdown failed: " + response.getError().getMessage());
            }
            sessionState = SessionState.DISCONNECTED;
            LOGGER.info("Disconnected from server");
        } catch (Exception e) {
            sessionState = SessionState.ERROR;
            throw new IOException("Disconnect failed", e);
        }
    }

    private JsonRpcResponse sendRequest(String method, String params) throws IOException {
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
            request.setParams(new Builder(params));
        }

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes(StandardCharsets.UTF_8));
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
                
                JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
                jsonRpcResponse.parse(response.toString());
                return jsonRpcResponse;
            }
        } catch (ApplicationException e) {
            throw new IOException("Failed to parse response", e);
        } finally {
            conn.disconnect();
        }
    }

    private void startEventStream() {
        Thread eventThread = new Thread(() -> {
            try {
                URL url = new URL(baseUrl + "?q=" + Endpoints.EVENTS + "&" + Http.TOKEN_PARAM + "=" + authToken);
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

    public SessionState getSessionState() {
        return sessionState;
    }
}