package ai.headkey.rest.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * LangChain4J Configuration for Embedding Models.
 * 
 * This configuration class provides CDI beans for LangChain4J embedding models,
 * following the 12-factor app principles by externalizing configuration and
 * providing graceful degradation when API keys are not available.
 * 
 * The configuration supports multiple embedding providers through LangChain4J:
 * - OpenAI (primary, production-ready)
 * - Local models (future extensibility)
 * - Mock models (development/testing)
 * 
 * Features:
 * - Automatic fallback to mock models when API keys are missing
 * - Environment-specific configuration
 * - Proper error handling and logging
 * - Configurable timeouts and retry logic
 * 
 * @author HeadKey Development Team
 * @since 1.0.0
 */
@ApplicationScoped
public class LangChain4JConfig {
    
    private static final Logger LOG = Logger.getLogger(LangChain4JConfig.class);
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.api-key")
    Optional<String> openAiApiKey;
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.base-url", defaultValue = "https://api.openai.com/v1")
    String openAiBaseUrl;
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.embedding-model.model-name", defaultValue = "text-embedding-3-small")
    String embeddingModelName;
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.embedding-model.dimensions", defaultValue = "1536")
    Integer embeddingDimensions;
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.timeout", defaultValue = "60s")
    Duration timeout;
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.max-retries", defaultValue = "3")
    Integer maxRetries;
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.log-requests", defaultValue = "false")
    Boolean logRequests;
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.log-responses", defaultValue = "false")
    Boolean logResponses;
    
    @Inject
    MemorySystemProperties memoryProperties;
    
    /**
     * Produces the EmbeddingModel as a CDI bean.
     * 
     * This method creates the appropriate embedding model based on configuration:
     * 1. OpenAI model if API key is available
     * 2. Mock model for development/testing when API key is missing
     * 
     * The method handles graceful degradation and provides detailed logging
     * about which model is being used and why.
     * 
     * @return A singleton instance of EmbeddingModel
     */
    @Produces
    @Singleton
    public EmbeddingModel embeddingModel() {
        LOG.info("Initializing EmbeddingModel for CDI");
        
        if (openAiApiKey.isPresent() && !openAiApiKey.get().trim().isEmpty()) {
            return createOpenAiEmbeddingModel();
        } else {
            LOG.warn("OpenAI API key not configured, using mock embedding model. " +
                    "Set OPENAI_API_KEY environment variable for production use.");
            return createMockEmbeddingModel();
        }
    }
    
    /**
     * Creates an OpenAI embedding model with full configuration.
     * 
     * @return Configured OpenAI embedding model
     */
    private EmbeddingModel createOpenAiEmbeddingModel() {
        LOG.infof("Creating OpenAI embedding model: %s (dimensions: %d)", 
                 embeddingModelName, embeddingDimensions);
        
        try {
            OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                    .apiKey(openAiApiKey.get())
                    .baseUrl(openAiBaseUrl)
                    .modelName(embeddingModelName)
                    .timeout(timeout)
                    .maxRetries(maxRetries)
                    .logRequests(logRequests)
                    .logResponses(logResponses);
            
            // Only set dimensions if the model supports it
            if (supportsCustomDimensions(embeddingModelName)) {
                builder.dimensions(embeddingDimensions);
            }
            
            EmbeddingModel model = builder.build();
            
            LOG.infof("Successfully created OpenAI embedding model: %s", embeddingModelName);
            return model;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to create OpenAI embedding model, falling back to mock model: %s", 
                      e.getMessage());
            return createMockEmbeddingModel();
        }
    }
    
    /**
     * Creates a mock embedding model for development and testing.
     * 
     * The mock model generates deterministic embeddings based on text hash,
     * which is useful for testing and development when API keys are not available.
     * 
     * @return Mock embedding model
     */
    private EmbeddingModel createMockEmbeddingModel() {
        LOG.info("Creating mock embedding model for development/testing");
        
        int dimension = memoryProperties.embedding().dimension();
        
        return new MockEmbeddingModel(dimension);
    }
    
    /**
     * Checks if the given model supports custom dimensions.
     * 
     * @param modelName The model name to check
     * @return true if the model supports custom dimensions
     */
    private boolean supportsCustomDimensions(String modelName) {
        return modelName != null && (
            modelName.contains("text-embedding-3-small") ||
            modelName.contains("text-embedding-3-large")
        );
    }
    
    /**
     * Simple mock embedding model for development and testing.
     * 
     * This implementation generates deterministic embeddings based on
     * the hash of the input text, making it suitable for testing
     * while maintaining consistency across runs.
     */
    public static class MockEmbeddingModel implements EmbeddingModel {
        
        private final int dimension;
        
        public MockEmbeddingModel(int dimension) {
            this.dimension = Math.max(1, dimension);
        }
        
        @Override
        public Response<Embedding> embed(String text) {
            if (text == null || text.trim().isEmpty()) {
                Embedding embedding = Embedding.from(new float[dimension]);
                return Response.from(embedding);
            }
            
            // Generate deterministic embedding based on text hash
            int hash = text.hashCode();
            float[] vector = new float[dimension];
            
            for (int i = 0; i < dimension; i++) {
                // Use different bit patterns to create variety in the vector
                int bitShift = (i * 7) % 32;
                vector[i] = ((hash >> bitShift) & 1) == 1 ? 1.0f : -1.0f;
                
                // Add some variance based on position and text length
                vector[i] *= (1.0f + (text.length() % 10) * 0.1f) * (1.0f + i * 0.01f);
            }
            
            // Normalize the vector
            float norm = 0.0f;
            for (float v : vector) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);
            
            if (norm > 0) {
                for (int i = 0; i < vector.length; i++) {
                    vector[i] /= norm;
                }
            }
            
            Embedding embedding = Embedding.from(vector);
            return Response.from(embedding);
        }

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
            // Simple implementation that embeds each segment individually
            List<Embedding> embeddings = textSegments.stream()
                    .map(segment -> embed(segment.text()).content())
                    .toList();
            return Response.from(embeddings);
        }
        
        @Override
        public String toString() {
            return String.format("MockEmbeddingModel{dimension=%d}", dimension);
        }
    }
}