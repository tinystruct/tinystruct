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
import java.security.Security;
import java.util.Properties;


public class SMTPConnection implements Connection {
    /**
     * Email Connection .
     */
    private Configuration<String> config;

    private Session session;
    private Transport transport;

    private String SMTP_SOCKETFACTORY_PORT;

    private String username;
    private String password;

    private boolean isSSL = false;
    private String id;

    public SMTPConnection(Configuration<String> properties) throws ApplicationException {
        this.config = properties;

        this.init();
    }

    private void init() throws ApplicationException {
        this.id = java.util.UUID.randomUUID().toString().replace("-", "");

        /**
         * Load Configuration
         */
        Properties props = System.getProperties();

        this.isSSL = Boolean.valueOf(this.config.get("mail.ssl.on"));
        this.SMTP_SOCKETFACTORY_PORT = this.config.get("mail.smtp.socketFactory.port");

        this.username = this.config.get("smtp.auth.user");
        this.password = this.config.get("smtp.auth.pwd");

        if (this.isSSL) {
//            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

            props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
            props.setProperty("mail.smtp.socketFactory.port", this.SMTP_SOCKETFACTORY_PORT);
        }

        String host = this.config.get("mail.smtp.host").trim(),
                port = this.config.get("mail.smtp.port").trim(),
                autho = this.config.get("mail.smtp.auth").trim(),
                from = this.config.get("mail.smtp.from").trim();
        props.setProperty("mail.smtp.from", from.length() == 0 ? this.username : from);
        props.setProperty("mail.smtp.host", host.length() == 0 ? "localhost" : host); // eg. smtp.gmail.com
        props.setProperty("mail.smtp.port", port.length() == 0 ? "25" : port); // eg. // 443
        props.setProperty("mail.smtp.auth", autho.length() == 0 ? "false" : autho); // "true" or "false"

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
            this.transport = this.session.getTransport("smtp");
            this.transport.connect();
        } catch (NoSuchProviderException e) {
            throw new ApplicationException(e.getMessage(), e);
        } catch (MessagingException e) {
            throw new ApplicationException(e.getMessage(), e);
        }

    }

    public Transport getTransport() throws ApplicationException {
        if (!this.transport.isConnected()) {
            try {
                this.transport.connect();
            } catch (MessagingException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }

        return this.transport;
    }

    /* (non-Javadoc)
     * @see org.mover.system.mail.Connection#getSession()
     */
    public Session getSession() {
        return this.session;
    }

    /* (non-Javadoc)
     * @see org.mover.system.mail.Connection#Available()
     */
    public boolean available() {
        return this.transport.isConnected();
    }

    /* (non-Javadoc)
     * @see org.mover.system.mail.Connection#close()
     */
    public void close() throws MessagingException {
        if (this.available())
            this.transport.close();
    }

    public void send(Message message, Address[] allRecipients)
            throws MessagingException {
        this.transport.sendMessage(message, allRecipients);
    }

    public void send(Message message)
            throws MessagingException {
        Transport.send(message);
    }

    /* (non-Javadoc)
     * @see org.mover.system.mail.Connection#getId()
     */
    public String getId() {
        return id;
    }

    public PROTOCOL getProtocol() {
        return PROTOCOL.SMTP;
    }
}
