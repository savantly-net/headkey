package ai.headkey.rest.config;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration properties for the Memory System.
 *
 * This class maps application properties to strongly-typed configuration
 * objects using Quarkus's ConfigMapping feature. This provides type safety,
 * validation, and better IDE support compared to using @ConfigProperty.
 *
 * All properties are prefixed with "headkey.memory" and can be overridden
 * via environment variables or system properties following standard conventions.
 *
 * Example environment variable mappings:
 * - headkey.memory.strategy -> HEADKEY_MEMORY_STRATEGY
 * - headkey.memory.batch-size -> HEADKEY_MEMORY_BATCH_SIZE
 *
 * @since 1.0
 */
@ConfigMapping(prefix = "headkey.memory")
public interface MemorySystemProperties {
    /**
     * The persistence type to use for the memory system.
     *
     * Valid values:
     * - "postgres": Use PostgreSQL-based persistence with JPA
     * - "elasticsearch": Use Elasticsearch-based persistence
     *
     * @return the persistence type, defaults to "postgres"
     */
    @WithDefault("postgres")
    @WithName("persistence-type")
    String persistenceType();

    /**
     * The similarity search strategy to use.
     *
     * Valid values:
     * - "auto": Automatically detect optimal strategy based on database
     * - "text": Use text-based similarity search only
     * - "vector": Use vector-based similarity search with fallback
     * - "postgres": Use PostgreSQL-optimized strategy
     *
     * @return the strategy name, defaults to "auto"
     */
    @WithDefault("auto")
    String strategy();

    /**
     * Batch size for bulk operations in the memory system.
     *
     * @return the batch size, defaults to 100
     */
    @WithDefault("100")
    @WithName("batch-size")
    int batchSize();

    /**
     * Maximum number of results to return from similarity searches.
     *
     * @return the maximum similarity search results, defaults to 1000
     */
    @WithDefault("1000")
    @WithName("max-similarity-results")
    int maxSimilarityResults();

    /**
     * Minimum similarity threshold for search results (0.0 to 1.0).
     * Results below this threshold will be filtered out.
     *
     * @return the similarity threshold, defaults to 0.0 (no filtering)
     */
    @WithDefault("0.0")
    @WithName("similarity-threshold")
    double similarityThreshold();

    /**
     * Whether to enable JPA second-level cache for improved performance.
     *
     * @return true if second-level cache should be enabled, defaults to true
     */
    @WithDefault("true")
    @WithName("enable-second-level-cache")
    boolean enableSecondLevelCache();

    /**
     * Database configuration section.
     */
    DatabaseConfig database();

    /**
     * Nested configuration for database-specific settings.
     */
    interface DatabaseConfig {
        /**
         * The database type/kind being used.
         * This helps the system optimize for specific database capabilities.
         *
         * @return the database kind (e.g., "h2", "postgresql", "mysql")
         */
        @WithDefault("h2")
        String kind();

        /**
         * Whether to create database schema automatically.
         *
         * @return true if schema should be auto-created, defaults to true
         */
        @WithDefault("true")
        @WithName("auto-create-schema")
        boolean autoCreateSchema();

        /**
         * Connection pool configuration.
         */
        ConnectionPoolConfig pool();
    }

    /**
     * Nested configuration for database connection pool settings.
     */
    interface ConnectionPoolConfig {
        /**
         * Minimum number of connections in the pool.
         *
         * @return minimum pool size, defaults to 5
         */
        @WithDefault("5")
        @WithName("min-size")
        int minSize();

        /**
         * Maximum number of connections in the pool.
         *
         * @return maximum pool size, defaults to 20
         */
        @WithDefault("20")
        @WithName("max-size")
        int maxSize();

        /**
         * Connection timeout in milliseconds.
         *
         * @return connection timeout, defaults to 30000ms (30 seconds)
         */
        @WithDefault("30000")
        @WithName("timeout-ms")
        long timeoutMs();
    }

    /**
     * Embedding configuration section.
     */
    EmbeddingConfig embedding();

    /**
     * Nested configuration for vector embedding settings.
     */
    interface EmbeddingConfig {
        /**
         * Whether to enable vector embedding generation.
         *
         * @return true if embeddings should be generated, defaults to true
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * The dimension of generated embeddings.
         *
         * @return embedding dimension, defaults to 384
         */
        @WithDefault("384")
        int dimension();

        /**
         * The embedding model or strategy to use.
         *
         * @return embedding model name, defaults to "default"
         */
        @WithDefault("default")
        String model();
    }

    /**
     * Performance tuning configuration section.
     */
    PerformanceConfig performance();

    /**
     * Elasticsearch configuration section.
     */
    ElasticsearchConfig elasticsearch();

    /**
     * Nested configuration for performance tuning.
     */
    interface PerformanceConfig {
        /**
         * Whether to enable performance statistics collection.
         *
         * @return true if statistics should be collected, defaults to true
         */
        @WithDefault("true")
        @WithName("enable-statistics")
        boolean enableStatistics();

        /**
         * Cache size for frequently accessed memories.
         *
         * @return cache size, defaults to 1000
         */
        @WithDefault("1000")
        @WithName("cache-size")
        int cacheSize();

        /**
         * Whether to enable async processing for non-critical operations.
         *
         * @return true if async processing should be enabled, defaults to false
         */
        @WithDefault("false")
        @WithName("enable-async")
        boolean enableAsync();
    }

    /**
     * Nested configuration for Elasticsearch-specific settings.
     */
    interface ElasticsearchConfig {
        /**
         * Elasticsearch host configuration.
         *
         * @return host, defaults to "localhost"
         */
        @WithDefault("localhost")
        String host();

        /**
         * Elasticsearch port configuration.
         *
         * @return port, defaults to 9200
         */
        @WithDefault("9200")
        int port();

        /**
         * Elasticsearch scheme (http/https).
         *
         * @return scheme, defaults to "http"
         */
        @WithDefault("http")
        String scheme();

        /**
         * Elasticsearch username for authentication.
         *
         * @return username, empty by default
         */
        Optional<String> username();

        /**
         * Elasticsearch password for authentication.
         *
         * @return password, empty by default
         */
        Optional<String> password();

        /**
         * Connection timeout in milliseconds.
         *
         * @return connection timeout, defaults to 5000ms (5 seconds)
         */
        @WithDefault("5000")
        @WithName("connect-timeout-ms")
        int connectTimeoutMs();

        /**
         * Socket timeout in milliseconds.
         *
         * @return socket timeout, defaults to 30000ms (30 seconds)
         */
        @WithDefault("30000")
        @WithName("socket-timeout-ms")
        int socketTimeoutMs();

        /**
         * Maximum number of connections.
         *
         * @return max connections, defaults to 100
         */
        @WithDefault("100")
        @WithName("max-connections")
        int maxConnections();

        /**
         * Maximum number of connections per route.
         *
         * @return max connections per route, defaults to 10
         */
        @WithDefault("10")
        @WithName("max-connections-per-route")
        int maxConnectionsPerRoute();

        /**
         * Whether to automatically create indices.
         *
         * @return true if indices should be auto-created, defaults to true
         */
        @WithDefault("true")
        @WithName("auto-create-indices")
        boolean autoCreateIndices();

        /**
         * Search timeout in milliseconds.
         *
         * @return search timeout, defaults to 30000ms (30 seconds)
         */
        @WithDefault("30000")
        @WithName("search-timeout-ms")
        int searchTimeoutMs();
    }
}
