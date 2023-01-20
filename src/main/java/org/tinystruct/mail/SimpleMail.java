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

import org.tinystruct.ApplicationException;
import org.tinystruct.mail.Connection.PROTOCOL;
import org.tinystruct.system.Configuration;
import org.tinystruct.system.Settings;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mover
 */
public class SimpleMail {
    private final static Logger logger = Logger.getLogger(SimpleMail.class.getName());

    private final String username; // smtp认证用户名和密码
    private String fromName;
    private List<InternetAddress> to;
    private List<InternetAddress> copyTo;
    private List<InternetAddress> behindCopyTo;
    private String content;
    private String subject;

    private InternetAddress toAddress;

    private MimeBodyPart mBodyPart;

    private static final Configuration<String> config = new Settings();
    private static final ConnectionManager manager = ConnectionManager.getInstance();

    public SimpleMail() {
        this.username = config.get("smtp.auth.user");
    }

    public void setSubject(String mailSubject) {
        this.subject = mailSubject;
    }

    public void setBody(String mailBody) {
        this.content = mailBody;
    }

    /**
     * Attach a file with the file name.
     *
     * @param fileName file name
     * @return true or false
     */
    public boolean attachFile(String fileName) {
        logger.log(Level.INFO, "增加邮件附件：{}", fileName);
        mBodyPart = new MimeBodyPart();
        FileDataSource fileds = new FileDataSource(fileName);

        try {
            mBodyPart.setDataHandler(new DataHandler(fileds));
            mBodyPart.setFileName(MimeUtility.encodeWord(fileds.getName(), "utf-8", "Q"));
        } catch (MessagingException e) {
            logger.severe("增加邮件附件：" + fileName + "发生错误！" + e);
            return false;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Set the from name.
     *
     * @param fromName from name
     */
    public void setFrom(String fromName) {
        this.fromName = fromName;
    }

    /**
     * Set the address to be sent to.
     *
     * @param to to address
     * @throws ApplicationException application exception
     */
    public void setTo(String to) throws ApplicationException {
        try {
            this.toAddress = new InternetAddress(to);
        } catch (AddressException e) {

            throw new ApplicationException(e.getMessage(), e);
        }
    }

    public void addTo(String to) throws ApplicationException {
        if (this.to == null) {
            this.to = new ArrayList<InternetAddress>();
        }

        try {
            this.to.add(new InternetAddress(to));
        } catch (AddressException e) {

            throw new ApplicationException(e.getMessage(), e);
        }

    }

    public void addCopyTo(String copyto) throws AddressException {
        if (this.copyTo == null) {
            this.copyTo = new ArrayList<InternetAddress>();
        }

        this.copyTo.add(new InternetAddress(copyto));
    }

    public void addBehindCopyTo(String copyto) throws ApplicationException {
        if (this.behindCopyTo == null) {
            this.behindCopyTo = new ArrayList<InternetAddress>();
        }

        try {
            this.behindCopyTo.add(new InternetAddress(copyto));
        } catch (AddressException e) {

            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Send mail.
     *
     * @return true or false
     * @throws ApplicationException application exception
     */
    public boolean send() throws ApplicationException {

        SMTPConnection connection = (SMTPConnection) manager.getConnection(config, PROTOCOL.SMTP);

        Session session = connection.getSession();
        Message message = new MimeMessage(session);

        logger.log(Level.INFO, "正在发送邮件....");
        try {
            Multipart multipart = new MimeMultipart();

            BodyPart body = new MimeBodyPart();
            body.setHeader("Content-Transfer-Encoding", "base64");
            body.setContent(this.content, "text/html;charset=utf-8");
            multipart.addBodyPart(body);

            if (this.mBodyPart != null) {
                multipart.addBodyPart(mBodyPart);
            }

            if (this.fromName != null && this.fromName.trim().length() > 0)
                message.setFrom(new InternetAddress(this.username, MimeUtility.encodeWord(this.fromName, "utf-8", "Q")));
            else
                message.setFrom(new InternetAddress(this.username));

            message.setSubject(MimeUtility.encodeWord(this.subject, "utf-8", "Q"));
            message.setSentDate(new Date());
            message.setContent(multipart);

            if (this.toAddress == null) {
                if (this.to != null && this.to.size() > 0) {
                    InternetAddress[] to = new InternetAddress[this.to.size()];
                    message.addRecipients(Message.RecipientType.TO, this.to.toArray(to));
                }
            } else {
                message.setRecipient(Message.RecipientType.TO, toAddress);
            }

            if (this.copyTo != null && this.copyTo.size() > 0) {
                InternetAddress[] cp = new InternetAddress[this.copyTo.size()];
                message.addRecipients(Message.RecipientType.CC, this.copyTo.toArray(cp));
            }

            if (this.behindCopyTo != null && this.behindCopyTo.size() > 0) {
                InternetAddress[] bcp = new InternetAddress[this.behindCopyTo.size()];
                message.addRecipients(Message.RecipientType.BCC, this.behindCopyTo.toArray(bcp));
            }
            message.saveChanges();

            connection.send(message, message.getAllRecipients());
            logger.log(Level.INFO, "发送邮件成功！");
            return true;
        } catch (MessagingException e) {
            throw new ApplicationException(e.getLocalizedMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new ApplicationException(e.getLocalizedMessage(), e);
        } finally {
            manager.flush(connection);
        }

    }

    public Message[] receive() throws ApplicationException {
        POP3Connection connection = (POP3Connection) manager.getConnection(config, PROTOCOL.POP3);

        Store store = connection.getStore();
        Message[] messages = new Message[]{};
        try {
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            messages = folder.getMessages();
            logger.info("Messages's length: " + messages.length);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return messages;
    }

}

