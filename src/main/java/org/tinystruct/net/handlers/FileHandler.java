package org.tinystruct.net.handlers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tinystruct.ApplicationException;
import org.tinystruct.net.URLHandler;
import org.tinystruct.net.URLRequest;
import org.tinystruct.net.URLResponse;

public class FileHandler implements URLHandler {
    private static final Logger logger = Logger.getLogger(FileHandler.class.getName());

    @Override
    public URLResponse handleRequest(URLRequest request) throws ApplicationException {
        try {
            URL url = request.getURL();
            File file = new File(url.getPath());
            
            if (!file.exists()) {
                throw new IOException("File not found: " + file.getPath());
            }

            if (!file.canRead()) {
                throw new IOException("Cannot read file: " + file.getPath());
            }

            return new FileResponse(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error handling file request", e);
            throw new ApplicationException("Error handling file request: " + e.getMessage());
        }
    }

    /**
     * @param request
     * @param consumer
     * @return
     * @throws ApplicationException
     */
    @Override
    public URLResponse handleRequest(URLRequest request, Consumer<String> consumer) throws ApplicationException {
        throw new ApplicationException("Not implemented yet.");
    }

    @Override
    public CompletableFuture<URLResponse> handleRequestAsync(URLRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return handleRequest(request);
            } catch (ApplicationException e) {
                throw new CompletionException(e);
            }
        });
    }
}

class FileResponse implements URLResponse {
    private final int statusCode;
    private final String body;
    private final Map<String, List<String>> headers;

    public FileResponse(File file) throws IOException {
        this.statusCode = 200; // Default success status
        
        // Read the file content
        this.body = Files.readString(file.toPath());
        
        // Set basic file headers
        this.headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList(Files.probeContentType(file.toPath())));
        headers.put("Content-Length", Collections.singletonList(String.valueOf(file.length())));
        headers.put("Last-Modified", Collections.singletonList(new Date(file.lastModified()).toString()));
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }
} 