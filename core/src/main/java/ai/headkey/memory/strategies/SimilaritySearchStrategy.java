package ai.headkey.memory.strategies;

import ai.headkey.memory.dto.MemoryRecord;

import java.sql.Connection;
import java.util.List;

/**
 * Strategy interface for similarity search implementations.
 * 
 * This interface allows for different similarity search strategies to be
 * plugged into the JDBC Memory Encoding System, enabling support for
 * different database backends with varying vector similarity capabilities.
 * 
 * Implementations can provide:
 * - Native vector similarity search (e.g., PostgreSQL with pgvector)
 * - Fallback text-based similarity search (e.g., HSQLDB)
 * - Hybrid approaches combining multiple similarity methods
 * 
 * @since 1.0
 */
public interface SimilaritySearchStrategy {
    
    /**
     * Performs similarity search for memories based on query content and vector.
     * 
     * @param connection Database connection to use for the search
     * @param queryContent The original text content to search for
     * @param queryVector The vector representation of the query content (may be null for text-only search)
     * @param agentId Optional agent ID to filter results (null for all agents)
     * @param limit Maximum number of results to return
     * @return List of MemoryRecords ordered by similarity score (highest first)
     * @throws Exception if the search operation fails
     */
    List<MemoryRecord> searchSimilar(Connection connection, String queryContent, 
                                   double[] queryVector, String agentId, int limit) throws Exception;
    
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
     * @param connection Database connection to use for initialization
     * @throws Exception if initialization fails
     */
    default void initialize(Connection connection) throws Exception {
        // Default implementation does nothing
    }
    
    /**
     * Validates that the database schema supports this similarity search strategy.
     * 
     * @param connection Database connection to validate against
     * @return true if the database schema is compatible with this strategy
     * @throws Exception if validation fails
     */
    default boolean validateSchema(Connection connection) throws Exception {
        // Default implementation assumes compatibility
        return true;
    }
}