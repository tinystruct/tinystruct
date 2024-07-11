package org.tinystruct.data;

/**
 * Represents a document with content and associated metadata.
 */
public class Document {
    // The main content of the document.
    private String content;

    // Metadata associated with the document.
    private Metadata metadata;

    /**
     * Constructs a new Document with the specified content.
     *
     * @param content the content of the document
     */
    public Document(String content) {
        this.content = content;
        this.metadata = new Metadata();
    }

    /**
     * Returns the content of the document.
     *
     * @return the content of the document
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content of the document.
     *
     * @param content the new content of the document
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns the metadata associated with the document.
     *
     * @return the metadata of the document
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata for the document.
     *
     * @param metadata the new metadata for the document
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Adds a metadata entry to the document.
     *
     * @param key   the key of the metadata entry
     * @param value the value of the metadata entry
     */
    public void addMetadata(String key, String value) {
        this.metadata.addMetadata(key, value);
    }

    /**
     * Retrieves the value of a specific metadata entry.
     *
     * @param key the key of the metadata entry
     * @return the value of the metadata entry, or null if not found
     */
    public String getMetadataValue(String key) {
        return this.metadata.getMetadataValue(key);
    }
}
