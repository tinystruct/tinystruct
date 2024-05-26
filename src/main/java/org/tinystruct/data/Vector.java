package org.tinystruct.data;

import java.util.List;

/**
 * The Vector interface provides methods for handling collections of Data objects.
 * It supports adding, deleting, and finding similar documents based on a query.
 */
public interface Vector {

    /**
     * Adds a list of Data objects to the collection.
     *
     * @param documents the list of Data objects to be added
     */
    void add(List<Data> documents);

    /**
     * Deletes Data objects from the collection based on their identifiers.
     *
     * @param ids the list of identifiers of the Data objects to be deleted
     * @return true if the deletion was successful, false otherwise
     */
    boolean delete(List<String> ids);

    /**
     * Finds and returns a list of Data objects that are similar to the given query.
     *
     * @param query the query string to search for similar Data objects
     * @return a list of Data objects that are similar to the query
     */
    List<Data> similarity(String query);
}
