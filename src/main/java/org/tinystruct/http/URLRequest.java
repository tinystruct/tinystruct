package org.tinystruct.http;

/*******************************************************************************
 * Copyright  (c) 2013, 2017 James Mover Zhou
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

import org.tinystruct.ApplicationException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class URLRequest {

    private URL url;
    private Map<String, String> headers;

    private String method = "GET";

    public URLRequest(URL url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    /**
     * Set the method for the URL request, one of:
     * <UL>
     * <LI>GET
     * <LI>POST
     * <LI>HEAD
     * <LI>OPTIONS
     * <LI>PUT
     * <LI>DELETE
     * <LI>TRACE
     * </UL> are legal, subject to protocol restrictions.  The default
     * method is GET.
     *
     * @param method the HTTP method
     * @see #getMethod()
     */
    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public URLRequest setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public URL getUrl() {
        return url;
    }

    public URLRequest setUrl(URL url) {
        this.url = url;
        return this;
    }

    public byte[] send(Map<String, Object> parameters) throws ApplicationException, URISyntaxException {
        try {
            return send(parameters, new Callback<ByteArrayOutputStream>() {
                public byte[] process(ByteArrayOutputStream out) throws ApplicationException {
                    return out.toByteArray();
                }
            });
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }
    }

    public byte[] send(Map<String, Object> parameters, Callback<ByteArrayOutputStream> callback)
            throws ApplicationException, IOException {
        HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
        connection.setRequestProperty("accept", "*/*");
        connection.setRequestProperty("connection", "Keep-Alive");
        connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(15000);
        connection.setRequestMethod(method);

        if (headers != null) {
            Set<String> set = headers.keySet();
            Iterator<String> i = set.iterator();
            String key;

            while (i.hasNext()) {
                key = i.next();
                connection.setRequestProperty(key, headers.get(key));
            }
        }

        connection.setDoInput(true);
        connection.setDoOutput(true);

        if (parameters != null && parameters.size() > 0) {
            OutputStream os = connection.getOutputStream();
            String query = this.buildQuery(parameters);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();
        }

        connection.connect();

        InputStream in = connection.getInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = new byte[1024];
        int len;
        while ((len = in.read(bytes)) != -1) {
            out.write(bytes, 0, len);
        }
        in.close();
        out.close();
        connection.disconnect();
        return callback.process(out);
    }

    private String buildQuery(Map<String, Object> parameters) {
        Set<String> keySet = parameters.keySet();
        Iterator<String> iterator = keySet.iterator();
        StringBuilder buffer = new StringBuilder();
        String key;
        boolean first = true;
        while (iterator.hasNext()) {
            key = iterator.next();
            if (first) {
                first = false;
            } else
                buffer.append("&");

            buffer.append(key).append("=").append(parameters.get(key));
        }

        return buffer.toString();
    }

}
