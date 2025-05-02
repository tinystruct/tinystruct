package org.tinystruct.data;

import java.io.File;
import java.io.IOException;

public interface FileEntity {
    /**
     * Returns the original filename in the client's filesystem,
     * as provided by the browser (or other client software).
     *
     * @return the original filename
     */
    String getFilename();

    /**
     * Set the original filename
     * @param filename File name
     */
    void setFilename(String filename);

    /**
     * Returns the content type passed by the browser or null if not defined.
     *
     * @return the content type passed by the browser or null if not defined.
     */
    String getContentType();

    /**
     * Set the Content Type passed by the browser if defined
     *
     * @param contentType Content Type to set - must be not null
     */
    void setContentType(String contentType);

    /**
     * Returns the Content-Transfer-Encoding
     *
     * @return the Content-Transfer-Encoding
     */
    String getContentTransferEncoding();

    /**
     * Set the Content-Transfer-Encoding type from String as 7bit, 8bit or binary
     * @param contentTransferEncoding Content transfer encoding
     */
    void setContentTransferEncoding(String contentTransferEncoding);

    /**
     * Returns the size in byte of the InterfaceHttpData
     *
     * @return the size of the InterfaceHttpData
     */
    long length();

    /**
     * Returns the contents of the file item as an array of bytes.<br>
     * Note: this method will allocate a lot of memory, if the data is currently stored on the file system.
     *
     * @return the contents of the file item as an array of bytes.
     * @throws IOException
     */
    byte[] get() throws IOException;

    /**
     * Set the contents of the attachment.
     *
     * @param content the contents of the file item as an array of bytes.
     */
    void setContent(byte[] content);

    /**
     * @return the associated File if this data is represented in a file
     * @throws IOException if this data is not represented by a file
     */
    File getFile() throws IOException;

    /**
     * Returns the field name for this file.
     *
     * @return the field name for this file
     */
    String getName();

    /**
     * Sets the field name for this file.
     *
     * @param name the field name for this file
     */
    void setName(String name);
}
