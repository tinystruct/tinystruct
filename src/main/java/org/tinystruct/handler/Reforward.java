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
    private final String currentURL;

    /**
     * Constructor for Reforward class.
     *
     * @param request  The HTTP request object.
     * @param response The HTTP response object.
     * @throws ApplicationException if an error occurs.
     */
    public Reforward(Request request, Response response) throws ApplicationException {
        this.response = response;

        // Construct the current URL
        if (request.query() != null) {
            this.currentURL = request.uri() + '?' + request.query();
        } else {
            this.currentURL = request.uri();
        }

        // Determine the 'from' URL based on the request
        Headers headers = request.headers();
        if (request.getParameter("from") != null && request.getParameter("from").trim().length() > 0) {
            this.setDefault(request.getParameter("from"));
        } else if (headers.get(Header.REFERER) != null && headers.get(Header.REFERER).toString().startsWith(request.isSecure() ? "https" : "http" + "://" + request.headers().get(Header.SERVER))) {
            this.fromURL = request.isSecure() ? headers.get(Header.REFERER).toString().replaceAll("http://", "https://") : headers.get(Header.REFERER).toString();
        } else {
            this.fromURL = "/";
        }

        // Add the current cookies to response header to avoid missing them
        Cookie[] cookies = request.cookies();
        ResponseHeaders responseHeaders = new ResponseHeaders(response);
        for (Cookie cookie : cookies) {
            responseHeaders.add(Header.SET_COOKIE.set(cookie));
        }

        // Set JSESSIONID cookie in response
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

    /**
     * Set the default 'from' URL.
     *
     * @param url The default 'from' URL.
     * @throws ApplicationException if an error occurs.
     */
    public void setDefault(String url) throws ApplicationException {
        if (url.contains("%3A")) {
            this.fromURL = URLDecoder.decode(url, StandardCharsets.UTF_8);
        } else this.fromURL = url;
    }

    /**
     * Match an action and set the 'from' URL accordingly.
     *
     * @param action  The action to match.
     * @param fromURL The 'from' URL to set.
     */
    public void match(String action, String fromURL) {
        if ("".equals(action)) {
            this.fromURL = fromURL;
        }
    }

    /**
     * Forward the response to the 'from' URL.
     *
     * @return The HTTP response object.
     * @throws ApplicationException if an error occurs.
     */
    public Response forward() throws ApplicationException {
        try {
            response.sendRedirect(this.fromURL);
        } catch (IOException io) {
            throw new ApplicationException(io.getMessage(), io);
        }

        return response;
    }

    /**
     * Get the current URL encoded.
     *
     * @return The current URL encoded.
     */
    public String getCurrentURL() {
        return URLEncoder.encode(this.currentURL, StandardCharsets.UTF_8);
    }

    /**
     * Get the 'from' URL encoded.
     *
     * @return The 'from' URL encoded.
     */
    public String getFromURL() {
        return URLEncoder.encode(this.fromURL, StandardCharsets.UTF_8);
    }

}
