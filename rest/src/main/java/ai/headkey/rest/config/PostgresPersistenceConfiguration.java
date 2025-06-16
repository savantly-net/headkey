package ai.headkey.rest.config;

import ai.headkey.memory.implementations.InMemoryContextualCategorizationEngine;
import ai.headkey.memory.implementations.InformationIngestionModuleImpl;
import ai.headkey.memory.implementations.SimplePatternBeliefExtractionService;
import ai.headkey.memory.implementations.StandardBeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.BeliefExtractionService;
import ai.headkey.memory.interfaces.BeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.BeliefRelationshipService;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.memory.interfaces.VectorEmbeddingGenerator;
import ai.headkey.memory.langchain4j.LangChain4JBeliefExtractionService;
import ai.headkey.memory.langchain4j.LangChain4JContextualCategorizationEngine;
import ai.headkey.persistence.factory.JpaMemorySystemFactory;
import ai.headkey.persistence.repositories.BeliefConflictRepository;
import ai.headkey.persistence.repositories.BeliefRelationshipRepository;
import ai.headkey.persistence.repositories.BeliefRepository;
import ai.headkey.persistence.repositories.impl.BeliefRelationshipRepositoryImpl;
import ai.headkey.persistence.repositories.impl.JpaBeliefConflictRepository;
import ai.headkey.persistence.repositories.impl.JpaBeliefRepository;
import ai.headkey.persistence.services.JpaBeliefRelationshipService;
import ai.headkey.persistence.services.JpaBeliefStorageService;
import ai.headkey.persistence.services.JpaMemoryEncodingSystem;
import ai.headkey.persistence.strategies.jpa.DefaultJpaSimilaritySearchStrategy;
import ai.headkey.persistence.strategies.jpa.JpaSimilaritySearchStrategy;
import ai.headkey.persistence.strategies.jpa.JpaSimilaritySearchStrategyFactory;
import ai.headkey.persistence.strategies.jpa.PostgresJpaSimilaritySearchStrategy;
import ai.headkey.persistence.strategies.jpa.TextBasedJpaSimilaritySearchStrategy;
import ai.headkey.rest.service.LangChain4JVectorEmbeddingGenerator;
import ai.headkey.rest.service.QuarkusCategoryExtractionService;
import ai.headkey.rest.service.QuarkusTagExtractionService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

/**
 * PostgreSQL-specific persistence configuration for the Memory System.
 *
 * This configuration class provides CDI beans specifically for PostgreSQL/JPA-based
 * persistence implementations. It's conditionally activated when the persistence
 * type is configured as "postgres".
 *
 * Features:
 * - JPA-based memory encoding system with PostgreSQL optimizations
 * - Vector embedding support with pgvector integration
 * - Automatic similarity search strategy detection
 * - Configurable performance parameters
 * - Database-specific optimizations for PostgreSQL
 *
 * The configuration follows SOLID principles by:
 * - Single responsibility: Only handles PostgreSQL persistence
 * - Open/closed: Extensible without modification
 * - Interface segregation: Uses specific interfaces
 * - Dependency inversion: Depends on abstractions
 */
@ApplicationScoped
@IfBuildProfile("postgres")
public class PostgresPersistenceConfiguration {

    @Inject
    EntityManager entityManager;

    @Inject
    MemorySystemProperties properties;

    @Inject
    @io.quarkus.arc.Unremovable
    jakarta.enterprise.inject.Instance<EmbeddingModel> embeddingModel;

    @Inject
    @io.quarkus.arc.Unremovable
    jakarta.enterprise.inject.Instance<ChatModel> chatModel;

    /**
     * Produces the JPA-based MemoryEncodingSystem when PostgreSQL persistence is configured.
     *
     * This method creates and configures the JPA memory system using the
     * JpaMemorySystemFactory with PostgreSQL-optimized similarity search strategies.
     *
     * @return A singleton instance of JpaMemoryEncodingSystem
     */
    @Produces
    @Singleton
    @PostgresPersistence
    public JpaMemoryEncodingSystem jpaMemoryEncodingSystem() {
        Log.info("Initializing PostgreSQL-based JpaMemoryEncodingSystem");

        try {
            // Create LangChain4J embedding generator
            VectorEmbeddingGenerator embeddingGenerator =
                createLangChain4JEmbeddingGenerator();

            // Create PostgreSQL-optimized similarity search strategy
            JpaSimilaritySearchStrategy strategy =
                createPostgresSimilaritySearchStrategy();
            strategy.initialize(entityManager);

            // Build the memory system with PostgreSQL-specific configuration
            JpaMemoryEncodingSystem memorySystem =
                JpaMemorySystemFactory.builder()
                    .entityManagerFactory(
                        entityManager.getEntityManagerFactory()
                    )
                    .embeddingGenerator(embeddingGenerator)
                    .similarityStrategy(strategy)
                    .batchSize(properties.batchSize())
                    .enableSecondLevelCache(properties.enableSecondLevelCache())
                    .maxSimilaritySearchResults(
                        properties.maxSimilarityResults()
                    )
                    .similarityThreshold(properties.similarityThreshold())
                    .build();

            Log.infof(
                "PostgreSQL JpaMemoryEncodingSystem successfully initialized with strategy: %s",
                strategy.getStrategyName()
            );
            return memorySystem;
        } catch (Exception e) {
            Log.errorf(
                e,
                "Failed to initialize PostgreSQL JpaMemoryEncodingSystem: %s",
                e.getMessage()
            );
            throw new RuntimeException(
                "Unable to initialize PostgreSQL JPA memory system",
                e
            );
        }
    }

