package org.tinystruct.system;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.mcp.*;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MCPServer extends AbstractApplication implements Bootstrap {
    private static final Logger LOGGER = Logger.getLogger(MCPServer.class.getName());
    private final ExecutorService executorService;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private int port = 8080;

    public MCPServer() {
        this.executorService = Executors.newCachedThreadPool();
        this.running = false;
    }

    @Override
    public void init() {
        this.setTemplateRequired(false);
    }

    @Action(value = "start", description = "Start up the MCP server.", options = {
            @Argument(key = "server-port", description = "Server port"),
            @Argument(key = "http.proxyHost", description = "Proxy host for http"),
            @Argument(key = "http.proxyPort", description = "Proxy port for http"),
            @Argument(key = "https.proxyHost", description = "Proxy host for https"),
            @Argument(key = "https.proxyPort", description = "Proxy port for https")
    }, example = "bin/dispatcher start --import org.tinystruct.system.MCPServer --server-port 777", mode = org.tinystruct.application.Action.Mode.CLI)
    @Override
    public void start() throws ApplicationException {
        if (getContext() != null) {
            if (getContext().getAttribute("--server-port") != null) {
                this.port = Integer.parseInt(getContext().getAttribute("--server-port").toString());
            }

            if (getContext().getAttribute("--http.proxyHost") != null && getContext().getAttribute("--http.proxyPort") != null) {
                System.setProperty("http.proxyHost", getContext().getAttribute("--http.proxyHost").toString());
                System.setProperty("http.proxyPort", getContext().getAttribute("--http.proxyPort").toString());
            }

            if (getContext().getAttribute("--https.proxyHost") != null && getContext().getAttribute("--https.proxyPort") != null) {
                System.setProperty("https.proxyHost", getContext().getAttribute("--https.proxyHost").toString());
                System.setProperty("https.proxyPort", getContext().getAttribute("--https.proxyPort").toString());
            }
        }

        System.out.println(ApplicationManager.call("--logo", null, org.tinystruct.application.Action.Mode.CLI));

        String charsetName = null;
        Settings settings = new Settings();
        if (settings.get("default.file.encoding") != null)
            charsetName = settings.get("default.file.encoding");

        if (charsetName != null && !charsetName.trim().isEmpty())
            System.setProperty("file.encoding", charsetName);

        settings.set("language", "zh_CN");
        if (settings.get("system.directory") == null)
            settings.set("system.directory", System.getProperty("user.dir"));

        try {
            // Initialize the application manager with the configuration.
            ApplicationManager.init(settings);
        } catch (ApplicationException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        long startTime = System.currentTimeMillis();
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            LOGGER.info("MCP Server started on port " + port);

            // Open the default browser
            getContext().setAttribute("--url", "mcp://localhost:" + port);
            ApplicationManager.call("open", getContext(), org.tinystruct.application.Action.Mode.CLI);

            LOGGER.info("MCP server (" + port + ") startup in " + (System.currentTimeMillis() - startTime) + " ms");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.execute(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        LOGGER.severe("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new ApplicationException("Failed to start MCP server: " + e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOGGER.severe("Error closing server socket: " + e.getMessage());
        }
        executorService.shutdown();
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final BufferedReader reader;
        private final PrintWriter writer;
        private boolean initialized = false;

        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            
            LOGGER.info("New client connected from " + socket.getInetAddress());
        }

        @Override
        public void run() {
            try {
                handleClient();
            } catch (IOException e) {
                LOGGER.severe("Error handling client: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void handleClient() throws IOException {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    processJsonRpcMessage(line);
                }
            }
        }

        private void processJsonRpcMessage(String jsonStr) {
            try {
                Builder jsonObject = new Builder();
                jsonObject.parse(jsonStr);
                
                // Check if it's a request, response, or notification
                if (jsonObject.containsKey("method")) {
                    if (jsonObject.containsKey("id")) {
                        // It's a request
                        JsonRpcRequest request = new JsonRpcRequest();
                        request.parse(jsonStr);
                        handleRequest(request);
                    } else {
                        // It's a notification
                        JsonRpcNotification notification = new JsonRpcNotification();
                        notification.parse(jsonStr);
                        handleNotification(notification);
                    }
                } else if (jsonObject.containsKey("result") || jsonObject.containsKey("error")) {
                    // It's a response
                    JsonRpcResponse response = new JsonRpcResponse();
                    response.parse(jsonStr);
                    handleResponse(response);
                }
            } catch (Exception e) {
                sendErrorResponse(null, -32700, "Parse error: " + e.getMessage());
            }
        }

        private void handleRequest(JsonRpcRequest request) {
            String method = request.getMethod();
            
            try {
                switch (method) {
                    case MCPLifecycle.METHOD_INITIALIZE:
                        if (!initialized) {
                            JsonRpcResponse response = MCPLifecycle.createInitializeResponse(
                                UUID.randomUUID().toString(),
                                new String[]{"base", "lifecycle", "resources"}
                            );
                            response.setId(request.getId());
                            sendResponse(response);
                            initialized = true;
                        } else {
                            sendErrorResponse(request.getId(), -32600, "Server already initialized");
                        }
                        break;
                        
                    case MCPLifecycle.METHOD_SHUTDOWN:
                        if (initialized) {
                            JsonRpcResponse response = MCPLifecycle.createShutdownResponse();
                            response.setId(request.getId());
                            sendResponse(response);
                            cleanup();
                        } else {
                            sendErrorResponse(request.getId(), -32600, "Server not initialized");
                        }
                        break;
                        
                    case MCPLifecycle.METHOD_CAPABILITIES:
                        // Handle capabilities request
                        Builder result = new Builder();
                        result.put("version", version());
                        JsonRpcResponse response = new JsonRpcResponse();
                        response.setId(request.getId());
                        response.setResult(result);
                        sendResponse(response);
                        break;
                        
                    default:
                        sendErrorResponse(request.getId(), -32601, "Method not found: " + method);
                }
            } catch (Exception e) {
                sendErrorResponse(request.getId(), -32603, "Internal error: " + e.getMessage());
            }
        }

        private void handleNotification(JsonRpcNotification notification) {
            // Handle notifications (no response needed)
            LOGGER.info("Received notification: " + notification.getMethod());
        }

        private void handleResponse(JsonRpcResponse response) {
            // Handle responses to our requests (if any)
            LOGGER.info("Received response for ID: " + response.getId());
        }

        private void sendResponse(JsonRpcResponse response) {
            writer.println(response.toString());
            writer.flush();
        }

        private void sendErrorResponse(String id, int code, String message) {
            JsonRpcResponse response = new JsonRpcResponse();
            response.setId(id);
            JsonRpcError error = new JsonRpcError(code, message);
            response.setError(error);
            writer.println(response.toString());
            writer.flush();
        }

        private void cleanup() {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {
                LOGGER.severe("Error during cleanup: " + e.getMessage());
            }
        }
    }
} 