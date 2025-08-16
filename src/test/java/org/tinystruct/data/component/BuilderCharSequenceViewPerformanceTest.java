package org.tinystruct.data.component;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

/**
 * Performance test focused on CharSequenceView improvements in Builder class.
 * Tests parsing performance and memory efficiency without external dependencies.
 */
public class BuilderCharSequenceViewPerformanceTest {

    private static final int ITERATIONS = 10000;
    private static final int WARMUP_ITERATIONS = 1000;

    @Test
    public void testCharSequenceViewPerformance() throws ApplicationException {
        System.out.println("=== CharSequenceView Performance Test ===");
        System.out.println("Iterations: " + ITERATIONS);
        System.out.println("Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println();

        // Test data
        String simpleJson = createSimpleJson();
        String complexJson = createComplexJson();
        String largeJson = createLargeJson();

        // Warmup
        System.out.println("Warming up...");
        warmup(simpleJson, complexJson, largeJson);
        System.out.println("Warmup completed.\n");

        // Performance tests
        System.out.println("=== Simple JSON Performance ===");
        testJsonPerformance("Simple JSON", simpleJson);

        System.out.println("\n=== Complex JSON Performance ===");
        testJsonPerformance("Complex JSON", complexJson);

        System.out.println("\n=== Large JSON Performance ===");
        testJsonPerformance("Large JSON", largeJson, ITERATIONS / 10);

        // Memory usage test
        System.out.println("\n=== Memory Usage Test ===");
        testMemoryUsage(simpleJson, complexJson, largeJson);

        // Concurrent performance test
        System.out.println("\n=== Concurrent Performance Test ===");
        try {
            testConcurrentPerformance(complexJson);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Concurrent test interrupted: " + e.getMessage());
        }
    }

    private void warmup(String simpleJson, String complexJson, String largeJson) throws ApplicationException {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Builder builder1 = new Builder();
            builder1.parse(simpleJson);
            Builder builder2 = new Builder();
            builder2.parse(complexJson);
            Builder builder3 = new Builder();
            builder3.parse(largeJson);
        }
    }

    private void testJsonPerformance(String testType, String json) throws ApplicationException {
        testJsonPerformance(testType, json, ITERATIONS);
    }

    private void testJsonPerformance(String testType, String json, int iterations) throws ApplicationException {
        // Parsing performance
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Builder builder = new Builder();
            builder.parse(json);
        }
        long parseTime = System.nanoTime() - startTime;

