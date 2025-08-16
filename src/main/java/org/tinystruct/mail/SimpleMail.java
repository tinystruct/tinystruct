/*******************************************************************************
 * Copyright  (c) 2023 James Mover Zhou
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

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.tinystruct.ApplicationException;
import org.tinystruct.mail.Connection.PROTOCOL;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced email service implementation that supports both simple emails and emails with attachments.
 * This class implements the MailService interface and provides a fluent builder pattern for configuration.
 * Uses connection pooling through ConnectionManager for better resource management.
 */
public class SimpleMail implements MailService {
    private final static Logger logger = Logger.getLogger(SimpleMail.class.getName());
    private static final Configuration<String> config = new Settings();
    private static final ConnectionManager manager = ConnectionManager.getInstance();

    private final Properties properties;
    private final String username;
    private final String password;
    private String fromName;
    private List<InternetAddress> to;
    private List<InternetAddress> copyTo;
    private List<InternetAddress> behindCopyTo;
    private String content;
    private String subject;
    private List<File> attachments;

    /**
     * Creates a new SimpleMail instance with SMTP configuration.
     *
     * @param host SMTP host
     * @param port SMTP port
     * @param username SMTP username
     * @param password SMTP password
     */
    public SimpleMail(String host, int port, String username, String password) {
        this.properties = new Properties();
        this.properties.put("mail.smtp.host", host);
        this.properties.put("mail.smtp.port", port);
        this.properties.put("mail.smtp.auth", "true");
        this.properties.put("mail.smtp.starttls.enable", "true");
        
        this.username = username;
        this.password = password;
        this.to = new ArrayList<>();
        this.copyTo = new ArrayList<>();
        this.behindCopyTo = new ArrayList<>();
        this.attachments = new ArrayList<>();
    }

    /**
     * Creates a new SimpleMail instance with default configuration from settings.
     */
    public SimpleMail() {
        this(
            config.get("mail.smtp.host"),
            Integer.parseInt(config.get("mail.smtp.port")),
            config.get("mail.smtp.username"),
            config.get("mail.smtp.password")
        );
    }

    @Override
    public void sendMail(String to, String subject, String content) throws ApplicationException {
        try {
            clearRecipients();
            setTo(to);
            setSubject(subject);
            setBody(content);
            send();
        } catch (Exception e) {
            throw new ApplicationException("Failed to send email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendMailWithAttachments(String to, String subject, String content, String[] attachmentPaths) 
            throws ApplicationException {
        try {
            clearRecipients();
            setTo(to);
            setSubject(subject);
            setBody(content);
            
            // Add attachments
            for (String path : attachmentPaths) {
                attachFile(path);
            }
            
            send();
        } catch (Exception e) {
            throw new ApplicationException("Failed to send email with attachments: " + e.getMessage(), e);
        }
    }

    /**
     * Sets the email subject.
     */
    public SimpleMail setSubject(String mailSubject) {
        this.subject = mailSubject;
        return this;
    }

    /**
     * Sets the email body content.
     */
    public SimpleMail setBody(String mailBody) {
        this.content = mailBody;
        return this;
    }

    /**
     * Adds a file attachment to the email.
     */
    public boolean attachFile(String fileName) {
        try {
            File file = new File(fileName);
            if (file.exists() && file.canRead()) {
                attachments.add(file);
                return true;
            }
            logger.warning("Cannot attach file: " + fileName + " (file not found or not readable)");
            return false;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to attach file: " + fileName, e);
            return false;
        }
    }

    /**
     * Sets the sender's display name.
     */
    public SimpleMail setFrom(String fromName) {
        this.fromName = fromName;
        return this;
    }

    /**
     * Sets the primary recipient.
     */
    public SimpleMail setTo(String to) throws ApplicationException {
        try {
            clearRecipients();
            addTo(to);
            return this;
        } catch (Exception e) {
            throw new ApplicationException("Invalid recipient address: " + to, e);
        }
    }

    /**
     * Adds a recipient to the TO list.
     */
    public SimpleMail addTo(String to) throws ApplicationException {
        try {
            this.to.add(new InternetAddress(to));
            return this;
        } catch (Exception e) {
            throw new ApplicationException("Invalid recipient address: " + to, e);
        }
    }

    /**
     * Adds a CC recipient.
     */
    public SimpleMail addCopyTo(String copyTo) throws ApplicationException {
        try {
            this.copyTo.add(new InternetAddress(copyTo));
            return this;
        } catch (Exception e) {
            throw new ApplicationException("Invalid CC address: " + copyTo, e);
        }
    }

    /**
     * Adds a BCC recipient.
     */
    public SimpleMail addBehindCopyTo(String bccTo) throws ApplicationException {
        try {
            this.behindCopyTo.add(new InternetAddress(bccTo));
            return this;
        } catch (Exception e) {
            throw new ApplicationException("Invalid BCC address: " + bccTo, e);
        }
    }

    /**
     * Sends the email with current configuration using a connection from the pool.
     */
    public boolean send() throws ApplicationException {
        if (to.isEmpty()) {
            throw new ApplicationException("No recipients specified");
        }

        Connection connection = null;
        try {
            // Get a connection from the pool
            connection = manager.getConnection(config, PROTOCOL.SMTP);

            // Create the message
            Session session = connection.getSession();
            MimeMessage message = new MimeMessage(session);
            
            // Set From
            if (fromName != null) {
                message.setFrom(new InternetAddress(username, fromName));
            } else {
                message.setFrom(new InternetAddress(username));
            }

            // Set recipients
            InternetAddress[] toAddresses = to.toArray(new InternetAddress[0]);
            message.addRecipients(Message.RecipientType.TO, toAddresses);
            if (!copyTo.isEmpty()) {
                message.addRecipients(Message.RecipientType.CC, copyTo.toArray(new InternetAddress[0]));
            }
            if (!behindCopyTo.isEmpty()) {
                message.addRecipients(Message.RecipientType.BCC, behindCopyTo.toArray(new InternetAddress[0]));
            }

            message.setSubject(subject);

            // Handle content and attachments
            if (attachments.isEmpty()) {
                message.setContent(content, "text/html; charset=utf-8");
            } else {
                // Create multipart message
                MimeMultipart multipart = new MimeMultipart();

                // Add content part
                MimeBodyPart contentPart = new MimeBodyPart();
                contentPart.setContent(content, "text/html; charset=utf-8");
                multipart.addBodyPart(contentPart);

                // Add attachment parts
                for (File file : attachments) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(file);
                    multipart.addBodyPart(attachmentPart);
                }

                message.setContent(multipart);
            }

            // Send message using the pooled connection
            connection.send(message, message.getAllRecipients());
            logger.log(Level.INFO, "Email sent successfully to {0}", to);
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send email", e);
            throw new ApplicationException("Failed to send email: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                manager.releaseConnection(connection);
            }
        }
    }

    private void clearRecipients() {
        to.clear();
        copyTo.clear();
        behindCopyTo.clear();
        attachments.clear();
    }
}

