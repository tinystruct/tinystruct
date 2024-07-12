package org.tinystruct.data.tools;

public class CosineSimilarity {

    /**
     * Method to calculate the dot product and magnitude of a vector simultaneously.
     *
     * @param vectorA the first vector
     * @param vectorB the second vector
     * @return an array where index 0 is the dot product and index 1 is the magnitude
     */
    public static double[] dotProductAndMagnitude(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            magnitudeA += vectorA[i] * vectorA[i];
            magnitudeB += vectorB[i] * vectorB[i];
        }

        magnitudeA = Math.sqrt(magnitudeA);
        magnitudeB = Math.sqrt(magnitudeB);

        return new double[]{dotProduct, magnitudeA, magnitudeB};
    }

    /**
     * Method to calculate the cosine similarity between two vectors
     *
     * @param vectorA the first vector
     * @param vectorB the second vector
     * @return the cosine similarity between the two vectors, or 0.0 if one of the vectors has zero magnitude
     */
    public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double[] values = dotProductAndMagnitude(vectorA, vectorB);
        double dotProduct = values[0];
        double magnitudeA = values[1];
        double magnitudeB = values[2];

        // Check for zero magnitude to avoid division by zero
        if (magnitudeA == 0.0 || magnitudeB == 0.0) {
            return 0.0; // Return 0.0 to indicate undefined cosine similarity
        }

        return dotProduct / (magnitudeA * magnitudeB);
    }
}