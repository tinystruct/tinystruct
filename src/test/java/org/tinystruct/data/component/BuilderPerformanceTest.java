package org.tinystruct.data.component;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationException;

/**
 * Performance test to compare org.json.JSONObject with Builder class.
 * Measures parsing, serialization, and memory usage.
 */
public class BuilderPerformanceTest {

    private static final int ITERATIONS = 10000;
    private static final int WARMUP_ITERATIONS = 1000;

    @Test
    public void testPerformanceComparison() throws ApplicationException {
        System.out.println("=== Performance Comparison: org.json.JSONObject vs Builder ===");
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

        // Simple JSON tests
        System.out.println("=== Simple JSON Performance ===");
        testSimpleJsonPerformance(simpleJson);

        // Complex JSON tests
        System.out.println("\n=== Complex JSON Performance ===");
        testComplexJsonPerformance(complexJson);

        // Large JSON tests
        System.out.println("\n=== Large JSON Performance ===");
        testLargeJsonPerformance(largeJson);

        // Memory usage tests
        System.out.println("\n=== Memory Usage Tests ===");
        testMemoryUsage(simpleJson, complexJson, largeJson);
    }

    private void warmup(String simpleJson, String complexJson, String largeJson) throws ApplicationException {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            // JSONObject warmup
            new JSONObject(simpleJson);
            new JSONObject(complexJson);
            new JSONObject(largeJson);

            // Builder warmup
            Builder builder1 = new Builder();
            builder1.parse(simpleJson);
            Builder builder2 = new Builder();
            builder2.parse(complexJson);
            Builder builder3 = new Builder();
            builder3.parse(largeJson);
        }
    }

    private void testSimpleJsonPerformance(String json) throws ApplicationException {
        // JSONObject parsing
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            new JSONObject(json);
        }
        long jsonObjectParseTime = System.nanoTime() - startTime;

        // Builder parsing
        startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Builder builder = new Builder();
            builder.parse(json);
        }
        long builderParseTime = System.nanoTime() - startTime;

        // JSONObject serialization
        JSONObject jsonObject = new JSONObject(json);
        startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            jsonObject.toString();
        }
        long jsonObjectSerializeTime = System.nanoTime() - startTime;

        // Builder serialization
        Builder builder = new Builder();
        builder.parse(json);
        startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            builder.toString();
        }
        long builderSerializeTime = System.nanoTime() - startTime;

        printResults("Simple JSON", jsonObjectParseTime, builderParseTime, 
                    jsonObjectSerializeTime, builderSerializeTime);
    }

    private void testComplexJsonPerformance(String json) throws ApplicationException {
        // JSONObject parsing
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            new JSONObject(json);
        }
        long jsonObjectParseTime = System.nanoTime() - startTime;

        // Builder parsing
        startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Builder builder = new Builder();
            builder.parse(json);
        }
        long builderParseTime = System.nanoTime() - startTime;

        // JSONObject serialization
        JSONObject jsonObject = new JSONObject(json);
        startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            jsonObject.toString();
        }
        long jsonObjectSerializeTime = System.nanoTime() - startTime;

        // Builder serialization
        Builder builder = new Builder();
        builder.parse(json);
        startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            builder.toString();
        }
        long builderSerializeTime = System.nanoTime() - startTime;

        printResults("Complex JSON", jsonObjectParseTime, builderParseTime, 
                    jsonObjectSerializeTime, builderSerializeTime);
    }

    private void testLargeJsonPerformance(String json) throws ApplicationException {
        // JSONObject parsing
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS / 10; i++) { // Fewer iterations for large JSON
            new JSONObject(json);
        }
        long jsonObjectParseTime = System.nanoTime() - startTime;

        // Builder parsing
        startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS / 10; i++) {
            Builder builder = new Builder();
            builder.parse(json);
        }
        long builderParseTime = System.nanoTime() - startTime;

        // JSONObject serialization
        JSONObject jsonObject = new JSONObject(json);
        startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS / 10; i++) {
            jsonObject.toString();
        }
        long jsonObjectSerializeTime = System.nanoTime() - startTime;

        // Builder serialization
        Builder builder = new Builder();
        builder.parse(json);
        startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS / 10; i++) {
            builder.toString();
        }
        long builderSerializeTime = System.nanoTime() - startTime;

        printResults("Large JSON", jsonObjectParseTime, builderParseTime, 
                    jsonObjectSerializeTime, builderSerializeTime);
    }

    private void testMemoryUsage(String simpleJson, String complexJson, String largeJson) throws ApplicationException {
        System.gc();
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Test JSONObject memory usage
        System.out.println("Testing JSONObject memory usage...");
        for (int i = 0; i < 1000; i++) {
            new JSONObject(simpleJson);
            new JSONObject(complexJson);
            new JSONObject(largeJson);
        }
        System.gc();
        long jsonObjectMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Test Builder memory usage
        System.out.println("Testing Builder memory usage...");
        for (int i = 0; i < 1000; i++) {
            Builder builder1 = new Builder();
            builder1.parse(simpleJson);
            Builder builder2 = new Builder();
            builder2.parse(complexJson);
            Builder builder3 = new Builder();
            builder3.parse(largeJson);
        }
        System.gc();
        long builderMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        System.out.println("Memory Usage (bytes):");
        System.out.println("  JSONObject: " + (jsonObjectMemory - initialMemory));
        System.out.println("  Builder: " + (builderMemory - jsonObjectMemory));
        System.out.println("  Builder advantage: " + 
            String.format("%.2f%%", (double)(jsonObjectMemory - initialMemory) / (builderMemory - jsonObjectMemory) * 100));
    }

    private void printResults(String testType, long jsonObjectParseTime, long builderParseTime,
                            long jsonObjectSerializeTime, long builderSerializeTime) {
        System.out.println(testType + " Results:");
        System.out.println("  Parsing (ns):");
        System.out.println("    JSONObject: " + jsonObjectParseTime);
        System.out.println("    Builder: " + builderParseTime);
        System.out.println("    Builder speedup: " + 
            String.format("%.2fx", (double) jsonObjectParseTime / builderParseTime));
        
        System.out.println("  Serialization (ns):");
        System.out.println("    JSONObject: " + jsonObjectSerializeTime);
        System.out.println("    Builder: " + builderSerializeTime);
        System.out.println("    Builder speedup: " + 
            String.format("%.2fx", (double) jsonObjectSerializeTime / builderSerializeTime));
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

    @Test
    public void testCharSequenceViewPerformance() throws ApplicationException {
        System.out.println("\n=== CharSequenceView Performance Test ===");
        
        String testJson = createComplexJson();
        int iterations = 10000;
        
        // Test with regular string parsing
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Builder builder = new Builder();
            builder.parse(testJson);
        }
        long regularTime = System.nanoTime() - startTime;
        
        System.out.println("Regular parsing time: " + regularTime + " ns");
        System.out.println("Average time per parse: " + (regularTime / iterations) + " ns");
    }

    @Test
    public void testConcurrentPerformance() throws InterruptedException, ApplicationException {
        System.out.println("\n=== Concurrent Performance Test ===");
        
        String testJson = createComplexJson();
        int threadCount = 4;
        int iterationsPerThread = 1000;
        
        Thread[] threads = new Thread[threadCount];
        long[] results = new long[threadCount];
        
        // Test JSONObject
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                long startTime = System.nanoTime();
                for (int i = 0; i < iterationsPerThread; i++) {
                    new JSONObject(testJson);
                }
                results[threadId] = System.nanoTime() - startTime;
            });
        }
        
        long startTime = System.nanoTime();
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long jsonObjectTime = System.nanoTime() - startTime;
        
        // Test Builder
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                long threadStartTime = System.nanoTime();
                for (int i = 0; i < iterationsPerThread; i++) {
                    try {
                        Builder builder = new Builder();
                        builder.parse(testJson);
                    } catch (ApplicationException e) {
                        e.printStackTrace();
                    }
                }
                results[threadId] = System.nanoTime() - threadStartTime;
            });
        }
        
        long builderStartTime = System.nanoTime();
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long builderTime = System.nanoTime() - builderStartTime;
        
        System.out.println("Concurrent performance (" + threadCount + " threads, " + iterationsPerThread + " iterations each):");
        System.out.println("  JSONObject: " + jsonObjectTime + " ns");
        System.out.println("  Builder: " + builderTime + " ns");
        System.out.println("  Builder speedup: " + String.format("%.2fx", (double) jsonObjectTime / builderTime));
    }
}
