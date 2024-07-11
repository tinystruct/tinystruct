package org.tinystruct.system.chain;

import org.tinystruct.AbstractApplication;
import org.tinystruct.system.Processor;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for implementing processor chains.
 *
 * @param <T> the type of data processed by the chain
 */
public abstract class AbstractProcessorChain<T> extends AbstractApplication {

    // List to hold processors in the chain
    private final List<Processor<T>> processors = new ArrayList<>();

    /**
     * Adds a processor to the chain.
     *
     * @param processor the processor to add
     */
    public void addProcessor(Processor<T> processor) {
        processors.add(processor);
    }

    /**
     * Processes the input data through all processors in the chain.
     *
     * @param input the input data to process
     * @return the processed data after passing through all processors
     */
    public T process(T input) {
        T currentData = input;
        for (Processor<T> processor : processors) {
            currentData = processor.process(currentData);
        }
        return currentData;
    }

    /**
     * Returns the version of the processor chain (empty implementation in this abstract class).
     *
     * @return the version string
     */
    @Override
    public String version() {
        return ""; // Version information can be provided in subclasses
    }
}
