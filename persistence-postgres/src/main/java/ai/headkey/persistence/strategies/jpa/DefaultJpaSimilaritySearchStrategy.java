package ai.headkey.persistence.strategies.jpa;

import ai.headkey.memory.dto.MemoryRecord;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default JPA similarity search strategy that delegates to specialized implementations.
 *
 * This coordinator strategy:
 * - Automatically detects database capabilities (PostgreSQL vs others)
 * - Delegates to PostgresJpaSimilaritySearchStrategy for PostgreSQL databases
 * - Delegates to TextBasedJpaSimilaritySearchStrategy for other databases
 * - Provides a unified interface while leveraging database-specific optimizations
 * - Handles strategy initialization and caching for performance
 *
 * The delegation is transparent to the caller and provides the best possible
 * search performance for each database type.
 *
 * @since 1.0
 */
public class DefaultJpaSimilaritySearchStrategy
    implements JpaSimilaritySearchStrategy {

    private static final Logger log = LoggerFactory.getLogger(
        DefaultJpaSimilaritySearchStrategy.class
    );

    private JpaSimilaritySearchStrategy delegateStrategy;
    private boolean isInitialized = false;
    private DatabaseType databaseType;

    /**
     * Supported database types for strategy selection.
     */
    private enum DatabaseType {
        POSTGRESQL,
        H2,
        OTHER,
    }

    @Override
    public List<MemoryRecord> searchSimilar(
        EntityManager entityManager,
        String queryContent,
        double[] queryVector,
        String agentId,
        int limit,
        int maxSimilaritySearchResults,
        double similarityThreshold
    ) throws Exception {
        log.debug(
            "Executing similarity search with queryContent: {}, agentId: {}, limit: {}, " +
            "maxResults: {}, similarityThreshold: {}",
            queryContent,
            agentId,
            limit,
            maxSimilaritySearchResults,
            similarityThreshold
        );
        ensureInitialized(entityManager);

        return delegateStrategy.searchSimilar(
            entityManager,
            queryContent,
            queryVector,
            agentId,
            limit,
            maxSimilaritySearchResults,
            similarityThreshold
        );
    }

    @Override
    public boolean supportsVectorSearch() {
        if (!isInitialized) {
            throw new IllegalStateException(
                "DefaultJpaSimilaritySearchStrategy is not initialized"
            );
        }
        return delegateStrategy.supportsVectorSearch();
    }

    @Override
    public String getStrategyName() {
        if (!isInitialized) {
            return "Default JPA Similarity Search (uninitialized)";
        }
        return (
            "Default JPA Similarity Search (delegating to: " +
            delegateStrategy.getStrategyName() +
            ")"
        );
    }

    @Override
    public void initialize(EntityManager entityManager) throws Exception {
        if (!isInitialized) {
            initializeDelegate(entityManager);
        }
        // Always call initialize on the delegate in case it needs to update its state
        if (delegateStrategy != null) {
            delegateStrategy.initialize(entityManager);
        }
    }

    @Override
    public boolean validateSchema(EntityManager entityManager)
        throws Exception {
        ensureInitialized(entityManager);
        return delegateStrategy.validateSchema(entityManager);
    }

    /**
     * Calculates cosine similarity between two vectors.
     * This method is provided as a utility for any strategies that need it.
     *
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Cosine similarity value between 0.0 and 1.0, or 0.0 if vectors are null/invalid
     */
    @Override
    public double calculateCosineSimilarity(
        double[] vector1,
        double[] vector2
    ) {
        if (
            vector1 == null ||
            vector2 == null ||
            vector1.length != vector2.length ||
            vector1.length == 0
        ) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            magnitude1 += vector1[i] * vector1[i];
            magnitude2 += vector2[i] * vector2[i];
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (magnitude1 * magnitude2);
    }

    /**
     * Ensures the strategy is initialized and ready to use.
     *
     * @param entityManager EntityManager for database operations
     * @throws Exception if initialization fails
     */
    private void ensureInitialized(EntityManager entityManager)
        throws Exception {
        if (!isInitialized) {
            initializeDelegate(entityManager);
        }
    }

    /**
     * Initializes the appropriate delegate strategy based on database type.
     *
     * @param entityManager EntityManager for database detection
     * @throws Exception if initialization fails
     */
    private void initializeDelegate(EntityManager entityManager)
        throws Exception {
        try {
            databaseType = detectDatabaseType(entityManager);

            switch (databaseType) {
                case POSTGRESQL:
                    delegateStrategy =
                        new PostgresJpaSimilaritySearchStrategy();
                    break;
                case H2:
                case OTHER:
                default:
                    delegateStrategy =
                        new TextBasedJpaSimilaritySearchStrategy();
                    break;
            }

            // Initialize the delegate strategy
            log.info(
                "Initializing {} strategy for database type: {}",
                delegateStrategy.getStrategyName(),
                databaseType
            );
            delegateStrategy.initialize(entityManager);
            isInitialized = true;
        } catch (Exception e) {
            // Fallback to text-based strategy if detection/initialization fails
            delegateStrategy = new TextBasedJpaSimilaritySearchStrategy();
            delegateStrategy.initialize(entityManager);
            isInitialized = true;
            databaseType = DatabaseType.OTHER;

            // Log the fallback decision (in a real implementation, use proper logging)
            System.err.println(
                "Warning: Failed to detect database type or initialize preferred strategy. " +
                "Falling back to TextBasedJpaSimilaritySearchStrategy. Error: " +
                e.getMessage()
            );
        }
    }

    /**
     * Detects the database type based on the EntityManager configuration.
     *
     * @param entityManager EntityManager to inspect
     * @return Detected database type
     * @throws Exception if database type cannot be determined
     */
    private DatabaseType detectDatabaseType(EntityManager entityManager)
        throws Exception {
        try {
            // Check Hibernate dialect configuration
            Object dialectProperty = entityManager
                .getEntityManagerFactory()
                .getProperties()
                .get("hibernate.dialect");

            if (dialectProperty != null) {
                String dialect = dialectProperty.toString().toLowerCase();

                if (dialect.contains("postgresql")) {
                    return DatabaseType.POSTGRESQL;
                } else if (dialect.contains("h2")) {
                    return DatabaseType.H2;
                }
            }

            // Fallback: Try to detect via native query
            try {
                // PostgreSQL detection query
                entityManager
                    .createNativeQuery("SELECT version()")
                    .setMaxResults(1)
                    .getSingleResult();
                Object result = entityManager
                    .createNativeQuery("SELECT version()")
                    .setMaxResults(1)
                    .getSingleResult();
                if (
                    result != null &&
                    result.toString().toLowerCase().contains("postgresql")
                ) {
                    return DatabaseType.POSTGRESQL;
                }
            } catch (Exception e) {
                // Ignore and try other detection methods
            }

            try {
                // H2 detection query
                entityManager
                    .createNativeQuery("SELECT H2VERSION()")
                    .setMaxResults(1)
                    .getSingleResult();
                return DatabaseType.H2;
            } catch (Exception e) {
                // Ignore and fall through to default
            }

            // Default fallback
            return DatabaseType.OTHER;
        } catch (Exception e) {
            throw new Exception("Failed to detect database type", e);
        }
    }

    /**
     * Gets the current delegate strategy.
     * Primarily for testing and debugging purposes.
     *
     * @return The current delegate strategy, or null if not initialized
     */
    public JpaSimilaritySearchStrategy getDelegateStrategy() {
        return delegateStrategy;
    }

    /**
     * Gets the detected database type.
     * Primarily for testing and debugging purposes.
     *
     * @return The detected database type, or null if not initialized
     */
    public String getDatabaseType() {
        return databaseType != null ? databaseType.toString() : "UNKNOWN";
    }

    /**
     * Forces re-initialization of the strategy.
     * Useful if the database configuration changes at runtime.
     *
     * @param entityManager EntityManager for re-initialization
     * @throws Exception if re-initialization fails
     */
    public void reinitialize(EntityManager entityManager) throws Exception {
        isInitialized = false;
        delegateStrategy = null;
        databaseType = null;
        initializeDelegate(entityManager);
    }
}
