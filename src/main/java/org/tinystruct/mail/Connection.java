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

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;

/**
 * Interface for mail connections that can be pooled and managed.
 */
public interface Connection {
    /**
     * Gets the unique identifier for this connection.
     *
     * @return Connection ID
     */
    String getId();

    /**
     * Gets the mail protocol used by this connection.
     *
     * @return Mail protocol
     */
    PROTOCOL getProtocol();

    /**
     * Gets the mail session associated with this connection.
     *
     * @return Mail session
     */
    Session getSession();

    /**
     * Checks if the connection is available for use.
     *
     * @return true if the connection is available
     */
    boolean available();

    /**
     * Sends a message to specified recipients.
     *
     * @param message The message to send
     * @param recipients The recipients
     * @throws MessagingException if sending fails
     */
    void send(Message message, Address[] recipients) throws MessagingException;

    /**
     * Closes the connection and releases resources.
     *
     * @throws MessagingException if closing fails
     */
    void close() throws MessagingException;

    /**
     * Mail protocols supported by connections.
     */
    enum PROTOCOL {
        SMTP,
        POP3,
        IMAP
    }
}