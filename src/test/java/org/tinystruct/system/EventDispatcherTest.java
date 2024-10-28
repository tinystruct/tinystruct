package org.tinystruct.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Test class for EventDispatcher
class EventDispatcherTest {

    private EventDispatcher eventDispatcher;

    @BeforeEach
    void setUp() {
        eventDispatcher = EventDispatcher.getInstance();
    }

    // A simple event class for testing purposes implementing the Event interface
    static class TestEvent implements Event<String> {
        private final String name;
        private final String payload;

        public TestEvent(String name, String payload) {
            this.name = name;
            this.payload = payload;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getPayload() {
            return payload;
        }
    }

    @Test
    void testRegisterAndDispatchEvent() {
        // Create a counter to verify if the handler is called
        AtomicInteger counter = new AtomicInteger(0);

        // Register a handler for TestEvent
        eventDispatcher.registerHandler(TestEvent.class, event -> {
            assertNotNull(event);
            counter.incrementAndGet(); // Increment the counter when the event is handled
            System.out.println("Handled event: " + event.getPayload());
        });

        // Create a TestEvent instance
        TestEvent testEvent = new TestEvent("Test Event", "Hello, Event!");

        // Dispatch the event
        eventDispatcher.dispatch(testEvent);

        // Verify the handler was invoked
        assertEquals(1, counter.get(), "Handler should be called once");
    }

    @Test
    void testDispatchWithoutRegisteredHandlers() {
        // Create an event that has no handler registered
        TestEvent testEvent = new TestEvent("Unregistered Event", "No handlers for this event");

        // Dispatch the event without any registered handlers
        eventDispatcher.dispatch(testEvent); // Should not throw any exception
    }

    @Test
    void testMultipleHandlersForSameEvent() {
        // Create a counter to track how many times handlers are called
        AtomicInteger counter = new AtomicInteger(0);

        // Register multiple handlers for the same event type
        Consumer<TestEvent> handler1 = event -> {
            counter.incrementAndGet(); // Increment for handler 1
            System.out.println("Handler 1 processed: " + event.getPayload());
        };

        Consumer<TestEvent> handler2 = event -> {
            counter.incrementAndGet(); // Increment for handler 2
            System.out.println("Handler 2 processed: " + event.getPayload());
        };

        eventDispatcher.registerHandler(TestEvent.class, handler1);
        eventDispatcher.registerHandler(TestEvent.class, handler2);

        // Create a TestEvent instance
        TestEvent testEvent = new TestEvent("Multiple Handlers Event", "Hello, Multiple Handlers!");

        // Dispatch the event
        eventDispatcher.dispatch(testEvent);

        // Verify both handlers were invoked
        assertEquals(2, counter.get(), "Both handlers should be called once");
    }
}
