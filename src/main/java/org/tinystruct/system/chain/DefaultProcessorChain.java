package org.tinystruct.system.chain;

import org.tinystruct.system.annotation.Action;

public class DefaultProcessorChain extends AbstractProcessorChain<String> {

    /**
     * Initializes the processor chain (empty implementation in this abstract class).
     */
    @Override
    public void init() {
        // Processors can be added here.

    }

    @Action("process")
    public String process(String text) {
        return super.process(text);
    }

}
