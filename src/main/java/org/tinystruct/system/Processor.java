package org.tinystruct.system;

import org.tinystruct.ApplicationException;

/**
 * Interface for defining processors that operate on a specific type of input.
 *
 * @param <T> the type of input data that the processor operates on and returns
 */
public interface Processor<T> {

    /**
     * Processes the input data and returns the processed result.
     *
     * @param input the input data to process
     * @return the processed result of type T
     */
    T process(T input) throws ApplicationException;
}
