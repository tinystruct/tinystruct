/*******************************************************************************
 * Copyright  (c) 2013, 2023 James Mover Zhou
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
package org.tinystruct.http.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.tinystruct.http.Header;
import org.tinystruct.http.Request;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The MultipartFormData class is responsible for reading the
 * input data of a multipart request and splitting it up into
 * input elements, wrapped inside of a
 * {@link org.tinystruct.transfer.http.upload.ContentDisposition ContentDisposition}
 * for easy definition.  To use this class, create a new instance
 * of MultipartFormData passing it a HttpServletRequest in the
 * constructor.  Then use the {@link #getNextPart() getNextPart}
 * method until it returns null, then you're finished.  Example: <br>
 * <pre>
 *      MultipartFormData iterator = new MultipartFormData(request);
 *      ContentDisposition element;
 *
 *      while ((element = iterator.getNextPart()) != null) {
 *           //do something with element
 *      }
 * </pre>
 *
 * @see org.tinystruct.transfer.http.upload.ContentDisposition
 */
public class MultipartFormData {

    /**
     * The request instance for this class
     */
    protected final Request<HttpServletRequest, ServletInputStream> request;

    /**
     * The input stream instance for this class
     */
    protected ServletInputStream inputStream;

    /**
     * The boundary for this multipart request
     */
    protected String boundary;

    /**
     * Whether or not the input stream is finished
     */
    protected boolean end = false;

    /**
     * The amount of data read from a request at a time.
     * This also represents the maximum size in bytes of
     * a line read from the request which defaults to 4 * 1024 (4 KB)
     */
    protected int bufferSize = 4 * 1024;

    public MultipartFormData(Request<HttpServletRequest, ServletInputStream> request) throws ServletException {
        this.request = request;

        parseRequest();
    }

    /**
     * Parses a content-type String for the boundary.  Appends a
     * "--" to the beginning of the boundary, because thats the
     * real boundary as opposed to the shortened one in the
     * content type.
     *
     * @param contentType content type
     * @return boundary
     */
    public static String parseBoundary(String contentType) {
        if (contentType.lastIndexOf("boundary=") != -1) {
            String _boundary = "--" + contentType.substring(contentType.lastIndexOf("boundary=") + 9);
            if (_boundary.endsWith("\n")) {
                //strip it off
                return _boundary.substring(0, _boundary.length() - 1);
            }
            return _boundary;
        }
        return null;
    }

    /**
     * Parses the "Content-Type" line of a multipart form for a content type
     *
     * @param contentTypeString A String representing the Content-Type line,
     *                          with a trailing "\n"
     * @return The content type specified, or <code>null</code> if one can't be
     * found.
     */
    public static String parseContentType(String contentTypeString) {
        int nameIndex = contentTypeString.indexOf("Content-Type: ");

        if (nameIndex != -1) {
            int endLineIndex = contentTypeString.indexOf("\n");
            if (endLineIndex != -1) {
                return contentTypeString.substring(nameIndex + 14, endLineIndex);
            }
        }
        return null;
    }

    /**
     * Retrieves the "name" attribute from a content disposition line
     *
     * @param dispositionString The entire "Content-disposition" string
     * @return <code>null</code> if no name could be found, otherwise,
     * returns the name
     * @see #parseForAttribute(String, String)
     */
    public static String parseDispositionName(String dispositionString) {
        return parseForAttribute("name", dispositionString);
    }

    /**
     * Retrieves the "filename" attribute from a content disposition line
     *
     * @param dispositionString The entire "Content-disposition" string
     * @return <code>null</code> if no filename could be found, otherwise,
     * returns the filename
     * @see #parseForAttribute(String, String)
     */
    public static String parseDispositionFilename(String dispositionString) {
        return parseForAttribute("filename", dispositionString);
    }

    /**
     * Parses a string looking for a attribute-value pair, and returns the value.
     * For example:
     * <pre>
     *      String parseString = "Content-Disposition: filename=\"bob\" name=\"jack\"";
     *      MultipartFormData.parseForAttribute(parseString, "name");
     * </pre>
     * That will return "bob".
     *
     * @param attribute   The name of the attribute you're trying to get
     * @param parseString The string to retrieve the value from
     * @return The value of the attribute, or <code>null</code> if none could be found
     */
    public static String parseForAttribute(String attribute, String parseString) {
        int nameIndex = parseString.indexOf(attribute + "=\"");
        if (nameIndex != -1) {
            int endQuoteIndex = parseString.indexOf("\"", nameIndex + attribute.length() + 3);

            if (endQuoteIndex != -1) {
                return parseString.substring(nameIndex + attribute.length() + 2, endQuoteIndex);
            }
        }
        return null;
    }

