package ai.headkey.memory.entities.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.Arrays;

/**
 * JPA AttributeConverter for vector embeddings to JSON conversion.
 * 
 * This converter handles the serialization and deserialization of double arrays
 * (vector embeddings) to/from JSON strings for database storage. It provides
 * efficient storage of high-dimensional vectors while maintaining compatibility
 * across different database systems.
 * 
 * The converter supports:
 * - Automatic conversion of double[] to JSON array strings
 * - Null-safe operations
 * - Optimized JSON format for minimal storage overhead
 * - Error handling with meaningful exception messages
 * 
 * For databases with native vector support (like PostgreSQL with pgvector),
 * this converter can be extended or replaced with database-specific implementations.
 * 
 * @since 1.0
 */
@Converter(autoApply = true)
public class VectorEmbeddingConverter implements AttributeConverter<double[], String> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<double[]> DOUBLE_ARRAY_TYPE_REF = new TypeReference<double[]>() {};
    
    /**
     * Converts double array (vector embedding) to JSON string for database storage.
     * 
     * The conversion produces a compact JSON array format like:
     * [0.1234, -0.5678, 0.9012, ...]
     * 
     * @param attribute The double array to convert
     * @return JSON string representation, or null if attribute is null
     * @throws RuntimeException if JSON serialization fails
     */
    @Override
    public String convertToDatabaseColumn(double[] attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert vector embedding to JSON: " + 
                                     "Array length: " + attribute.length + 
                                     ", First few values: " + Arrays.toString(Arrays.copyOf(attribute, Math.min(5, attribute.length))), e);
        }
    }
    
    /**
     * Converts JSON string from database to double array (vector embedding).
     * 
     * Parses JSON array format back into a double array. Handles various
     * edge cases including empty arrays and malformed JSON.
     * 
     * @param dbData The JSON string from database
     * @return double array, or null if dbData is null or empty
     * @throws RuntimeException if JSON deserialization fails
     */
    @Override
    public double[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        try {
            double[] result = objectMapper.readValue(dbData, DOUBLE_ARRAY_TYPE_REF);
            
            // Validate the result
            if (result != null) {
                for (int i = 0; i < result.length; i++) {
                    if (!Double.isFinite(result[i])) {
                        throw new RuntimeException("Invalid vector embedding: non-finite value at index " + i + ": " + result[i]);
                    }
                }
            }
            
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert JSON to vector embedding: " + 
                                     "JSON length: " + dbData.length() + 
                                     ", JSON preview: " + dbData.substring(0, Math.min(100, dbData.length())), e);
        }
    }
    
    /**
     * Utility method to validate vector embedding dimensions.
     * Can be used by the application layer to ensure vector consistency.
     * 
     * @param embedding The vector embedding to validate
     * @param expectedDimensions Expected number of dimensions (0 for any)
     * @return true if valid
     * @throws IllegalArgumentException if validation fails
     */
    public static boolean validateEmbedding(double[] embedding, int expectedDimensions) {
        if (embedding == null) {
            return true; // null is considered valid
        }
        
        if (expectedDimensions > 0 && embedding.length != expectedDimensions) {
            throw new IllegalArgumentException("Vector embedding has " + embedding.length + 
                                             " dimensions, expected " + expectedDimensions);
        }
        
        for (int i = 0; i < embedding.length; i++) {
            if (!Double.isFinite(embedding[i])) {
                throw new IllegalArgumentException("Vector embedding contains non-finite value at index " + i + ": " + embedding[i]);
            }
        }
        
        return true;
    }
    
    /**
     * Utility method to calculate the magnitude (L2 norm) of a vector.
     * Useful for normalizing vectors or calculating similarities.
     * 
     * @param embedding The vector embedding
     * @return The magnitude of the vector, or 0.0 if null or empty
     */
    public static double calculateMagnitude(double[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return 0.0;
        }
        
        double sumSquares = 0.0;
        for (double value : embedding) {
            sumSquares += value * value;
        }
        
        return Math.sqrt(sumSquares);
    }
    
    /**
     * Utility method to normalize a vector to unit length.
     * 
     * @param embedding The vector to normalize
     * @return A new normalized vector, or null if input is null
     * @throws IllegalArgumentException if the vector has zero magnitude
     */
    public static double[] normalize(double[] embedding) {
        if (embedding == null) {
            return null;
        }
        
        double magnitude = calculateMagnitude(embedding);
        if (magnitude == 0.0) {
            throw new IllegalArgumentException("Cannot normalize zero-magnitude vector");
        }
        
        double[] normalized = new double[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            normalized[i] = embedding[i] / magnitude;
        }
        
        return normalized;
    }
}