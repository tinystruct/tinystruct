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
package org.tinystruct.transfer.http.upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class ContentDisposition {

    public static final String LINE = "\r\n";

    private final String name;
    private final String fileName;
    private final String contentType;
    private final byte[] data;

    public ContentDisposition(String name, String fileName, String contentType,
                              byte[] data) {
        this.name = name;
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getTransferBytes() throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("Content-Disposition: form-data; name=\"").append(name).append("\"");
        if (this.fileName != null) {
            builder.append("; filename=\"").append(this.fileName).append("\"");
        }
        builder.append(LINE);

        if (this.fileName != null) {
            builder.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(fileName)).append(";").append(LINE);
            builder.append("Content-Transfer-Encoding: binary").append(LINE);
        } else {
            builder.append("Content-Type: text/plain; charset=utf-8").append(LINE);
        }
        builder.append(LINE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.write(this.data);
        outputStream.write(LINE.getBytes(StandardCharsets.UTF_8));

        return outputStream.toByteArray();
    }

}
