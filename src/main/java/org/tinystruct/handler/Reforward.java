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
package org.tinystruct.handler;


import org.tinystruct.ApplicationException;
import org.tinystruct.http.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Reforward {
    private String fromURL = "";
    private String currentURL = "";
    private final Response response;

    public Reforward(Request request, Response response) throws ApplicationException {

        this.response = response;

        if (request.query() != null)
            this.currentURL = request.uri() + '?' + request.query();
        else {
            this.currentURL = request.uri();
        }

        Headers headers = request.headers();
        if (request.getParameter("from") != null && request.getParameter("from").trim().length() > 0) {
            this.setDefault(request.getParameter("from"));
        } else if (headers.get(Header.REFERER) != null && headers.get(Header.REFERER).toString().startsWith("http://" + request.headers().get(Header.SERVER))) {
            this.fromURL = headers.get(Header.REFERER).toString();
        } else {
            this.fromURL = "/";
        }
    }

    public void setDefault(String url) throws ApplicationException {
        if (url.indexOf("%3A") != -1) {
            this.fromURL = URLDecoder.decode(url, StandardCharsets.UTF_8);
        }
        else this.fromURL = url;
    }

    public void match(String action, String fromURL) {
        String action1 = "";
        if (action1.equals(action)) {
            this.fromURL = fromURL;
        }
    }

    public Response forward() throws ApplicationException {
        try {
            response.sendRedirect(this.fromURL);
        } catch (IOException io) {
            throw new ApplicationException(io.getMessage(), io);
        }

        return response;
    }

    public String getCurrentURL() throws ApplicationException {
        return URLEncoder.encode(this.currentURL, StandardCharsets.UTF_8);
    }

    public String getFromURL() throws ApplicationException {
        return URLEncoder.encode(this.fromURL, StandardCharsets.UTF_8);
    }

}