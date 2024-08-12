package org.tinystruct.data;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.SearchResult;

import java.util.List;

/**
 * The Vector interface provides methods for handling collections of Data objects.
 * It supports adding, deleting, and finding similar documents based on a query.
 */
public interface Vector {

    /**
     * Adds a vector to the database with the associated label, entity ID, and entity type.
     *
     * @param vector     The vector to add, represented as a double array.
     * @param label      The label associated with the vector.
     * @param entityId   The ID of the entity associated with the vector.
     * @param entityType The type of the entity associated with the vector.
     * @throws ApplicationException If an error occurs while adding the vector to the database.
     */
    void add(double[] vector, String label, int entityId, String entityType) throws ApplicationException;

    /**
     * Deletes vectors from the database based on the provided list of vector IDs.
     *
     * @param ids A list of vector IDs to delete.
     * @return True if the deletion was successful, false otherwise.
     * @throws ApplicationException If an error occurs while deleting vectors from the database.
     */
    boolean delete(List<String> ids) throws ApplicationException;

    /**
     * Finds and returns a list of Data objects that are similar to the given query.
     *
     * @param query the query string to search for similar Data objects
     * @return a list of Data objects that are similar to the query
     */
    List<Data> similarity(String query);

    /**
     * Searches the database for vectors similar to the provided query vector.
     * Returns the top K most similar vectors based on cosine similarity.
     *
     * @param queryVector The vector to search for, represented as a double array.
     * @param topK        The number of top similar vectors to return.
     * @return A list of SearchResult objects containing the top K similar vectors.
     * @throws ApplicationException If an error occurs while searching the database.
     */
    List<SearchResult> search(double[] queryVector, int topK) throws ApplicationException;
}
