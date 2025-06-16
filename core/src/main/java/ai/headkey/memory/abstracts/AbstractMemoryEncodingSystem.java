package ai.headkey.memory.abstracts;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.exceptions.MemoryNotFoundException;
import ai.headkey.memory.exceptions.StorageException;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.memory.interfaces.VectorEmbeddingGenerator;

/**
 * Abstract base class for MemoryEncodingSystem implementations.
 * 
 * This class provides common functionality shared across different persistence
 * implementations, including:
 * - Statistics tracking and health monitoring
 * - Vector embedding generation and similarity search logic
 * - Input validation and error handling
 * - Common utility methods
 * 
 * Concrete implementations need to focus only on persistence-specific operations
 * while inheriting all the common memory system behaviors.
 * 
 * @since 1.0
 */
public abstract class AbstractMemoryEncodingSystem implements MemoryEncodingSystem {

    private static final Logger LOGGER = Logger.getLogger(AbstractMemoryEncodingSystem.class.getName());
    
    // Statistics tracking
    protected final AtomicLong totalOperations = new AtomicLong(0);
    protected final AtomicLong totalSearches = new AtomicLong(0);
    protected final AtomicLong totalUpdates = new AtomicLong(0);
    protected final AtomicLong totalDeletes = new AtomicLong(0);
    protected final Instant startTime;
    
    // Vector embedding configuration
    protected VectorEmbeddingGenerator embeddingGenerator;
    protected SimilarityMetric similarityMetric = SimilarityMetric.COSINE;
    
    
    /**
     * Enum for different similarity metrics.
     */
    public enum SimilarityMetric {
        COSINE, EUCLIDEAN, DOT_PRODUCT
    }
    
    /**
     * Constructor for abstract base class.
     */
    protected AbstractMemoryEncodingSystem() {
        this.startTime = Instant.now();
    }
    
    /**
     * Constructor with embedding generator.
     * 
     * @param embeddingGenerator Function to generate vector embeddings
     */
    protected AbstractMemoryEncodingSystem(VectorEmbeddingGenerator embeddingGenerator) {
        this();
        this.embeddingGenerator = embeddingGenerator;
    }
    
