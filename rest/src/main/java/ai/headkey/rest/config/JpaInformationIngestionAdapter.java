package ai.headkey.rest.config;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.IngestionResult;
import ai.headkey.memory.dto.MemoryInput;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.exceptions.InvalidInputException;
import ai.headkey.memory.exceptions.StorageException;
import ai.headkey.persistence.services.JpaMemoryEncodingSystem;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter that bridges the InformationIngestionModule interface with JpaMemoryEncodingSystem.
 * 
 * This adapter provides a complete implementation of the InformationIngestionModule
 * interface by delegating to the JpaMemoryEncodingSystem and adding the necessary
 * ingestion logic such as validation, categorization, and result processing.
 * 
 * The adapter handles:
 * - Input validation and sanitization
 * - Basic categorization (can be enhanced with ML models)
 * - Memory encoding and storage through JPA
 * - Dry run simulation
 * - Statistics and health monitoring
 * 
 * This is a transitional implementation that allows the REST API to work with
 * the new JPA-based memory system while maintaining the InformationIngestionModule
 * interface contract.
 */
public class JpaInformationIngestionAdapter implements InformationIngestionModule {
    
    private static final Logger LOG = Logger.getLogger(JpaInformationIngestionAdapter.class);
    
    private final JpaMemoryEncodingSystem memorySystem;
    private final Instant startTime;
    
    // Statistics tracking
    private long totalIngestions = 0;
    private long totalDryRuns = 0;
    private long totalValidations = 0;
    private long totalErrors = 0;
    
    /**
     * Creates a new adapter wrapping the provided JPA memory system.
     * 
     * @param memorySystem The JPA memory encoding system to delegate to
     */
    public JpaInformationIngestionAdapter(JpaMemoryEncodingSystem memorySystem) {
        this.memorySystem = memorySystem;
        this.startTime = Instant.now();
        LOG.info("JpaInformationIngestionAdapter initialized");
    }
    
