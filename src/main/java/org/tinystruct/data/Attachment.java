package org.tinystruct.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Attachment implements FileEntity {

    private String fileName;
    private String contentType;
    private byte[] content;
    private String contentTransferEncoding;
    private String name; // Field name for this attachment

    /**
     * Returns the original filename in the client's filesystem,
     * as provided by the browser (or other client software).
     *
     * @return the original filename
     */
    @Override
    public String getFilename() {
        return this.fileName;
    }

    /**
     * Set the original filename
     *
     * @param filename the filename.
     */
    @Override
    public void setFilename(String filename) {
        this.fileName = filename;
    }

    /**
     * Returns the content type passed by the browser or null if not defined.
     *
     * @return the content type passed by the browser or null if not defined.
     */
    @Override
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Set the Content Type passed by the browser if defined
     *
     * @param contentType Content Type to set - must be not null
     */
    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Returns the Content-Transfer-Encoding
     *
     * @return the Content-Transfer-Encoding
     */
    @Override
    public String getContentTransferEncoding() {
        return this.contentTransferEncoding;
    }

    /**
     * Set the Content-Transfer-Encoding type from String as 7bit, 8bit or binary
     *
     * @param contentTransferEncoding Content-Transfer-Encoding
     */
    @Override
    public void setContentTransferEncoding(String contentTransferEncoding) {
        this.contentTransferEncoding = contentTransferEncoding;
    }

    /**
     * Returns the size in byte of the InterfaceHttpData
     *
     * @return the size of the InterfaceHttpData
     */
    @Override
    public long length() {
        return this.content.length;
    }

    /**
     * Returns the contents of the file item as an array of bytes.<br>
     * Note: this method will allocate a lot of memory, if the data is currently stored on the file system.
     *
     * @return the contents of the file item as an array of bytes.
     * @throws IOException
     */
    @Override
    public byte[] get() throws IOException {
        return content;
    }

    /**
     * Set the contents of the attachment.
     *
     * @param content the contents of the file item as an array of bytes.
     */
    @Override
    public void setContent(byte[] content) {
        this.content = content;
    }

    /**
     * @return the associated File if this data is represented in a file
     * @throws IOException if this data is not represented by a file
     */
    @Override
    public File getFile() throws IOException {
        if(this.content == null) throw new IllegalArgumentException("File content is empty.");

        Path path = Files.write(Path.of(fileName), this.content);
        return path.toFile();
    }

    /**
     * Returns the field name for this file.
     *
     * @return the field name for this file
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Sets the field name for this file.
     *
     * @param name the field name for this file
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }
}
