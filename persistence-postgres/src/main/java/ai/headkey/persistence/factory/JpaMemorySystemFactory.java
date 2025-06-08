package ai.headkey.persistence.factory;

import ai.headkey.memory.abstracts.AbstractMemoryEncodingSystem;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.memory.strategies.jpa.JpaSimilaritySearchStrategy;
import ai.headkey.memory.strategies.jpa.JpaSimilaritySearchStrategyFactory;
import ai.headkey.memory.strategies.jpa.DefaultJpaSimilaritySearchStrategy;
import ai.headkey.memory.strategies.jpa.PostgresJpaSimilaritySearchStrategy;
import ai.headkey.memory.strategies.jpa.TextBasedJpaSimilaritySearchStrategy;
import ai.headkey.persistence.services.JpaMemoryEncodingSystem;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Factory for creating JPA-based memory encoding systems with optimal similarity search strategies.
 * 
 * This factory provides convenient methods for creating JPA memory systems with:
 * - Automatic database detection and strategy selection
 * - Custom similarity search strategies
 * - Pre-configured setups for common databases
 * - Builder pattern for complex configurations
 * 
 * @since 1.0
 */
public class JpaMemorySystemFactory {
    
    /**
     * Creates a JPA memory system with automatic strategy detection.
     * 
     * @param entityManagerFactory Pre-configured EntityManagerFactory
     * @return JpaMemoryEncodingSystem with optimal strategy for the database
     */
    public static JpaMemoryEncodingSystem createSystem(EntityManagerFactory entityManagerFactory) {
        return new JpaMemoryEncodingSystem(entityManagerFactory);
    }
    
    /**
     * Creates a JPA memory system with embedding generator and automatic strategy detection.
     * 
     * @param entityManagerFactory Pre-configured EntityManagerFactory
     * @param embeddingGenerator Function to generate vector embeddings
     * @return JpaMemoryEncodingSystem with optimal strategy for the database
     */
    public static JpaMemoryEncodingSystem createSystem(EntityManagerFactory entityManagerFactory,
                                                      AbstractMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator) {
        return new JpaMemoryEncodingSystem(entityManagerFactory, embeddingGenerator);
    }
    
    /**
     * Creates a JPA memory system with a specific similarity search strategy.
     * 
     * @param entityManagerFactory Pre-configured EntityManagerFactory
     * @param embeddingGenerator Function to generate vector embeddings
     * @param similarityStrategy Custom similarity search strategy
     * @return JpaMemoryEncodingSystem with the specified strategy
     */
    public static JpaMemoryEncodingSystem createSystemWithStrategy(
            EntityManagerFactory entityManagerFactory,
            AbstractMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator,
            JpaSimilaritySearchStrategy similarityStrategy) {
        return new JpaMemoryEncodingSystem(entityManagerFactory, embeddingGenerator, 
                                         100, true, 1000, 0.0, similarityStrategy);
    }
    