    /**
     * Produces the ContextualCategorizationEngine for PostgreSQL persistence.
     *
     * @param categoryService The category extraction service
     * @param tagService The tag extraction service
     * @return A singleton instance of ContextualCategorizationEngine
     */
    @Produces
    @Singleton
    @PostgresPersistence
    public ContextualCategorizationEngine contextualCategorizationEngine(
        QuarkusCategoryExtractionService categoryService,
        QuarkusTagExtractionService tagService
    ) {
        Log.info("Creating PostgreSQL ContextualCategorizationEngine");

        if (chatModel.isUnsatisfied()) {
            Log.info(
                "ChatModel not available, using InMemoryContextualCategorizationEngine"
            );
            return new InMemoryContextualCategorizationEngine();
        }

        Log.info(
            "Creating LangChain4JContextualCategorizationEngine for PostgreSQL"
        );
        return new LangChain4JContextualCategorizationEngine(
            categoryService,
            tagService
        );
    }

    /**
     * Produces the BeliefExtractionService for PostgreSQL persistence.
     *
     * @return A singleton instance of BeliefExtractionService
     */
    @Produces
    @Singleton
    @PostgresPersistence
    public BeliefExtractionService beliefExtractionService() {
        if (chatModel.isUnsatisfied()) {
            Log.info(
                "ChatModel not available, using SimplePatternBeliefExtractionService"
            );
            return new SimplePatternBeliefExtractionService();
        }

        try {
            ChatModel model = chatModel.get();
            Log.infof(
                "Creating PostgreSQL LangChain4JBeliefExtractionService with model: %s",
                model.getClass().getSimpleName()
            );
            return new LangChain4JBeliefExtractionService(model);
        } catch (Exception e) {
            Log.errorf(
                e,
                "Failed to create PostgreSQL LangChain4JBeliefExtractionService, falling back to simple implementation"
            );
            return new SimplePatternBeliefExtractionService();
        }
    }

    /**
     * Produces the BeliefStorageService for PostgreSQL persistence.
     *
     * @return A singleton instance of BeliefStorageService
     */
    @Produces
    @Singleton
    @PostgresPersistence
    public BeliefStorageService beliefStorageService() {
        Log.info("Creating PostgreSQL JpaBeliefStorageService");

        // Create LangChain4J embedding generator
        VectorEmbeddingGenerator embeddingGenerator =
            createLangChain4JEmbeddingGenerator();

        // Create PostgreSQL-optimized repository dependencies
        BeliefRepository beliefRepository = new JpaBeliefRepository(
            entityManager.getEntityManagerFactory(),
            embeddingGenerator
        );
        BeliefConflictRepository conflictRepository =
            new JpaBeliefConflictRepository(
                entityManager.getEntityManagerFactory()
            );

        return new JpaBeliefStorageService(
            beliefRepository,
            conflictRepository
        );
    }

    /**
     * Produces the BeliefReinforcementConflictAnalyzer for PostgreSQL persistence.
     *
     * @param beliefExtractionService The belief extraction service
     * @param beliefStorageService The belief storage service
     * @return A singleton instance of BeliefReinforcementConflictAnalyzer
     */
    @Produces
    @Singleton
    @PostgresPersistence
    public BeliefReinforcementConflictAnalyzer beliefReinforcementConflictAnalyzer(
        @PostgresPersistence BeliefExtractionService beliefExtractionService,
        @PostgresPersistence BeliefStorageService beliefStorageService
    ) {
        Log.infof(
            "Creating PostgreSQL BeliefReinforcementConflictAnalyzer with extraction service: %s",
            beliefExtractionService.getClass().getSimpleName()
        );
        return new StandardBeliefReinforcementConflictAnalyzer(
            beliefExtractionService,
            beliefStorageService
        );
    }

