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
package org.tinystruct.http;

import org.tinystruct.ApplicationException;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MultipartData {

    private final Request<?, InputStream> request;
    private final InputStream inputStream;
    private String boundary;
    private int bufferSize = 4 * 1024;

    public MultipartData(Request<?, InputStream> request) throws ApplicationException {
        this.request = request;
        this.inputStream = request.stream();
        parseRequest();
    }

    private void parseRequest() throws ApplicationException {
        Object contentTypeObj = request.headers().get(Header.CONTENT_TYPE);
        if (contentTypeObj == null)
            throw new ApplicationException("MultipartData: invalid multipart request data");

        String contentType = contentTypeObj.toString();
        boundary = parseBoundary(contentType);

        if (boundary == null || boundary.isEmpty()) {
            throw new ApplicationException("MultipartData: cannot retrieve boundary for multipart request");
        }

        String firstLine = readLine();
        if (firstLine == null || !firstLine.startsWith(boundary)) {
            throw new ApplicationException("MultipartData: invalid multipart request data");
        }
    }

    public static String parseBoundary(String contentType) {
        if (contentType.lastIndexOf("boundary=") != -1) {
            String _boundary = "--" + contentType.substring(contentType.lastIndexOf("boundary=") + 9);
            if (_boundary.endsWith("\n")) {
                return _boundary.substring(0, _boundary.length() - 1);
            }
            return _boundary;
        }
        return null;
    }

    public ContentDisposition getNextPart() throws ApplicationException {
        String disposition = readLine();
        if (disposition != null && disposition.startsWith("Content-Disposition")) {
            disposition = new String(disposition.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

            String name = parseForAttribute("name", disposition);
            String filename = parseForAttribute("filename", disposition);
            String contentType = null;

            if (filename != null) {
                filename = new File(filename).getName();
                int colonIndex = filename.indexOf(":");
                int slashIndex = filename.lastIndexOf("\\");
                if (colonIndex > -1 && slashIndex > -1) {
                    filename = filename.substring(slashIndex + 1);
                }

                String contentTypeLine = readLine();
                if (contentTypeLine != null && contentTypeLine.startsWith("Content-Type: ")) {
                    contentType = contentTypeLine.substring(14).trim();
                }
            }

            // Skip until data starts (empty line)
            String line;
            while ((line = readLine()) != null && !line.trim().isEmpty()) {
                // skip headers if any
            }

            java.io.ByteArrayOutputStream dataStream = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            
            // This is a bit tricky with standard InputStream as we need to find the boundary
            // For simplicity in this implementation, we read line by line until boundary
            // Note: This might not be efficient for large binary files
            while ((line = readLine()) != null && !line.startsWith(boundary)) {
                try {
                    dataStream.write(line.getBytes(StandardCharsets.ISO_8859_1));
                } catch (IOException e) {
                    throw new ApplicationException(e.getMessage(), e);
                }
            }

            byte[] data = dataStream.toByteArray();
            // Remove trailing CRLF from data if it exists and was part of the multipart formatting
            if (data.length >= 2 && data[data.length-2] == '\r' && data[data.length-1] == '\n') {
                byte[] newData = new byte[data.length - 2];
                System.arraycopy(data, 0, newData, 0, data.length - 2);
                data = newData;
            } else if (data.length >= 1 && data[data.length-1] == '\n') {
                byte[] newData = new byte[data.length - 1];
                System.arraycopy(data, 0, newData, 0, data.length - 1);
                data = newData;
            }

            return new ContentDisposition(name, filename, contentType, data);
        }
        return null;
    }

    private static String parseForAttribute(String attribute, String parseString) {
        int nameIndex = parseString.indexOf(attribute + "=\"");
        if (nameIndex != -1) {
            int endQuoteIndex = parseString.indexOf("\"", nameIndex + attribute.length() + 3);
            if (endQuoteIndex != -1) {
                return parseString.substring(nameIndex + attribute.length() + 2, endQuoteIndex);
            }
        }
        return null;
    }

    protected String readLine() throws ApplicationException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int b;
        try {
            while ((b = inputStream.read()) != -1) {
                baos.write(b);
                if (b == '\n') break;
            }
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        if (baos.size() == 0) return null;
        return baos.toString(StandardCharsets.ISO_8859_1);
    }
}
