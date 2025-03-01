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
package org.tinystruct.mail;

import org.tinystruct.ApplicationException;
import org.tinystruct.system.Configuration;

import jakarta.mail.*;
import java.util.Properties;


public class SMTPConnection implements Connection {
    /**
     * Email Connection .
     */
    private final Configuration<String> config;

    private Session session;
    private Transport transport;
    private String username;
    private String password;

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

        boolean isSSL = Boolean.parseBoolean(this.config.get("mail.ssl.on"));
        String socketFactoryPort = this.config.get("mail.smtp.socketFactory.port");

        this.username = this.config.get("smtp.auth.user");
        this.password = this.config.get("smtp.auth.pwd");

        if (isSSL) {
//            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
            props.setProperty("mail.smtp.socketFactory.port",  socketFactoryPort);
        }

        String host = this.config.get("mail.smtp.host"),
                port = this.config.get("mail.smtp.port"),
                auth = this.config.get("mail.smtp.auth"),
                from = this.config.get("mail.smtp.from");
        props.setProperty("mail.smtp.from", from.isEmpty() ? this.username : from);
        props.setProperty("mail.smtp.host", host.isEmpty() ? "localhost" : host); // eg. smtp.gmail.com
        props.setProperty("mail.smtp.port", port.isEmpty() ? "25" : port); // eg. // 443
        props.setProperty("mail.smtp.auth", auth.isEmpty() ? "false" : auth); // "true" or "false"

        if ("true".equalsIgnoreCase(auth)) {
            jakarta.mail.Authenticator authenticator = new jakarta.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };

            this.session = Session.getInstance(props, authenticator);
        } else {
            this.session = Session.getInstance(props);
        }

        try {
            this.transport = this.session.getTransport("smtp");
            this.transport.connect();
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
    @Override
	public Session getSession() {
        return this.session;
    }

    /* (non-Javadoc)
     * @see org.mover.system.mail.Connection#Available()
     */
    @Override
	public boolean available() {
        return this.transport.isConnected();
    }


    @Override
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
    @Override
	public String getId() {
        return id;
    }

    @Override
	public PROTOCOL getProtocol() {
        return PROTOCOL.SMTP;
    }
}
