/*******************************************************************************
 * Copyright  (c) 2017 James Mover Zhou
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
package org.tinystruct.transfer.http;

import org.tinystruct.ApplicationException;
import org.tinystruct.transfer.Reception;
import org.tinystruct.transfer.Transmission;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.net.Proxy.Type;
import java.util.Properties;

public class Transmitter implements Transmission {

    private final Properties properties = new Properties();
    private Proxy proxy = null;
    private URLConnection connection;

    public void set(String name, Object value) {
        this.properties.put(name, value);
    }

    public Object get(String name) {
        return this.properties.get(name);
    }

    public void proxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public URLConnection getConnection() {
        return this.connection;
    }

    public void transmit(Reception reception) throws ApplicationException {
        start();
        byte[] p = this.get("parameters").toString().getBytes();

        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", Integer.toString(p.length));
        connection.setRequestProperty("Content-Language", "en-US");

        connection.setUseCaches(false);

        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            // Send request
            wr.write(p);
            wr.flush();
            wr.close();

            reception.accept(this);
        } catch (MalformedURLException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }
    }

    public void start() throws ApplicationException {
        try {
            URL url = (URL) this.get("url");

            if (this.proxy != null)
                this.connection = url.openConnection(this.proxy);
            else
                this.connection = url.openConnection();

            this.connection.setDoOutput(true);
            this.connection.setDoInput(true);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }
    }

    public void stop() throws ApplicationException {
        try {
            connection.getOutputStream().close();
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }
    }

    public static void main(String[] args) throws ApplicationException, MalformedURLException {
        Transmission t = new Transmitter();
        t.set("url", new URL("http://ingod.asia/?"));
        t.set("parameters", "q=诗篇");
        t.proxy(new Proxy(Type.HTTP, new InetSocketAddress("135.245.115.123", 8000)));

        Reception r = new Receptor();
        t.transmit(r);
    }

}