    /**
     * Creates a JPA memory system optimized for PostgreSQL with pgvector support.
     * 
     * @param persistenceUnitName Name of the persistence unit in persistence.xml
     * @param embeddingGenerator Function to generate vector embeddings
     * @return JpaMemoryEncodingSystem optimized for PostgreSQL
     */
    public static JpaMemoryEncodingSystem createPostgreSQLSystem(String persistenceUnitName,
                                                                AbstractMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnitName);
        PostgresJpaSimilaritySearchStrategy strategy = new PostgresJpaSimilaritySearchStrategy();
        return new JpaMemoryEncodingSystem(emf, embeddingGenerator, 100, true, 1000, 0.0, strategy);
    }
    
    /**
     * Creates a JPA memory system optimized for PostgreSQL with custom configuration.
     * 
     * @param persistenceUnitName Name of the persistence unit in persistence.xml
     * @param properties Additional JPA properties
     * @param embeddingGenerator Function to generate vector embeddings
     * @param maxSimilarityResults Maximum results for similarity search
     * @param similarityThreshold Minimum similarity threshold
     * @return JpaMemoryEncodingSystem optimized for PostgreSQL
     */
    public static JpaMemoryEncodingSystem createPostgreSQLSystem(String persistenceUnitName,
                                                                Properties properties,
                                                                AbstractMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator,
                                                                int maxSimilarityResults,
                                                                double similarityThreshold) {
        Map<String, Object> props = new HashMap<>();
        if (properties != null) {
            properties.forEach((key, value) -> props.put(key.toString(), value));
        }
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnitName, props);
        PostgresJpaSimilaritySearchStrategy strategy = new PostgresJpaSimilaritySearchStrategy();
        return new JpaMemoryEncodingSystem(emf, embeddingGenerator, 100, true, 
                                         maxSimilarityResults, similarityThreshold, strategy);
    }
    
    /**
     * Creates a JPA memory system optimized for H2 database.
     * 
     * @param persistenceUnitName Name of the persistence unit in persistence.xml
     * @param embeddingGenerator Function to generate vector embeddings
     * @return JpaMemoryEncodingSystem optimized for H2
     */
    public static JpaMemoryEncodingSystem createH2System(String persistenceUnitName,
                                                         AbstractMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnitName);
        TextBasedJpaSimilaritySearchStrategy strategy = new TextBasedJpaSimilaritySearchStrategy();
        return new JpaMemoryEncodingSystem(emf, embeddingGenerator, 100, true, 500, 0.0, strategy);
    }
    
    /**
     * Creates a JPA memory system for testing purposes with in-memory H2 database.
     * 
     * @param embeddingGenerator Function to generate vector embeddings
     * @return JpaMemoryEncodingSystem configured for testing
     */
    public static JpaMemoryEncodingSystem createTestSystem(AbstractMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        properties.put("jakarta.persistence.jdbc.user", "sa");
        properties.put("jakarta.persistence.jdbc.password", "");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("hibernate.show_sql", "false");
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("default", properties);
        TextBasedJpaSimilaritySearchStrategy strategy = new TextBasedJpaSimilaritySearchStrategy();
        return new JpaMemoryEncodingSystem(emf, embeddingGenerator, 50, false, 100, 0.0, strategy);
    }
    
    /**
     * Builder pattern for creating JPA memory systems with complex configurations.
     */
    public static class Builder {
        private String persistenceUnitName;
        private Properties properties = new Properties();
        private AbstractMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator;
        private JpaSimilaritySearchStrategy similarityStrategy;
        private int batchSize = 100;
        private boolean enableSecondLevelCache = true;
        private int maxSimilaritySearchResults = 1000;
        private double similarityThreshold = 0.0;
        private EntityManagerFactory entityManagerFactory;
        
        public Builder persistenceUnit(String persistenceUnitName) {
            this.persistenceUnitName = persistenceUnitName;
            return this;
        }
        
        public Builder entityManagerFactory(EntityManagerFactory entityManagerFactory) {
            this.entityManagerFactory = entityManagerFactory;
            return this;
        }
        
        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }
        
        public Builder properties(Properties properties) {
            this.properties.putAll(properties);
            return this;
        }
        
        public Builder embeddingGenerator(AbstractMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator) {
            this.embeddingGenerator = embeddingGenerator;
            return this;
        }
        
        public Builder similarityStrategy(JpaSimilaritySearchStrategy similarityStrategy) {
            this.similarityStrategy = similarityStrategy;
            return this;
        }
        
        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }
        
        public Builder enableSecondLevelCache(boolean enableSecondLevelCache) {
            this.enableSecondLevelCache = enableSecondLevelCache;
            return this;
        }
        
        public Builder maxSimilaritySearchResults(int maxSimilaritySearchResults) {
            this.maxSimilaritySearchResults = maxSimilaritySearchResults;
            return this;
        }
        
        public Builder similarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
            return this;
        }
        
        public Builder autoDetectStrategy() {
            this.similarityStrategy = null; // Will trigger auto-detection
            return this;
        }
        
        public Builder postgresStrategy() {
            this.similarityStrategy = new PostgresJpaSimilaritySearchStrategy();
            return this;
        }
        
        public Builder textBasedStrategy() {
            this.similarityStrategy = new TextBasedJpaSimilaritySearchStrategy();
            return this;
        }
        
        public Builder defaultStrategy() {
            this.similarityStrategy = new DefaultJpaSimilaritySearchStrategy();
            return this;
        }
        
        public JpaMemoryEncodingSystem build() {
            EntityManagerFactory emf;
            
            if (entityManagerFactory != null) {
                emf = entityManagerFactory;
            } else if (persistenceUnitName != null) {
                if (properties.isEmpty()) {
                    emf = Persistence.createEntityManagerFactory(persistenceUnitName);
                } else {
                    Map<String, Object> props = new HashMap<>();
                    properties.forEach((key, value) -> props.put(key.toString(), value));
                    emf = Persistence.createEntityManagerFactory(persistenceUnitName, props);
                }
            } else {
                throw new IllegalStateException("Either persistenceUnitName or entityManagerFactory must be provided");
            }
            
            return new JpaMemoryEncodingSystem(emf, embeddingGenerator, batchSize, 
                                             enableSecondLevelCache, maxSimilaritySearchResults, 
                                             similarityThreshold, similarityStrategy);
        }
    }
    
    /**
     * Creates a new builder instance.
     * 
     * @return New builder for configuring JPA memory systems
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Analyzes database capabilities and recommends the best strategy.
     * 
     * @param entityManagerFactory EntityManagerFactory to analyze
     * @return Information about recommended strategy and database capabilities
     */
    public static StrategyRecommendation analyzeDatabase(EntityManagerFactory entityManagerFactory) {
        try (var em = entityManagerFactory.createEntityManager()) {
            var capabilities = JpaSimilaritySearchStrategyFactory.analyzeDatabaseCapabilities(em);
            var recommendedStrategy = JpaSimilaritySearchStrategyFactory.createStrategy(em);
            
            return new StrategyRecommendation(capabilities, recommendedStrategy);
        }
    }
    
    /**
     * Information about recommended strategy and database capabilities.
     */
    public static class StrategyRecommendation {
        private final JpaSimilaritySearchStrategyFactory.DatabaseCapabilities capabilities;
        private final JpaSimilaritySearchStrategy recommendedStrategy;
        
        public StrategyRecommendation(JpaSimilaritySearchStrategyFactory.DatabaseCapabilities capabilities,
                                    JpaSimilaritySearchStrategy recommendedStrategy) {
            this.capabilities = capabilities;
            this.recommendedStrategy = recommendedStrategy;
        }
        
        public JpaSimilaritySearchStrategyFactory.DatabaseCapabilities getCapabilities() {
            return capabilities;
        }
        
        public JpaSimilaritySearchStrategy getRecommendedStrategy() {
            return recommendedStrategy;
        }
        
        public boolean hasVectorSupport() {
            return capabilities.hasVectorSupport();
        }
        
        public boolean hasFullTextSupport() {
            return capabilities.hasFullTextSupport();
        }
        
        @Override
        public String toString() {
            return String.format("StrategyRecommendation{capabilities=%s, strategy=%s}", 
                               capabilities, recommendedStrategy.getStrategyName());
        }
    }
}