        // Serialization performance
        Builder builder = new Builder();
        builder.parse(json);
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            builder.toString();
        }
        long serializeTime = System.nanoTime() - startTime;

        System.out.println(testType + " Results:");
        System.out.println("  Parsing time: " + parseTime + " ns");
        System.out.println("  Average parse time: " + (parseTime / iterations) + " ns");
        System.out.println("  Serialization time: " + serializeTime + " ns");
        System.out.println("  Average serialize time: " + (serializeTime / iterations) + " ns");
        System.out.println("  JSON size: " + json.length() + " characters");
    }

    private void testMemoryUsage(String simpleJson, String complexJson, String largeJson) throws ApplicationException {
        System.gc();
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        System.out.println("Testing memory usage...");
        for (int i = 0; i < 1000; i++) {
            Builder builder1 = new Builder();
            builder1.parse(simpleJson);
            Builder builder2 = new Builder();
            builder2.parse(complexJson);
            Builder builder3 = new Builder();
            builder3.parse(largeJson);
        }
        System.gc();
        long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        System.out.println("Memory Usage:");
        System.out.println("  Initial memory: " + initialMemory + " bytes");
        System.out.println("  Final memory: " + finalMemory + " bytes");
        System.out.println("  Memory used: " + (finalMemory - initialMemory) + " bytes");
        System.out.println("  Average per operation: " + ((finalMemory - initialMemory) / 3000) + " bytes");
    }

    private void testConcurrentPerformance(String json) throws InterruptedException, ApplicationException {
        int threadCount = 4;
        int iterationsPerThread = 1000;

        Thread[] threads = new Thread[threadCount];
        long[] results = new long[threadCount];

        // Create threads
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                long startTime = System.nanoTime();
                for (int i = 0; i < iterationsPerThread; i++) {
                    try {
                        Builder builder = new Builder();
                        builder.parse(json);
                    } catch (ApplicationException e) {
                        e.printStackTrace();
                    }
                }
                results[threadId] = System.nanoTime() - startTime;
            });
        }

        // Start all threads
        long startTime = System.nanoTime();
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long totalTime = System.nanoTime() - startTime;

        System.out.println("Concurrent performance (" + threadCount + " threads, " + iterationsPerThread + " iterations each):");
        System.out.println("  Total time: " + totalTime + " ns");
        System.out.println("  Average time per thread: " + (totalTime / threadCount) + " ns");
        System.out.println("  Total operations: " + (threadCount * iterationsPerThread));
        System.out.println("  Average time per operation: " + (totalTime / (threadCount * iterationsPerThread)) + " ns");
    }

    @Test
    public void testCharSequenceViewEfficiency() throws ApplicationException {
        System.out.println("\n=== CharSequenceView Efficiency Test ===");
        
        String testJson = createComplexJson();
        int iterations = 10000;
        
        // Test parsing performance
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Builder builder = new Builder();
            builder.parse(testJson);
        }
        long parseTime = System.nanoTime() - startTime;
        
        System.out.println("Parsing Performance:");
        System.out.println("  Total time: " + parseTime + " ns");
        System.out.println("  Average time per parse: " + (parseTime / iterations) + " ns");
        System.out.println("  JSON size: " + testJson.length() + " characters");
        System.out.println("  Time per character: " + String.format("%.2f", (double) parseTime / (iterations * testJson.length())) + " ns");
    }

    @Test
    public void testDifferentJsonSizes() throws ApplicationException {
        System.out.println("\n=== Different JSON Sizes Performance Test ===");
        
        String[] testJsons = {
            createSimpleJson(),
            createComplexJson(),
            createLargeJson(),
            createVeryLargeJson()
        };
        
        String[] testNames = {"Simple", "Complex", "Large", "Very Large"};
        
        for (int i = 0; i < testJsons.length; i++) {
            String json = testJsons[i];
            String name = testNames[i];
            int iterations = i < 2 ? ITERATIONS : ITERATIONS / 10; // Fewer iterations for larger JSONs
            
            long startTime = System.nanoTime();
            for (int j = 0; j < iterations; j++) {
                Builder builder = new Builder();
                builder.parse(json);
            }
            long parseTime = System.nanoTime() - startTime;
            
            System.out.println(name + " JSON (" + json.length() + " chars):");
            System.out.println("  Parse time: " + parseTime + " ns");
            System.out.println("  Average time: " + (parseTime / iterations) + " ns");
            System.out.println("  Time per char: " + String.format("%.2f", (double) parseTime / (iterations * json.length())) + " ns");
            System.out.println();
        }
    }

    private String createSimpleJson() {
        return "{\"name\":\"John Doe\",\"age\":30,\"email\":\"john@example.com\",\"active\":true}";
    }

    private String createComplexJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"user\":{\"id\":12345,\"name\":\"John Doe\",\"email\":\"john@example.com\"},");
        sb.append("\"profile\":{\"age\":30,\"city\":\"New York\",\"country\":\"USA\"},");
        sb.append("\"preferences\":{\"theme\":\"dark\",\"notifications\":true,\"language\":\"en\"},");
        sb.append("\"metadata\":{\"created\":\"2023-01-01\",\"updated\":\"2023-12-01\",\"version\":\"1.0\"}");
        sb.append("}");
        return sb.toString();
    }

    private String createLargeJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        // Add many simple key-value pairs
        for (int i = 0; i < 1000; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"key").append(i).append("\":\"value").append(i).append("\"");
        }
        
        // Add some nested objects
        sb.append(",\"nested1\":{");
        for (int i = 0; i < 100; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"nkey").append(i).append("\":").append(i);
        }
        sb.append("}");
        
        // Add arrays
        sb.append(",\"array1\":[");
        for (int i = 0; i < 500; i++) {
            if (i > 0) sb.append(",");
            sb.append(i);
        }
        sb.append("]");
        
        sb.append("}");
        return sb.toString();
    }

    private String createVeryLargeJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        // Add many simple key-value pairs
        for (int i = 0; i < 5000; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"key").append(i).append("\":\"value").append(i).append("\"");
        }
        
        // Add multiple nested objects
        for (int j = 0; j < 10; j++) {
            sb.append(",\"nested").append(j).append("\":{");
            for (int i = 0; i < 200; i++) {
                if (i > 0) sb.append(",");
                sb.append("\"nkey").append(i).append("\":").append(i);
            }
            sb.append("}");
        }
        
        // Add multiple arrays
        for (int j = 0; j < 5; j++) {
            sb.append(",\"array").append(j).append("\":[");
            for (int i = 0; i < 1000; i++) {
                if (i > 0) sb.append(",");
                sb.append(i);
            }
            sb.append("]");
        }
        
        sb.append("}");
        return sb.toString();
    }
}
