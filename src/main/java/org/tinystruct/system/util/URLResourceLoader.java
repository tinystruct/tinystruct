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
package org.tinystruct.system.util;

import org.tinystruct.ApplicationException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class URLResourceLoader extends TextFileLoader {
    private final static Logger logger = Logger.getLogger(URLResourceLoader.class.getName());

    private HttpURLConnection connection;

    public URLResourceLoader(URL url) throws ApplicationException {
        connect(url);
    }

    private void connect(URL url) throws ApplicationException {
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:80.0) Gecko/20100101 Firefox/80.0");

            String charsetName = connection.getContentEncoding();
            if (charsetName != null) {
                this.setCharset(charsetName);
            } else {
                charsetName = connection.getContentType();
            }

            if (charsetName != null && charsetName.indexOf('=') != -1) {
                this.setCharset(charsetName.split("=")[1]);
            }

            this.setInputStream(connection.getInputStream());
        } catch (IOException e) {
            throw new ApplicationException(e.toString(), e);
        }
    }

    public URLResourceLoader(URL url, boolean autoconnect) {
        while (autoconnect)
            try {
                this.connect(url);
                autoconnect = false;
            } catch (ApplicationException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
    }

    @Override
	public StringBuilder getContent() throws ApplicationException {
        StringBuilder buffer;
        try {
            buffer = super.getContent();
        } finally {
            this.disconnection();
        }

        return buffer;
    }

    public void disconnection() {
        this.connection.disconnect();
    }

}