    @Override
    public IngestionResult ingest(MemoryInput input) {
        LOG.debugf("Starting ingestion for agent: %s", input.getAgentId());
        Instant processingStart = Instant.now();
        
        try {
            // Validate input
            validateInput(input);
            
            // Create category (basic implementation)
            CategoryLabel category = createCategory(input);
            LOG.debugf("Created category: %s", category);
            
            // Create metadata
            Metadata metadata = createMetadata(input);
            LOG.debugf("Created metadata: %s", metadata);
            
            // Validate DTO serialization before storing
            validateDtoSerialization(category, metadata);
            
            // Store the memory
            MemoryRecord stored = memorySystem.encodeAndStore(input.getContent(), category, metadata);
            
            // Create successful result
            IngestionResult result = IngestionResult.success(stored.getId(), category);
            result.setAgentId(input.getAgentId());
            result.setProcessingTimeMs(System.currentTimeMillis() - processingStart.toEpochMilli());
            
            totalIngestions++;
            LOG.infof("Successfully ingested memory with ID: %s for agent: %s", 
                     stored.getId(), input.getAgentId());
            
            return result;
            
        } catch (Exception e) {
            totalErrors++;
            LOG.errorf(e, "Failed to ingest memory for agent: %s", input.getAgentId());
            
            // Print detailed error information
            System.err.println("DETAILED ERROR in JpaInformationIngestionAdapter.ingest:");
            System.err.println("Agent ID: " + input.getAgentId());
            System.err.println("Content length: " + (input.getContent() != null ? input.getContent().length() : "null"));
            System.err.println("Exception type: " + e.getClass().getSimpleName());
            System.err.println("Exception message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
            e.printStackTrace();
            
            IngestionResult result = IngestionResult.failure("Ingestion failed: " + e.getMessage());
            result.setAgentId(input.getAgentId());
            result.setProcessingTimeMs(System.currentTimeMillis() - processingStart.toEpochMilli());
            return result;
        }
    }
    
    @Override
    public IngestionResult dryRunIngest(MemoryInput input) {
        LOG.debugf("Starting dry run for agent: %s", input.getAgentId());
        Instant processingStart = Instant.now();
        
        try {
            // Validate input
            validateInput(input);
            
            // Create category (same as real ingestion)
            CategoryLabel category = createCategory(input);
            
            // Create metadata
            Metadata metadata = createMetadata(input);
            
            // Generate a mock memory ID for dry run
            String mockMemoryId = "dry-run-" + UUID.randomUUID().toString();
            
            // Create dry run result
            IngestionResult result = IngestionResult.success(mockMemoryId, category);
            result.setAgentId(input.getAgentId());
            result.setDryRun(true);
            result.setProcessingTimeMs(System.currentTimeMillis() - processingStart.toEpochMilli());
            
            totalDryRuns++;
            LOG.infof("Successfully completed dry run for agent: %s", input.getAgentId());
            
            return result;
            
        } catch (Exception e) {
            totalErrors++;
            LOG.errorf(e, "Failed dry run for agent: %s", input.getAgentId());
            
            IngestionResult result = IngestionResult.failure("Dry run failed: " + e.getMessage());
            result.setAgentId(input.getAgentId());
            result.setDryRun(true);
            result.setProcessingTimeMs(System.currentTimeMillis() - processingStart.toEpochMilli());
            return result;
        }
    }
    
    @Override
    public void validateInput(MemoryInput input) throws InvalidInputException {
        totalValidations++;
        
        if (input == null) {
            throw new InvalidInputException("Input cannot be null");
        }
        
        if (input.getAgentId() == null || input.getAgentId().trim().isEmpty()) {
            throw new InvalidInputException("Agent ID cannot be null or empty");
        }
        
        if (input.getContent() == null || input.getContent().trim().isEmpty()) {
            throw new InvalidInputException("Content cannot be null or empty");
        }
        
        // Validate content length (reasonable limits)
        if (input.getContent().length() > 10000) {
            throw new InvalidInputException("Content too long (max 10000 characters)");
        }
        
        if (input.getAgentId().length() > 100) {
            throw new InvalidInputException("Agent ID too long (max 100 characters)");
        }
        
        // Additional validation can be added here
        LOG.debugf("Input validation passed for agent: %s", input.getAgentId());
    }
    
    @Override
    public Map<String, Object> getIngestionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic statistics
        stats.put("totalIngestions", totalIngestions);
        stats.put("totalDryRuns", totalDryRuns);
        stats.put("totalValidations", totalValidations);
        stats.put("totalErrors", totalErrors);
        stats.put("uptime", System.currentTimeMillis() - startTime.toEpochMilli());
        stats.put("startTime", startTime);
        
        // Memory system statistics
        try {
            Map<String, Object> memoryStats = memorySystem.getStorageStatistics();
            stats.put("memorySystem", memoryStats);
        } catch (Exception e) {
            LOG.warnf("Failed to retrieve memory system statistics: %s", e.getMessage());
            stats.put("memorySystemError", e.getMessage());
        }
        
        // Strategy information
        stats.put("similarityStrategy", memorySystem.getSimilaritySearchStrategy().getStrategyName());
        stats.put("supportsVectorSearch", memorySystem.getSimilaritySearchStrategy().supportsVectorSearch());
        
        // Configuration
        stats.put("batchSize", memorySystem.getBatchSize());
        stats.put("maxSimilarityResults", memorySystem.getMaxSimilaritySearchResults());
        stats.put("similarityThreshold", memorySystem.getSimilarityThreshold());
        stats.put("secondLevelCacheEnabled", memorySystem.isSecondLevelCacheEnabled());
        
        return stats;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Check memory system health
            boolean memorySystemHealthy = memorySystem.isHealthy();
            
            // Check if we can perform basic operations
            boolean basicOperationsHealthy = canPerformBasicOperations();
            
            boolean healthy = memorySystemHealthy && basicOperationsHealthy;
            
            LOG.debugf("Health check result: %s (memory system: %s, basic ops: %s)", 
                      healthy, memorySystemHealthy, basicOperationsHealthy);
            
            return healthy;
            
        } catch (Exception e) {
            LOG.errorf(e, "Health check failed: %s", e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a basic category for the input.
     * In a full implementation, this would use ML models for categorization.
     * 
     * @param input The memory input
     * @return A category label
     */
    private CategoryLabel createCategory(MemoryInput input) {
        // Basic categorization based on content analysis
        String content = input.getContent().toLowerCase();
        
        String primary, secondary;
        double confidence = 0.7; // Default confidence
        
        // Simple keyword-based categorization
        if (content.contains("question") || content.contains("?")) {
            primary = "question";
            secondary = "inquiry";
        } else if (content.contains("error") || content.contains("problem") || content.contains("issue")) {
            primary = "issue";
            secondary = "problem";
        } else if (content.contains("learning") || content.contains("study") || content.contains("education")) {
            primary = "education";
            secondary = "learning";
        } else if (content.contains("code") || content.contains("programming") || content.contains("function")) {
            primary = "technical";
            secondary = "programming";
        } else {
            primary = "general";
            secondary = "information";
            confidence = 0.5;
        }
        
        CategoryLabel category = new CategoryLabel(primary, secondary);
        category.setConfidence(confidence);
        
        return category;
    }
    
    /**
     * Creates metadata for the memory input.
     * 
     * @param input The memory input
     * @return Metadata object
     */
    private Metadata createMetadata(MemoryInput input) {
        Metadata metadata = new Metadata();
        
        // Set agent information
        metadata.setProperty("agentId", input.getAgentId());
        metadata.setProperty("ingestionMethod", "REST_API");
        metadata.setProperty("processingTimestamp", Instant.now().toString());
        
        // Set importance based on content characteristics
        double importance = calculateImportance(input.getContent());
        metadata.setImportance(importance);
        
        // Set source information
        metadata.setSource("headkey-rest-api");
        metadata.setProperty("contentLength", String.valueOf(input.getContent().length()));
        
        // Additional metadata from input
        if (input.getMetadata() != null) {
            Metadata inputMetadata = input.getMetadata();
            if (inputMetadata.getProperties() != null) {
                inputMetadata.getProperties().forEach(metadata::setProperty);
            }
            if (inputMetadata.getImportance() != null) {
                metadata.setImportance(inputMetadata.getImportance());
            }
            if (inputMetadata.getSource() != null) {
                metadata.setSource(inputMetadata.getSource());
            }
            if (inputMetadata.getTags() != null) {
                metadata.setTags(inputMetadata.getTags());
            }
            if (inputMetadata.getConfidence() != null) {
                metadata.setConfidence(inputMetadata.getConfidence());
            }
        }
        
        return metadata;
    }
    
    /**
     * Calculates importance score based on content characteristics.
     * 
     * @param content The content to analyze
     * @return Importance score between 0.0 and 1.0
     */
    private double calculateImportance(String content) {
        double importance = 0.5; // Base importance
        
        // Increase importance for longer content
        if (content.length() > 500) importance += 0.1;
        if (content.length() > 1000) importance += 0.1;
        
        // Increase importance for certain keywords
        String lowerContent = content.toLowerCase();
        if (lowerContent.contains("important") || lowerContent.contains("critical")) importance += 0.2;
        if (lowerContent.contains("urgent") || lowerContent.contains("priority")) importance += 0.2;
        if (lowerContent.contains("remember") || lowerContent.contains("note")) importance += 0.1;
        
        // Cap at 1.0
        return Math.min(1.0, importance);
    }
    
    /**
     * Validates that DTOs can be properly serialized to JSON.
     * This helps catch serialization issues early before they reach the database.
     * 
     * @param category The category to validate
     * @param metadata The metadata to validate
     * @throws RuntimeException if serialization fails
     */
    private void validateDtoSerialization(CategoryLabel category, Metadata metadata) {
        try {
            // Test CategoryLabel serialization using the same configuration as converters
            if (category != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = createConfiguredObjectMapper();
                
                String categoryJson = mapper.writeValueAsString(category);
                CategoryLabel deserializedCategory = mapper.readValue(categoryJson, CategoryLabel.class);
                LOG.debugf("CategoryLabel serialization test passed: %s", categoryJson);
            }
            
            // Test Metadata serialization using the same configuration as converters
            if (metadata != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = createConfiguredObjectMapper();
                
                String metadataJson = mapper.writeValueAsString(metadata);
                Metadata deserializedMetadata = mapper.readValue(metadataJson, Metadata.class);
                LOG.debugf("Metadata serialization test passed: %s", metadataJson);
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: DTO serialization validation failed");
            System.err.println("Category: " + category);
            System.err.println("Metadata: " + metadata);
            System.err.println("Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("DTO serialization validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an ObjectMapper with the same configuration as used in the JPA converters.
     * This ensures consistent serialization behavior.
     */
    private com.fasterxml.jackson.databind.ObjectMapper createConfiguredObjectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        // Configure ObjectMapper to handle various edge cases
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // Configure visibility to only use fields and ignore all getters/setters/is-getters
        objectMapper.setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.ALL, com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.FIELD, com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY);
        
        // Disable auto-detection of getters completely
        objectMapper.configure(com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_GETTERS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_IS_GETTERS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_SETTERS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_FIELDS, true);
        
        return objectMapper;
    }

    /**
     * Checks if basic operations can be performed.
     * 
     * @return true if basic operations are working
     */
    private boolean canPerformBasicOperations() {
        try {
            // Try to get system statistics
            memorySystem.getStorageStatistics();
            
            // Check if we can access the similarity strategy
            memorySystem.getSimilaritySearchStrategy().getStrategyName();
            
            return true;
        } catch (Exception e) {
            LOG.debugf("Basic operations check failed: %s", e.getMessage());
            return false;
        }
    }
}