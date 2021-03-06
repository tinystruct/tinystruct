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
package org.tinystruct.handler;


import org.tinystruct.ApplicationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class Reforward {
    private String action = "", fromURL = "", currentURL = "";
    private HttpServletRequest request;
    private HttpServletResponse response;

    public Reforward(HttpServletRequest request, HttpServletResponse response) throws ApplicationException {

        this.request = request;
        this.response = response;

        if (request.getQueryString() != null)
            this.currentURL = request.getRequestURL().append('?').append(this.request.getQueryString()).toString();
        else {
            this.currentURL = request.getRequestURL().toString();
        }

        if (request.getParameter("from") != null && request.getParameter("from").trim().length() > 0) {
            this.setDefault(request.getParameter("from"));
        } else if (request.getHeader("referer") != null && request.getHeader("referer").startsWith("http://" + request.getServerName())) {
            this.fromURL = request.getHeader("referer");
        } else {
            this.fromURL = "/";
        }
    }

    public void setDefault(String url) throws ApplicationException {
        if (url.indexOf("%3A") != -1)
            try {
                this.fromURL = URLDecoder.decode(url, "utf8");
            } catch (UnsupportedEncodingException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        else this.fromURL = url;
    }

    public void match(String action, String fromURL) {
        if (this.action.equals(action)) {
            this.fromURL = fromURL;
        }
    }

    public void forward() throws ApplicationException {
        try {
            response.sendRedirect(this.fromURL);
        } catch (IOException io) {
            throw new ApplicationException(io.getMessage(), io);
        }
    }

    public String getCurrentURL() throws ApplicationException {
        try {
            return URLEncoder.encode(this.currentURL, "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    public String getFromURL() throws ApplicationException {
        try {
            return URLEncoder.encode(this.fromURL, "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

}