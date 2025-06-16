package ai.headkey.rest.config;

import ai.headkey.memory.implementations.InMemoryBeliefRelationshipService;
import ai.headkey.memory.implementations.InMemoryContextualCategorizationEngine;
import ai.headkey.memory.implementations.InMemoryMemoryEncodingSystem;
import ai.headkey.memory.implementations.InformationIngestionModuleImpl;
import ai.headkey.memory.implementations.SimplePatternBeliefExtractionService;
import ai.headkey.memory.implementations.StandardBeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.BeliefExtractionService;
import ai.headkey.memory.interfaces.BeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.BeliefRelationshipService;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.memory.langchain4j.LangChain4JBeliefExtractionService;
import ai.headkey.memory.langchain4j.LangChain4JContextualCategorizationEngine;
import ai.headkey.persistence.elastic.configuration.ElasticsearchConfiguration;
import ai.headkey.persistence.elastic.services.ElasticsearchBeliefRelationshipService;
import ai.headkey.persistence.elastic.services.ElasticsearchBeliefStorageService;
import ai.headkey.persistence.elastic.services.ElasticsearchMemoryEncodingSystem;
import ai.headkey.rest.config.MemorySystemProperties.ElasticsearchConfig;
import ai.headkey.rest.service.LangChain4JVectorEmbeddingGenerator;
import ai.headkey.rest.service.QuarkusCategoryExtractionService;
import ai.headkey.rest.service.QuarkusTagExtractionService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Elasticsearch-specific persistence configuration for the Memory System.
 *
 * This configuration class provides CDI beans specifically for
 * Elasticsearch-based
 * persistence implementations. It's conditionally activated when the
 * persistence
 * type is configured as "elasticsearch".
 *
 * Features:
 * - Elasticsearch-based memory encoding system with full-text search
 * - Vector similarity search using dense_vector fields
 * - Distributed search and storage capabilities
 * - Automatic index management and mapping creation
 * - Configurable performance parameters
 * - Multi-tenant support through index isolation
 *
 * The configuration follows SOLID principles by:
 * - Single responsibility: Only handles Elasticsearch persistence
 * - Open/closed: Extensible without modification
 * - Interface segregation: Uses specific interfaces
 * - Dependency inversion: Depends on abstractions
 */
@ApplicationScoped
public class ElasticsearchPersistenceConfiguration {

    @Inject
    MemorySystemProperties properties;

    @Inject
    @io.quarkus.arc.Unremovable
    jakarta.enterprise.inject.Instance<EmbeddingModel> embeddingModel;

    @Inject
    @io.quarkus.arc.Unremovable
    jakarta.enterprise.inject.Instance<ChatModel> chatModel;

    @Produces
    @Singleton
    @ElasticsearchPersistence
    public ElasticsearchConfiguration elasticsearchConfiguration() {
        var esConnectionConfig = new ElasticsearchConfiguration.ElasticsearchConnectionConfig.Builder()
                .authentication(properties.elasticsearch().username(), properties.elasticsearch().password())
                .host(properties.elasticsearch().host(), properties.elasticsearch().port(),
                        properties.elasticsearch().scheme())
                .build();

        return ElasticsearchConfiguration.create(esConnectionConfig);
    }

    @Produces
    @Singleton
    @ElasticsearchPersistence
    public MemoryEncodingSystem memoryEncodingSystem(
        @ElasticsearchPersistence ElasticsearchConfiguration config,
        LangChain4JVectorEmbeddingGenerator embeddingGenerator
    ) {
        Log.info("Creating Elasticsearch MemoryEncodingSystem");
        if (chatModel.isUnsatisfied()) {
            Log.info("ChatModel not available, using InMemoryMemoryEncodingSystem");
            return new InMemoryMemoryEncodingSystem();
        }
        Log.info("Creating ElasticsearchMemoryEncodingSystem with LangChain4J embedding generator");
        
        int batchSize = properties.batchSize();
        boolean autoCreateIndices = properties.elasticsearch().autoCreateIndices();
        int searchTimeout = properties.elasticsearch().searchTimeoutMs();
        int maxSimilarityResults = properties.maxSimilarityResults();
        double similarityThreshold = properties.similarityThreshold();
        Log.infof("Elasticsearch MemoryEncodingSystem configuration: batchSize=%d, autoCreateIndices=%b, searchTimeout=%dms, maxSimilarityResults=%d, similarityThreshold=%.2f",
            batchSize, autoCreateIndices, searchTimeout, maxSimilarityResults, similarityThreshold);
        return new ElasticsearchMemoryEncodingSystem(config, embeddingGenerator, batchSize, autoCreateIndices, searchTimeout, maxSimilarityResults, similarityThreshold);
    }

    /**
     * Produces the ContextualCategorizationEngine for Elasticsearch persistence.
     *
     * @param categoryService The category extraction service
     * @param tagService      The tag extraction service
     * @return A singleton instance of ContextualCategorizationEngine
     */
    @Produces
    @Singleton
    @ElasticsearchPersistence
    public ContextualCategorizationEngine contextualCategorizationEngine(
            QuarkusCategoryExtractionService categoryService,
            QuarkusTagExtractionService tagService) {
        Log.info("Creating Elasticsearch ContextualCategorizationEngine");

        if (chatModel.isUnsatisfied()) {
            Log.info(
                    "ChatModel not available, using InMemoryContextualCategorizationEngine");
            return new InMemoryContextualCategorizationEngine();
        }

        Log.info(
                "Creating LangChain4JContextualCategorizationEngine for Elasticsearch");
        return new LangChain4JContextualCategorizationEngine(
                categoryService,
                tagService);
    }

