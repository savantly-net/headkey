package ai.headkey.memory.strategies.jpa;

import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.entities.MemoryEntity;

import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Strategy interface for JPA-based similarity search implementations.
 * 
 * This interface allows for different similarity search strategies to be
 * plugged into the JPA Memory Encoding System, enabling support for
 * different JPA providers and database backends with varying vector similarity capabilities.
 * 
 * Implementations can provide:
 * - Native vector similarity search (e.g., PostgreSQL with pgvector through JPA)
 * - Fallback text-based similarity search (e.g., H2, HSQLDB)
 * - Hybrid approaches combining multiple similarity methods
 * - Database-specific optimizations using native queries
 * 
 * @since 1.0
 */
public interface JpaSimilaritySearchStrategy {
    
    /**
     * Performs similarity search for memories based on query content and vector.
     * 
     * @param entityManager JPA EntityManager to use for the search
     * @param queryContent The original text content to search for
     * @param queryVector The vector representation of the query content (may be null for text-only search)
     * @param agentId Optional agent ID to filter results (null for all agents)
     * @param limit Maximum number of results to return
     * @param maxSimilaritySearchResults Maximum number of entities to retrieve for in-memory similarity calculation
     * @param similarityThreshold Minimum similarity threshold for results
     * @return List of MemoryRecords ordered by similarity score (highest first)
     * @throws Exception if the search operation fails
     */
    List<MemoryRecord> searchSimilar(EntityManager entityManager, String queryContent, 
                                   double[] queryVector, String agentId, int limit,
                                   int maxSimilaritySearchResults, double similarityThreshold) throws Exception;
    
    /**
     * Checks if this strategy supports vector-based similarity search.
     * 
     * @return true if the strategy can use vector embeddings for similarity search
     */
    boolean supportsVectorSearch();
    
    /**
     * Gets the name of this similarity search strategy.
     * 
     * @return A descriptive name for this strategy
     */
    String getStrategyName();
    
    /**
     * Initializes the strategy with any required setup.
     * This may include creating indexes, checking database capabilities, etc.
     * 
     * @param entityManager JPA EntityManager to use for initialization
     * @throws Exception if initialization fails
     */
    default void initialize(EntityManager entityManager) throws Exception {
        // Default implementation does nothing
    }
    
    /**
     * Validates that the database schema supports this similarity search strategy.
     * 
     * @param entityManager JPA EntityManager to validate against
     * @return true if the database schema is compatible with this strategy
     * @throws Exception if validation fails
     */
    default boolean validateSchema(EntityManager entityManager) throws Exception {
        // Default implementation assumes compatibility
        return true;
    }
    
    /**
     * Calculates cosine similarity between two vectors.
     * This is a utility method that can be used by implementations.
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Cosine similarity score (0.0 to 1.0)
     */
    default double calculateCosineSimilarity(double[] vector1, double[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}