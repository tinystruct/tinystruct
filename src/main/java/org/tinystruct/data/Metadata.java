package org.tinystruct.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents metadata associated with a document.
 */
public class Metadata {
    // Map to store key-value pairs of metadata.
    private final Map<String, String> metadataMap;

    /**
     * Constructs a new Metadata instance.
     */
    public Metadata() {
        this.metadataMap = new HashMap<>();
    }

    /**
     * Adds a metadata entry with the specified key and value.
     *
     * @param key   the key of the metadata entry
     * @param value the value of the metadata entry
     */
    public void addMetadata(String key, String value) {
        this.metadataMap.put(key, value);
    }

    /**
     * Retrieves the value of a specific metadata entry.
     *
     * @param key the key of the metadata entry
     * @return the value associated with the key, or null if the key is not found
     */
    public String getMetadataValue(String key) {
        return this.metadataMap.get(key);
    }

    /**
     * Returns all metadata entries as a map.
     *
     * @return a map containing all metadata entries
     */
    public Map<String, String> getAllMetadata() {
        return metadataMap;
    }
}
