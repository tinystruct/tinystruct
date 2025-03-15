package org.tinystruct.mcp;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MCPClient {
    private static final Logger LOGGER = Logger.getLogger(MCPClient.class.getName());
    private final String baseUrl;
    private final String authToken;
    private String clientId;

    public MCPClient(String baseUrl, String authToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.authToken = authToken;
    }

    public void connect() throws IOException {
        // Initialize connection
        JsonRpcResponse initResponse = sendRequest("mcp/initialize", null);
        Builder result = initResponse.getResult();
        this.clientId = result.get("serverId").toString();
        LOGGER.info("Connected to server. Client ID: " + clientId);

        // Get capabilities
        JsonRpcResponse capabilitiesResponse = sendRequest("mcp/capabilities", null);
        LOGGER.info("Server capabilities: " + capabilitiesResponse.getResult());

        // Start SSE connection
        startEventStream();
    }

    public void disconnect() throws IOException {
        sendRequest("mcp/shutdown", null);
        LOGGER.info("Disconnected from server");
    }

    private JsonRpcResponse sendRequest(String endpoint, String body) throws IOException {
        URL url = new URL(baseUrl +"/?q="+ endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");

        if (authToken != null) {
            conn.setRequestProperty("Authorization", authToken);
        }

        try {
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Request failed with response code: " + responseCode);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
            jsonRpcResponse.parse(response.toString());
            return jsonRpcResponse;
        } catch (ApplicationException e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }
    }

    private void startEventStream() {
        Thread eventThread = new Thread(() -> {
            try {
                URL url = new URL(baseUrl + "?q=mcp/events&token=" + authToken);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "text/event-stream");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("event: ")) {
                        String event = line.substring(7);
                        String data = reader.readLine().substring(6); // Skip "data: "
                        handleEvent(event, data);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error in SSE connection", e);
            }
        });
        eventThread.setDaemon(true);
        eventThread.start();
    }

    private void handleEvent(String event, String data) {
        LOGGER.info("Received event: " + event + ", data: " + data);
    }

    public static void main(String[] args) {
        String serverUrl = "http://localhost:8081/";
        String authToken = "123456"; // Should match server's token

        MCPClient client = new MCPClient(serverUrl, authToken);
        try {
            client.connect();
            // Keep the main thread alive for a while to receive events
            Thread.sleep(30000);
            client.disconnect();
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error running client", e);
        }
    }
}