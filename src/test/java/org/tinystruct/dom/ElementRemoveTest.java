package org.tinystruct.dom;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ElementRemoveTest {

    @Test
    void testRemoveNonExistentElementDoesNotLoopInfinitely() {
        Element root = new Element("root");
        root.addElement("child1");
        root.addElement("child2");

        // Removing an element that does not exist should finish and return false or null,
        // instead of getting stuck in an infinite loop due to missing i++ in removeElement(String).
        assertTimeoutPreemptively(java.time.Duration.ofMillis(500), () -> {
            boolean result = root.removeElement("nonexistent");
            assertFalse(result);
        });
    }
}
