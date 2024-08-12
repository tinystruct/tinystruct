package org.tinystruct.data.component;

/**
 * The SearchResult class represents the result of a search operation in a vector database.
 * It contains the vector data, a label describing the vector, an entity ID and type associated
 * with the vector, and the similarity score between the query vector and this vector.
 */
public class SearchResult {
    private final double[] vector;  // The vector data representing the result.
    private final String label;     // A descriptive label for the vector.
    private final int entityId;     // The ID of the entity associated with the vector.
    private final String entityType; // The type of the entity associated with the vector.
    private final double similarity; // The similarity score between the query vector and this vector.

    /**
     * Constructs a SearchResult object with the given vector, label, entity ID, entity type, and similarity score.
     *
     * @param vector      The vector data representing the result.
     * @param label       A descriptive label for the vector.
     * @param entityId    The ID of the entity associated with the vector.
     * @param entityType  The type of the entity associated with the vector.
     * @param similarity  The similarity score between the query vector and this vector.
     */
    public SearchResult(double[] vector, String label, int entityId, String entityType, double similarity) {
        this.vector = vector;
        this.label = label;
        this.entityId = entityId;
        this.entityType = entityType;
        this.similarity = similarity;
    }

    /**
     * Returns the vector data associated with this search result.
     *
     * @return A double array representing the vector data.
     */
    public double[] getVector() {
        return vector;
    }

    /**
     * Returns the label associated with this search result.
     *
     * @return A string representing the label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the entity ID associated with this search result.
     *
     * @return An integer representing the entity ID.
     */
    public int getEntityId() {
        return entityId;
    }

    /**
     * Returns the entity type associated with this search result.
     *
     * @return A string representing the entity type.
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Returns the similarity score between the query vector and this vector.
     *
     * @return A double representing the similarity score.
     */
    public double getSimilarity() {
        return similarity;
    }
}