    // Template method pattern for encoding and storage
    @Override
    public final MemoryRecord encodeAndStore(String content, CategoryLabel category, Metadata meta, String agentId) {
        validateEncodeAndStoreInputs(content, category, meta);
        
        try {
            totalOperations.incrementAndGet();
            
            // Generate embedding if generator is available
            double[] embedding = null;
            if (embeddingGenerator != null) {
                embedding = embeddingGenerator.generateEmbedding(content);
            }
            
            // Delegate to concrete implementation
            return doEncodeAndStore(content, category, meta, agentId, embedding);
            
        } catch (Exception e) {
            // Log the original exception for debugging
            System.err.println("Detailed error in encodeAndStore: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
            e.printStackTrace();
            throw new StorageException("Failed to encode and store memory: " + e.getMessage(), e);
        }
    }
    
    @Override
    public final Optional<MemoryRecord> getMemory(String memoryId) {
        validateMemoryId(memoryId);
        
        try {
            Optional<MemoryRecord> result = doGetMemory(memoryId);
            result.ifPresent(this::updateAccessStatistics);
            return result;
        } catch (Exception e) {
            System.err.println("Detailed error in getMemory for ID " + memoryId + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            throw new StorageException("Failed to retrieve memory: " + memoryId + " - " + e.getMessage(), e);
        }
    }
    
    @Override
    public final Map<String, MemoryRecord> getMemories(Set<String> memoryIds) {
        validateMemoryIds(memoryIds);
        
        try {
            Map<String, MemoryRecord> results = doGetMemories(memoryIds);
            results.values().forEach(this::updateAccessStatistics);
            return results;
        } catch (Exception e) {
            System.err.println("Detailed error in getMemories: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            throw new StorageException("Failed to retrieve memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public final MemoryRecord updateMemory(MemoryRecord memoryRecord) {
        validateMemoryRecord(memoryRecord);
        
        try {
            totalUpdates.incrementAndGet();
            
            // Generate new embedding if content changed and generator is available
            double[] embedding = null;
            if (embeddingGenerator != null) {
                embedding = embeddingGenerator.generateEmbedding(memoryRecord.getContent());
            }
            
            return doUpdateMemory(memoryRecord, embedding);
            
        } catch (MemoryNotFoundException e) {
            System.err.println("Memory not found for update: " + memoryRecord.getId());
            throw e; // Re-throw specific exception
        } catch (Exception e) {
            System.err.println("Detailed error in updateMemory for ID " + memoryRecord.getId() + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            throw new StorageException("Failed to update memory: " + memoryRecord.getId() + " - " + e.getMessage(), e);
        }
    }
    
    @Override
    public final boolean removeMemory(String memoryId) {
        validateMemoryId(memoryId);
        
        try {
            totalDeletes.incrementAndGet();
            return doRemoveMemory(memoryId);
        } catch (Exception e) {
            System.err.println("Detailed error in removeMemory for ID " + memoryId + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            throw new StorageException("Failed to remove memory: " + memoryId + " - " + e.getMessage(), e);
        }
    }
    
    @Override
    public final Set<String> removeMemories(Set<String> memoryIds) {
        validateMemoryIds(memoryIds);
        
        try {
            totalDeletes.addAndGet(memoryIds.size());
            return doRemoveMemories(memoryIds);
        } catch (Exception e) {
            System.err.println("Detailed error in removeMemories: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            throw new StorageException("Failed to remove memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public final List<MemoryRecord> searchSimilar(String queryContent, int limit, String agentId) {
        validateSearchInputs(queryContent, limit);
        
        try {
            totalSearches.incrementAndGet();
            
            // Generate query embedding if generator is available
            double[] queryEmbedding = null;
            if (embeddingGenerator != null) {
                queryEmbedding = embeddingGenerator.generateEmbedding(queryContent);
            }
            
            List<MemoryRecord> results = doSearchSimilar(queryContent, queryEmbedding, limit, agentId);
            results.forEach(this::updateAccessStatistics);
            return results;
            
        } catch (Exception e) {
            System.err.println("Detailed error in searchSimilar: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            throw new StorageException("Failed to search similar memories: " + e.getMessage(), e);
        }
    }
    
    // Delegate methods for concrete implementations to override
    
    /**
     * Concrete implementation of memory encoding and storage.
     * 
     * @param content The content to store
     * @param category The category label
     * @param meta The metadata
     * @param embedding The generated vector embedding (may be null)
     * @return The stored MemoryRecord
     */
    protected abstract MemoryRecord doEncodeAndStore(String content, CategoryLabel category, 
                                                    Metadata meta, String agentId, double[] embedding);
    
    /**
     * Concrete implementation of memory retrieval by ID.
     * 
     * @param memoryId The memory identifier
     * @return Optional containing the MemoryRecord if found
     */
    protected abstract Optional<MemoryRecord> doGetMemory(String memoryId);
    
    /**
     * Concrete implementation of bulk memory retrieval.
     * 
     * @param memoryIds The memory identifiers
     * @return Map of found memories
     */
    protected abstract Map<String, MemoryRecord> doGetMemories(Set<String> memoryIds);
    
    /**
     * Concrete implementation of memory update.
     * 
     * @param memoryRecord The memory record to update
     * @param embedding The new vector embedding (may be null)
     * @return The updated MemoryRecord
     */
    protected abstract MemoryRecord doUpdateMemory(MemoryRecord memoryRecord, double[] embedding);
    
    /**
     * Concrete implementation of memory removal.
     * 
     * @param memoryId The memory identifier
     * @return true if removed successfully
     */
    protected abstract boolean doRemoveMemory(String memoryId);
    
    /**
     * Concrete implementation of bulk memory removal.
     * 
     * @param memoryIds The memory identifiers
     * @return Set of successfully removed memory IDs
     */
    protected abstract Set<String> doRemoveMemories(Set<String> memoryIds);
    
    /**
     * Concrete implementation of similarity search.
     * 
     * @param queryContent The search query content
     * @param queryEmbedding The query vector embedding (may be null)
     * @param limit Maximum results to return
     * @return List of similar memories
     */
    protected abstract List<MemoryRecord> doSearchSimilar(String queryContent, 
                                                         double[] queryEmbedding, int limit, String agentId);
    
    // Common utility methods
    
    /**
     * Calculates cosine similarity between two vectors.
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Cosine similarity score (0.0 to 1.0)
     */
    protected double calculateCosineSimilarity(double[] vector1, double[] vector2) {
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
    
    /**
     * Calculates Euclidean distance between two vectors.
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Euclidean distance
     */
    protected double calculateEuclideanDistance(double[] vector1, double[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length) {
            return Double.MAX_VALUE;
        }
        
        double sum = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            double diff = vector1[i] - vector2[i];
            sum += diff * diff;
        }
        
        return Math.sqrt(sum);
    }
    
    /**
     * Updates access statistics for a memory record.
     * 
     * @param memoryRecord The accessed memory record
     */
    protected void updateAccessStatistics(MemoryRecord memoryRecord) {
        if (memoryRecord != null) {
            memoryRecord.updateLastAccessed();
        }
    }
    
    /**
     * Generates a unique memory ID.
     * 
     * @return A unique identifier string
     */
    protected String generateMemoryId() {
        return "mem_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    // Default implementations for statistics and health methods
    
    @Override
    public Map<String, Object> getStorageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOperations", totalOperations.get());
        stats.put("totalSearches", totalSearches.get());
        stats.put("totalUpdates", totalUpdates.get());
        stats.put("totalDeletes", totalDeletes.get());
        stats.put("startTime", startTime);
        stats.put("uptimeSeconds", Instant.now().getEpochSecond() - startTime.getEpochSecond());
        
        // Add implementation-specific statistics
        addImplementationSpecificStatistics(stats);
        
        return stats;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Basic health checks
            if (totalOperations.get() < 0) return false;
            
            // Delegate to implementation-specific health check
            return doHealthCheck();
            
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getCapacityInfo() {
        Map<String, Object> capacity = new HashMap<>();
        
        // Default capacity information
        capacity.put("unlimited", true);
        capacity.put("currentCount", getCurrentMemoryCount());
        
        // Add implementation-specific capacity info
        addImplementationSpecificCapacityInfo(capacity);
        
        return capacity;
    }
    
    // Methods for concrete implementations to override for customization
    
    /**
     * Adds implementation-specific statistics to the stats map.
     * 
     * @param stats The statistics map to add to
     */
    protected void addImplementationSpecificStatistics(Map<String, Object> stats) {
        // Default: no additional statistics
    }
    
    /**
     * Performs implementation-specific health checks.
     * 
     * @return true if healthy
     */
    protected boolean doHealthCheck() {
        return true; // Default: assume healthy
    }
    
    /**
     * Gets the current count of stored memories.
     * 
     * @return Number of stored memories
     */
    protected abstract long getCurrentMemoryCount();
    
    /**
     * Adds implementation-specific capacity information.
     * 
     * @param capacity The capacity map to add to
     */
    protected void addImplementationSpecificCapacityInfo(Map<String, Object> capacity) {
        // Default: no additional capacity info
    }
    
    // Validation methods
    
    private void validateEncodeAndStoreInputs(String content, CategoryLabel category, Metadata meta) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        if (category == null) {
            LOGGER.warning("Category cannot be null");
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (meta == null) {
            LOGGER.warning("Metadata cannot be null");
            throw new IllegalArgumentException("Metadata cannot be null");
        }
    }
    
    private void validateMemoryId(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Memory ID cannot be null or empty");
        }
    }
    
    private void validateMemoryIds(Set<String> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            throw new IllegalArgumentException("Memory IDs cannot be null or empty");
        }
        for (String id : memoryIds) {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Memory ID cannot be null or empty");
            }
        }
    }
    
    private void validateMemoryRecord(MemoryRecord memoryRecord) {
        if (memoryRecord == null) {
            throw new IllegalArgumentException("Memory record cannot be null");
        }
        if (memoryRecord.getId() == null || memoryRecord.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Memory record ID cannot be null or empty");
        }
        if (memoryRecord.getContent() == null || memoryRecord.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Memory record content cannot be null or empty");
        }
    }
    
    private void validateSearchInputs(String queryContent, int limit) {
        if (queryContent == null || queryContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Query content cannot be null or empty");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
    }
    
    // Getters and setters for configuration
    
    public VectorEmbeddingGenerator getEmbeddingGenerator() {
        return embeddingGenerator;
    }
    
    public void setEmbeddingGenerator(VectorEmbeddingGenerator embeddingGenerator) {
        this.embeddingGenerator = embeddingGenerator;
    }
    
    public SimilarityMetric getSimilarityMetric() {
        return similarityMetric;
    }
    
    public void setSimilarityMetric(SimilarityMetric similarityMetric) {
        this.similarityMetric = similarityMetric;
    }
}