package ai.headkey.rest.service;

import java.util.Objects;

import org.jboss.logging.Logger;

import ai.headkey.memory.interfaces.VectorEmbeddingGenerator;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * LangChain4J implementation of VectorEmbeddingGenerator.
 * 
 * This class integrates the HeadKey memory system with LangChain4J's
 * embedding models, providing a bridge between the two systems.
 * 
 * The implementation follows SOLID principles:
 * - Single Responsibility: Only handles embedding generation using LangChain4J
 * - Open/Closed: Can be extended for different embedding models
 * - Liskov Substitution: Fully implements VectorEmbeddingGenerator contract
 * - Interface Segregation: Uses focused interfaces
 * - Dependency Inversion: Depends on abstractions (EmbeddingModel interface)
 * 
 * Features:
 * - Robust error handling with detailed logging
 * - Input validation for reliability
 * - Automatic type conversion between float[] and double[]
 * - CDI integration for dependency injection
 * - Performance monitoring and logging
 * 
 * @author HeadKey Development Team
 * @since 1.0.0
 */
@ApplicationScoped
public class LangChain4JVectorEmbeddingGenerator implements VectorEmbeddingGenerator {
    
    private static final Logger LOG = Logger.getLogger(LangChain4JVectorEmbeddingGenerator.class);
    
    private final EmbeddingModel embeddingModel;
    
    /**
     * Constructor for CDI injection.
     * 
     * @param embeddingModel The LangChain4J embedding model to use
     * @throws IllegalArgumentException if embeddingModel is null
     */
    @Inject
    public LangChain4JVectorEmbeddingGenerator(EmbeddingModel embeddingModel) {
        this.embeddingModel = Objects.requireNonNull(embeddingModel, 
            "EmbeddingModel cannot be null");
        
        LOG.infof("Initialized LangChain4JVectorEmbeddingGenerator with model: %s", 
                 embeddingModel.getClass().getSimpleName());
    }
    
    /**
     * Generates a vector embedding for the given text content using LangChain4J.
     * 
     * This method handles the conversion between LangChain4J's Embedding format
     * (float[]) and the HeadKey system's expected format (double[]).
     * 
     * @param content The text content to generate embeddings for
     * @return A double array representing the vector embedding
     * @throws IllegalArgumentException if content is null
     * @throws Exception if embedding generation fails
     */
    @Override
    public double[] generateEmbedding(String content) throws Exception {
        if (content == null) {
            LOG.warn("Attempted to generate embedding for null content");
            throw new IllegalArgumentException("Content cannot be null");
        }
        
        try {
            LOG.debugf("Generating embedding for content of length: %d", content.length());
            
            long startTime = System.currentTimeMillis();
            
            // Generate embedding using LangChain4J
            Response<Embedding> response = embeddingModel.embed(content);
            
            if (response == null) {
                LOG.error("EmbeddingModel returned null response");
                throw new Exception("EmbeddingModel returned null response");
            }
            
            Embedding embedding = response.content();
            
            long endTime = System.currentTimeMillis();
            LOG.debugf("Embedding generation completed in %d ms", (endTime - startTime));
            
            if (embedding == null) {
                LOG.error("EmbeddingModel returned null embedding");
                throw new Exception("EmbeddingModel returned null embedding");
            }
            
            // Convert from float[] to double[]
            float[] floatVector = embedding.vector();
            if (floatVector == null) {
                LOG.error("Embedding contains null vector");
                throw new Exception("Embedding contains null vector");
            }
            
            double[] doubleVector = new double[floatVector.length];
            for (int i = 0; i < floatVector.length; i++) {
                doubleVector[i] = floatVector[i];
            }
            
            LOG.debugf("Successfully generated embedding with dimension: %d", doubleVector.length);
            return doubleVector;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to generate embedding for content: %s", 
                      content.length() > 100 ? content.substring(0, 100) + "..." : content);
            throw new Exception("Failed to generate embedding", e);
        }
    }
    
    /**
     * Gets the underlying LangChain4J embedding model.
     * 
     * This method is provided for testing and advanced configuration scenarios.
     * 
     * @return The LangChain4J embedding model instance
     */
    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
    
    /**
     * Provides a string representation of this generator.
     * 
     * @return A string describing this embedding generator
     */
    @Override
    public String toString() {
        return String.format("LangChain4JVectorEmbeddingGenerator{embeddingModel=%s}", 
                           embeddingModel.toString());
    }
    
    /**
     * Checks if this generator is equal to another object.
     * 
     * @param obj The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LangChain4JVectorEmbeddingGenerator that = (LangChain4JVectorEmbeddingGenerator) obj;
        return Objects.equals(embeddingModel, that.embeddingModel);
    }
    
    /**
     * Returns the hash code for this generator.
     * 
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(embeddingModel);
    }
}