    /**
     * Produces the InformationIngestionModule for PostgreSQL persistence.
     *
     * @param categorizationEngine The categorization engine
     * @param memorySystem The JPA memory encoding system
     * @param beliefAnalyzer The belief analysis system
     * @return A singleton instance of InformationIngestionModule
     */
    @Produces
    @Singleton
    @PostgresPersistence
    public InformationIngestionModule informationIngestionModule(
        @PostgresPersistence ContextualCategorizationEngine categorizationEngine,
        @PostgresPersistence JpaMemoryEncodingSystem memorySystem,
        @PostgresPersistence BeliefReinforcementConflictAnalyzer beliefAnalyzer
    ) {
        Log.info(
            "Creating PostgreSQL InformationIngestionModule using InformationIngestionModuleImpl"
        );
        return new InformationIngestionModuleImpl(
            categorizationEngine,
            memorySystem,
            beliefAnalyzer
        );
    }

    /**
     * Produces the BeliefRelationshipService for PostgreSQL persistence.
     *
     * @return A singleton instance of BeliefRelationshipService
     */
    @Produces
    @Singleton
    @PostgresPersistence
    public BeliefRelationshipService beliefRelationshipService() {
        Log.info("Creating PostgreSQL JpaBeliefRelationshipService");

        // Create PostgreSQL-optimized repository dependencies
        BeliefRepository beliefRepository = new JpaBeliefRepository(
            entityManager.getEntityManagerFactory()
        );
        BeliefRelationshipRepository relationshipRepository =
            new BeliefRelationshipRepositoryImpl(entityManager);

        return new JpaBeliefRelationshipService(
            relationshipRepository,
            beliefRepository
        );
    }

    /**
     * Creates a PostgreSQL-optimized similarity search strategy based on configuration.
     *
     * @return The appropriate PostgreSQL similarity search strategy
     */
    private JpaSimilaritySearchStrategy createPostgresSimilaritySearchStrategy() {
        String strategyName = properties.strategy();
        Log.infof(
            "Creating PostgreSQL similarity search strategy: %s",
            strategyName
        );

        switch (strategyName.toLowerCase()) {
            case "auto":
                // Let the factory auto-detect the best PostgreSQL strategy
                return JpaSimilaritySearchStrategyFactory.createStrategy(
                    entityManager
                );
            case "text":
                return new TextBasedJpaSimilaritySearchStrategy();
            case "vector":
            case "default":
                return new DefaultJpaSimilaritySearchStrategy();
            case "postgres":
                return new PostgresJpaSimilaritySearchStrategy();
            default:
                Log.warnf(
                    "Unknown PostgreSQL strategy '%s', using postgres-optimized strategy",
                    strategyName
                );
                return new PostgresJpaSimilaritySearchStrategy();
        }
    }

    /**
     * Creates a LangChain4J-based vector embedding generator for PostgreSQL.
     *
     * @return A LangChain4J-based vector embedding generator
     */
    private VectorEmbeddingGenerator createLangChain4JEmbeddingGenerator() {
        if (!properties.embedding().enabled()) {
            Log.info("Vector embedding generation disabled by configuration");
            return createFallbackEmbeddingGenerator();
        }

        if (embeddingModel.isUnsatisfied()) {
            Log.warn(
                "No EmbeddingModel bean available for PostgreSQL, falling back to mock generator"
            );
            return createFallbackEmbeddingGenerator();
        }

        EmbeddingModel model = embeddingModel.get();
        Log.infof(
            "Creating PostgreSQL LangChain4J embedding generator with model: %s",
            model.getClass().getSimpleName()
        );

        try {
            return new LangChain4JVectorEmbeddingGenerator(model);
        } catch (Exception e) {
            Log.errorf(
                e,
                "Failed to create PostgreSQL LangChain4J embedding generator, falling back to mock generator"
            );
            return createFallbackEmbeddingGenerator();
        }
    }

    /**
     * Creates a fallback embedding generator for PostgreSQL when LangChain4J is not available.
     *
     * @return A simple mock embedding generator
     */
    private VectorEmbeddingGenerator createFallbackEmbeddingGenerator() {
        Log.warn(
            "Using fallback mock embedding generator for PostgreSQL - not suitable for production"
        );

        return text -> {
            int dimension = properties.embedding().dimension();
            double[] vector = new double[dimension];

            // Simple hash-based embedding for demonstration
            int hash = text.hashCode();
            for (int i = 0; i < dimension; i++) {
                vector[i] = ((hash >> (i % 32)) & 1) == 1 ? 1.0 : 0.0;
            }

            // Normalize the vector
            double norm = 0.0;
            for (double v : vector) {
                norm += v * v;
            }
            norm = Math.sqrt(norm);

            if (norm > 0) {
                for (int i = 0; i < vector.length; i++) {
                    vector[i] /= norm;
                }
            }

            return vector;
        };
    }
}
