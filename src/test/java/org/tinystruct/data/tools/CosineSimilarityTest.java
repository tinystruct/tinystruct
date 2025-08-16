package org.tinystruct.data.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CosineSimilarityTest {

    @Test
    public void testDotProductAndMagnitude() {
        double[] vectorA = {1.0, 2.0, 3.0};
        double[] vectorB = {4.0, 5.0, 6.0};

        double[] result = CosineSimilarity.dotProductAndMagnitude(vectorA, vectorB);

        // Assert dot product calculation
        assertEquals(32.0, result[0], 0.0001);

        // Assert magnitude calculation for vectorA
        assertEquals(Math.sqrt(14.0), result[1], 0.0001);

        // Assert magnitude calculation for vectorB
        assertEquals(Math.sqrt(77.0), result[2], 0.0001);
    }

    @Test
    public void testCosineSimilarity() {
        double[] vectorA = {1.0, 2.0, 3.0};
        double[] vectorB = {4.0, 5.0, 6.0};

        double similarity = CosineSimilarity.cosineSimilarity(vectorA, vectorB);

        assertEquals(0.9746318461970762, similarity, 0.0001);
    }

    @Test
    public void testCosineSimilarityZeroMagnitude() {
        double[] vectorA = {1.0, 2.0, 3.0};
        double[] vectorB = {0.0, 0.0, 0.0};

        double similarity = CosineSimilarity.cosineSimilarity(vectorA, vectorB);

        assertEquals(0.0, similarity, 0.0001);
    }
}
