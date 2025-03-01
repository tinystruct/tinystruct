package org.tinystruct.mail;

import org.tinystruct.ApplicationException;

/**
 * Interface for email services.
 */
public interface MailService {
    
    /**
     * Sends an email to the specified recipient.
     *
     * @param to The recipient's email address
     * @param subject The email subject
     * @param content The email content (HTML format)
     * @throws ApplicationException if sending fails
     */
    void sendMail(String to, String subject, String content) throws ApplicationException;
    
    /**
     * Sends an email with attachments.
     *
     * @param to The recipient's email address
     * @param subject The email subject
     * @param content The email content (HTML format)
     * @param attachments Array of file paths to attach
     * @throws ApplicationException if sending fails
     */
    void sendMailWithAttachments(String to, String subject, String content, String[] attachments) throws ApplicationException;
} 