    /**
     * Retrieves the next element in the iterator if one exists.
     *
     * @return a {@link org.tinystruct.transfer.http.upload.ContentDisposition ContentDisposition}
     * representing the next element in the request data
     */
    public ContentDisposition getNextPart() {
        //retrieve the "Content-Disposition" header
        //and parse
        String disposition = readLine();
        if ((disposition != null) && (disposition.startsWith("Content-Disposition"))) {
            //convert line into byte form for Content-Disposition filename and name
            disposition = new String(disposition.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

            String name = parseDispositionName(disposition);
            String filename = parseDispositionFilename(disposition);
            String contentType = null;

            byte[] data;

            if (filename != null) {
                filename = new File(filename).getName();

                //check for windows filenames,
                //from linux jdk's the entire filepath
                //isn't parsed correctly from File.getName()
                int colonIndex = filename.indexOf(":");
                int slashIndex = filename.lastIndexOf("\\");

                if ((colonIndex > -1) && (slashIndex > -1)) {
                    //then consider this filename to be a full
                    //windows filepath, and parse it accordingly
                    //to retrieve just the file name
                    filename = filename.substring(slashIndex + 1);
                }

                //get the content type
                contentType = readLine();
                contentType = parseContentType(contentType);
            }

            //read data into String form, then convert to bytes
            //for both normal text and file
            StringBuilder textData = new StringBuilder();
            String line;

            //ignore next line (whitespace)
            readLine();

            //parse for text data
            line = readLine();

            while ((line != null) && (!line.startsWith(boundary))) {
                textData.append(line);
                line = readLine();
            }

            String text = textData.toString();
            //remove the "\r\n" if it's there
            if (text.endsWith("\r\n")) {
                textData.setLength(textData.length() - 2);
            }

            //remove the "\n" if it's there
            if (text.endsWith("\n")) {
                textData.setLength(textData.length() - 1);
            }

            text = textData.toString();
            //convert data into byte form for ContentDisposition
            data = text.getBytes(StandardCharsets.ISO_8859_1);

            return new ContentDisposition(name,
                    filename,
                    contentType,
                    data);
        }
        return null;
    }

    /**
     * Get the maximum amount of bytes read from a line at one time
     *
     * @return buffer size
     * @see jakarta.servlet.ServletInputStream#readLine(byte[], int, int)
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Set the maximum amount of bytes read from a line at one time
     *
     * @param bufferSize buffer size
     * @see jakarta.servlet.ServletInputStream#readLine(byte[], int, int)
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Handles retrieving the boundary and setting the input stream
     *
     * @throws ServletException servlet exception
     */
    protected void parseRequest() throws ServletException {
        // validate the content type header
        if (request.headers().get(Header.CONTENT_TYPE) == null)
            throw new ServletException("MultipartFormData: invalid multipart request data");

        // set boundary
        boundary = parseBoundary(request.headers().get(Header.CONTENT_TYPE).toString());

        // set the input stream
        inputStream = request.stream();

        if ((boundary == null) || (boundary.length() < 1)) {
            // try retrieving the header through more "normal" means
            boundary = parseBoundary(request.headers().get(Header.CONTENT_TYPE).toString());
        }

        if ((boundary == null) || (boundary.length() < 1)) {
            throw new ServletException("MultipartFormData: cannot retrieve boundary for multipart request");
        }

        // read first line
        if (!readLine().startsWith(boundary)) {
            throw new ServletException("MultipartFormData: invalid multipart request data");
        }
    }

    /**
     * Reads the input stream until it reaches a new line
     *
     * @return one line
     */
    protected String readLine() {

        byte[] bufferByte = new byte[bufferSize];
        int bytesRead;
        try {
            bytesRead = inputStream.readLine(bufferByte, 0, bufferSize);
        } catch (IOException ioe) {
            return null;
        }

        if (bytesRead == -1) {
            end = true;
            return null;
        }

        return new String(bufferByte, 0, bytesRead, StandardCharsets.ISO_8859_1);
    }
}