    /**
     * Produces the BeliefExtractionService for Elasticsearch persistence.
     *
     * @return A singleton instance of BeliefExtractionService
     */
    @Produces
    @Singleton
    @ElasticsearchPersistence
    public BeliefExtractionService beliefExtractionService() {
        if (chatModel.isUnsatisfied()) {
            Log.warn(
                    "ChatModel not available, using SimplePatternBeliefExtractionService");
            return new SimplePatternBeliefExtractionService();
        }

        try {
            ChatModel model = chatModel.get();
            Log.infof(
                    "Creating Elasticsearch LangChain4JBeliefExtractionService with model: %s",
                    model.getClass().getSimpleName());
            return new LangChain4JBeliefExtractionService(model);
        } catch (Exception e) {
            Log.errorf(
                    e,
                    "Failed to create Elasticsearch LangChain4JBeliefExtractionService, falling back to simple implementation");
            return new SimplePatternBeliefExtractionService();
        }
    }

    @Produces
    @Singleton
    @ElasticsearchPersistence
    public ElasticsearchBeliefStorageService beliefStorageService(
            @ElasticsearchPersistence ElasticsearchConfiguration config,
            LangChain4JVectorEmbeddingGenerator embeddingGenerator) {

        Log.info("Creating Elasticsearch BeliefStorageService");

        int batchSize = properties.batchSize();
        boolean autoCreateIndices = properties.elasticsearch().autoCreateIndices();
        int searchTimeout = properties.elasticsearch().searchTimeoutMs();
        int maxResults = properties.maxSimilarityResults();
        double similarityThreshold = properties.similarityThreshold();

        return new ElasticsearchBeliefStorageService(config, embeddingGenerator, batchSize, autoCreateIndices,
                searchTimeout, maxResults, similarityThreshold);
    }

    /**
     * Produces the BeliefReinforcementConflictAnalyzer for Elasticsearch
     * persistence.
     *
     * @param beliefExtractionService The belief extraction service
     * @param beliefStorageService    The belief storage service
     * @return A singleton instance of BeliefReinforcementConflictAnalyzer
     */
    @Produces
    @Singleton
    @ElasticsearchPersistence
    public BeliefReinforcementConflictAnalyzer beliefReinforcementConflictAnalyzer(
            @ElasticsearchPersistence BeliefExtractionService beliefExtractionService,
            @ElasticsearchPersistence BeliefStorageService beliefStorageService) {
        Log.infof(
                "Creating Elasticsearch BeliefReinforcementConflictAnalyzer with extraction service: %s",
                beliefExtractionService.getClass().getSimpleName());
        return new StandardBeliefReinforcementConflictAnalyzer(
                beliefExtractionService,
                beliefStorageService);
    }

    /**
     * Produces the InformationIngestionModule for Elasticsearch persistence.
     *
     * @param categorizationEngine The categorization engine
     * @param memorySystem         The Elasticsearch memory encoding system
     * @param beliefAnalyzer       The belief analysis system
     * @return A singleton instance of InformationIngestionModule
     */
    @Produces
    @Singleton
    @ElasticsearchPersistence
    public InformationIngestionModule informationIngestionModule(
            @ElasticsearchPersistence ContextualCategorizationEngine categorizationEngine,
            @ElasticsearchPersistence MemoryEncodingSystem memorySystem,
            @ElasticsearchPersistence BeliefReinforcementConflictAnalyzer beliefAnalyzer) {
        Log.info(
                "Creating Elasticsearch InformationIngestionModule using InformationIngestionModuleImpl");
        return new InformationIngestionModuleImpl(
                categorizationEngine,
                memorySystem,
                beliefAnalyzer);
    }

    @Produces
    @Singleton
    @ElasticsearchPersistence
    public BeliefRelationshipService beliefRelationshipService(
            @ElasticsearchPersistence ElasticsearchBeliefStorageService beliefStorageService) {
        Log.info("Creating Elasticsearch BeliefRelationshipService");

        if (chatModel.isUnsatisfied()) {
            Log.warn(
                    "ChatModel not available, using InMemoryBeliefRelationshipService");
            return new InMemoryBeliefRelationshipService();
        }

        Log.infof(
                "Creating ElasticsearchBeliefRelationshipService with chat model: %s",
                chatModel.get().getClass().getSimpleName());
        int batchSize = properties.batchSize();
        boolean autoCreateIndices = properties.elasticsearch().autoCreateIndices();
        int searchTimeout = properties.elasticsearch().searchTimeoutMs();
        int maxResults = properties.maxSimilarityResults();
        return new ElasticsearchBeliefRelationshipService(beliefStorageService, batchSize, autoCreateIndices,
                searchTimeout, maxResults);
    }

}
