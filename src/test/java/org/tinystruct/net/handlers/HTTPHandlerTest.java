package org.tinystruct.net.handlers;

import org.junit.jupiter.api.Test;
import org.tinystruct.data.Attachment;
import org.tinystruct.data.Attachments;
import org.tinystruct.net.URLRequest;
import org.tinystruct.net.URLResponse;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class HTTPHandlerTest {

    @Test
    public void testGetRequestWithParameters() throws Exception {
        // Use httpbin.org to test a GET request.
        URL url = URI.create("https://httpbin.org/get").toURL();
        URLRequest request = new URLRequest(url);
        request.setMethod("GET");
        request.setHeader("Accept", "application/json");

        // Add a query parameter.
        request.setParameter("testParam", "testValue");

        // Use our HTTPHandler to handle the request.
        HTTPHandler handler = new HTTPHandler();
        URLResponse response = handler.handleRequest(request);

        // Assert that we received a successful response.
        assertEquals(200, response.getStatusCode(), "Expected HTTP status 200");

        String body = response.getBody();
        assertNotNull(body, "Response body should not be null");

        // httpbin.org returns a JSON that includes the 'args' object with the query parameters.
        // We verify that our parameter appears in the response.
        assertTrue(body.contains("\"testParam\": \"testValue\""),
                "Response body should contain the query parameter 'testParam'");
    }

    @Test
    void testHandleRequestAsync() throws InterruptedException, MalformedURLException, ExecutionException {
        // Given
        URL url = URI.create("https://httpbin.org/get").toURL();
        URLRequest request = new URLRequest(url);
        HTTPHandler handler = new HTTPHandler();

        // When
        CompletableFuture<URLResponse> futureResponse = handler.handleRequestAsync(request);

        // Then
        assertNotNull(futureResponse);

        URLResponse response = futureResponse.get(); // Blocking to retrieve response for assertion
        assertNotNull(response);
        assertEquals(200, response.getStatusCode(), "Response status should be 200.");
        assertTrue(response.getBody().contains("\"url\": \"https://httpbin.org/get\""), "Response should contain request URL.");
    }

    @Test
    public void testFileUpload() throws Exception {
        // Create a URLRequest for a file upload (POST) to httpbin.org.
        URL url = URI.create("https://httpbin.org/post").toURL();
        URLRequest request = new URLRequest(url);
        request.setMethod("POST");
        // Set the header so that HTTPHandler uses the multipart logic.
        request.setHeader("Content-Type", "multipart/form-data");

        // Optionally, add a simple text parameter.
        request.setParameter("param1", "value1");

        // Create dummy file attachment data.
        byte[] fileData = "Hello, file upload!".getBytes(StandardCharsets.UTF_8);
        DummyAttachment dummyAttachment = new DummyAttachment("test.txt", fileData);
        DummyAttachments dummyAttachments = new DummyAttachments(dummyAttachment.getFilename(), List.of(dummyAttachment));

        // Set the attachments in the URLRequest.
        request.setAttachments(dummyAttachments);

        // Execute the request using the HTTPHandler.
        HTTPHandler handler = new HTTPHandler();
        URLResponse response = handler.handleRequest(request);

        // Assert that we received a successful response.
        assertEquals(200, response.getStatusCode(), "Expected HTTP status 200");

        String body = response.getBody();
        assertNotNull(body, "Response body should not be null");

        // httpbin.org returns a JSON response with "files" and "form" fields.
        // Check that the response contains our file name and file content.
        assertTrue(body.contains("test.txt"), "Response should contain the filename 'test.txt'");
        assertTrue(body.contains("Hello, file upload!"), "Response should contain the file content");
    }

    @Test
    void testHandleSSEAutoDetection() throws Exception {
        URLRequest request = new URLRequest(URI.create("https://httpbin.org/stream/3").toURL());
        HTTPHandler handler = new HTTPHandler();

        AtomicInteger eventCounter = new AtomicInteger();

        URLResponse response = handler.handleRequest(request, (s) -> {
            System.out.println(s);
            eventCounter.incrementAndGet();
        });

        assertEquals(200, response.getStatusCode());
        assertEquals(3, eventCounter.get(), "Should receive 3 SSE events");
    }

    public static class DummyAttachment extends Attachment {
        private final String filename;
        private final byte[] data;

        public DummyAttachment(String filename, byte[] data) {
            this.filename = filename;
            this.data = data;
        }

        public String getFilename() {
            return filename;
        }

        public byte[] get() {
            return data;
        }
    }

    public static class DummyAttachments extends Attachments {

        public DummyAttachments(String parameterName, List<Attachment> attachments) {
            super(parameterName, attachments);
        }

    }
}
