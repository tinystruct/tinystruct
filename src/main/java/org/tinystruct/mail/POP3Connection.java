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
package org.tinystruct.mail;

import org.tinystruct.ApplicationException;
import org.tinystruct.system.Configuration;

import javax.mail.*;
import java.util.Properties;


public class POP3Connection implements Connection {

    /**
     * Email Connection .
     */
    private final Configuration<String> config;

    private Session session;
    private Store store;

    private String username;
    private String password;

    private String id;

    public POP3Connection(Configuration<String> properties) throws ApplicationException {
        this.config = properties;

        this.init();
    }

    private void init() throws ApplicationException {
        this.id = java.util.UUID.randomUUID().toString().replace("-", "");

        /**
         * Load Configuration
         */
        Properties props = System.getProperties();

        boolean isSSL = Boolean.valueOf(this.config.get("mail.ssl.on"));

        String POP3_SOCKETFACTORY_PORT = this.config.get("mail.pop3.socketFactory.port");

        this.username = this.config.get("smtp.auth.user");
        this.password = this.config.get("smtp.auth.pwd");

        props.setProperty("mail.store.protocol", this.config.get("mail.store.protocol"));
        if (isSSL) {
//            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

            props.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.pop3.socketFactory.fallback", "false");
            props.setProperty("mail.pop3.socketFactory.port", POP3_SOCKETFACTORY_PORT);
        }

        String host = this.config.get("mail.pop3.host").trim(),
                port = this.config.get("mail.pop3.port").trim(),
                autho = this.config.get("mail.pop3.auth").trim();

        props.setProperty("mail.pop3.host", host.length() == 0 ? "localhost" : host); // eg. smtp.gmail.com
        props.setProperty("mail.pop3.port", port.length() == 0 ? "995" : port); // eg. // 443
        props.setProperty("mail.pop3.auth", autho.length() == 0 ? "false" : autho); // "true" or "false"

        if (autho.equalsIgnoreCase("true")) {
            javax.mail.Authenticator auth = new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };

            this.session = Session.getInstance(props, auth);
        } else {
            this.session = Session.getInstance(props);
        }

        try {
            this.store = this.session.getStore();
            this.store.connect();

        } catch (NoSuchProviderException e) {

            throw new ApplicationException(e.getMessage(), e);
        } catch (MessagingException e) {

            throw new ApplicationException(e.getMessage(), e);
        }

    }

    public Store getStore() throws ApplicationException {
        if (!this.store.isConnected()) {
            try {
                this.store.connect();
            } catch (MessagingException e) {

                throw new ApplicationException(e.getMessage(), e);
            }
        }

        return this.store;
    }

    public Session getSession() {
        return this.session;
    }

    public boolean available() {
        return this.store.isConnected();
    }

    public void close() throws MessagingException {
        if (this.available())
            this.store.close();

        if (this.store.isConnected())
            this.store.close();
    }

    public Folder getFolder(String folder)
            throws MessagingException {
        return this.store.getFolder(folder);
    }

    public String getId() {
        return id;
    }

    public PROTOCOL getProtocol() {
        return PROTOCOL.POP3;
    }
}
