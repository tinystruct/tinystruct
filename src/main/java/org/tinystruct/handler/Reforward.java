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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.tinystruct.http.Constants.JSESSIONID;

public class Reforward {
    private final Response response;
    private String fromURL = "";
    private String currentURL = "";

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

        // Add the current cookies to response header to avoid missing them
        Cookie[] cookies = request.cookies();
        ResponseHeaders responseHeaders = new ResponseHeaders(response);
        for (Cookie cookie : cookies) {
            responseHeaders.add(Header.SET_COOKIE.set(cookie));
        }

        String host = request.headers().get(Header.HOST).toString();
        Cookie cookie = new CookieImpl(JSESSIONID);
        if (host.contains(":"))
            cookie.setDomain(host.substring(0, host.indexOf(":")));
        cookie.setValue(request.getSession().getId());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(-1);

        responseHeaders.add(Header.SET_COOKIE.set(cookie));
    }

    public void setDefault(String url) throws ApplicationException {
        if (url.indexOf("%3A") != -1) {
            this.fromURL = URLDecoder.decode(url, StandardCharsets.UTF_8);
        } else this.fromURL = url;
    }

    public void match(String action, String fromURL) {
        if ("".equals(action)) {
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