package ai.headkey.memory.implementations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.*;

import ai.headkey.memory.dto.BeliefUpdateResult;
import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.IngestionResult;
import ai.headkey.memory.dto.MemoryInput;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.enums.Status;
import ai.headkey.memory.exceptions.InvalidInputException;
import ai.headkey.memory.interfaces.BeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;

/**
 * General implementation of the Information Ingestion Module (IIM).
 * 
 * This implementation orchestrates the complete memory ingestion pipeline
 * by coordinating the Contextual Categorization Engine (CCE), Memory Encoding
 * System (MES), and Belief Reinforcement & Conflict Analyzer (BRCA).
 * 
 * The implementation provides comprehensive validation, error handling,
 * and statistics tracking while maintaining thread safety and performance.
 * 
 * This is a general-purpose implementation that can work with any service
 * implementations injected through its constructor, making it suitable for
 * both in-memory and persistent storage scenarios.
 * 
 * @since 1.0
 */
public class InformationIngestionModuleImpl implements InformationIngestionModule {

    private static final Logger LOG = Logger.getLogger(InformationIngestionModuleImpl.class.getName());
    
    private final ContextualCategorizationEngine categorizationEngine;
    private final MemoryEncodingSystem encodingSystem;
    private final BeliefReinforcementConflictAnalyzer beliefAnalyzer;
    private final Map<String, Object> configuration;
    private final Object lock = new Object();
    
    // Validation parameters
    private int minContentLength = 1;
    private int maxContentLength = 10000;
    private int maxAgentIdLength = 100;
    private Set<String> requiredFields = Set.of("agentId", "content");
    private Set<String> allowedSources = new HashSet<>();
    
    // Statistics tracking
    private final AtomicLong totalIngestions = new AtomicLong(0);
    private final AtomicLong successfulIngestions = new AtomicLong(0);
    private final AtomicLong failedIngestions = new AtomicLong(0);
    private final AtomicLong validationFailures = new AtomicLong(0);
    private final AtomicLong dryRunExecutions = new AtomicLong(0);
    private final Map<String, Long> agentIngestionCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> sourceDistribution = new ConcurrentHashMap<>();
    private final Map<String, Long> categoryDistribution = new ConcurrentHashMap<>();
    private final Map<String, Double> processingTimes = new ConcurrentHashMap<>();
    private final Instant startTime;
    
    /**
     * Creates a new information ingestion module implementation.
     * 
     * @param categorizationEngine The categorization engine to use
     * @param encodingSystem The memory encoding system to use
     * @param beliefAnalyzer The belief analysis system to use
     */
    public InformationIngestionModuleImpl(
            ContextualCategorizationEngine categorizationEngine,
            MemoryEncodingSystem encodingSystem,
            BeliefReinforcementConflictAnalyzer beliefAnalyzer) {
        
        this.categorizationEngine = categorizationEngine;
        this.encodingSystem = encodingSystem;
        this.beliefAnalyzer = beliefAnalyzer;
        this.configuration = new ConcurrentHashMap<>();
        this.startTime = Instant.now();
        
        initializeConfiguration();
        initializeAllowedSources();
    }
    
    /**
     * Initializes default configuration parameters.
     */
    private void initializeConfiguration() {
        configuration.put("minContentLength", minContentLength);
        configuration.put("maxContentLength", maxContentLength);
        configuration.put("maxAgentIdLength", maxAgentIdLength);
        configuration.put("enableValidation", true);
        configuration.put("enableBeliefAnalysis", true);
        configuration.put("enableCategorization", true);
        
        // Log configuration for debugging
        System.out.println("InformationIngestionModule Configuration:");
        System.out.println("  enableBeliefAnalysis: " + configuration.get("enableBeliefAnalysis"));
        System.out.println("  enableCategorization: " + configuration.get("enableCategorization"));
        configuration.put("retryAttempts", 3);
        configuration.put("timeoutSeconds", 30);
    }
    
    /**
     * Initializes allowed sources (empty means all sources allowed).
     */
    private void initializeAllowedSources() {
        // Allow common sources by default
        allowedSources.addAll(Set.of(
            "conversation", "user_input", "system_event", "sensor", 
            "knowledge_base", "api", "file", "manual", "import"
        ));
    }
    
    @Override
    public IngestionResult ingest(MemoryInput input) throws InvalidInputException {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        
        long startTime = System.currentTimeMillis();
        totalIngestions.incrementAndGet();
        
        try {
            // Step 1: Validate input
            validateInput(input);
            
            // Step 2: Perform categorization
            CategoryLabel category = null;
            if ((Boolean) configuration.get("enableCategorization")) {
                category = categorizationEngine.categorize(input.getContent(), input.getMetadata());
            }
            
            // Step 3: Encode and store memory
            Metadata metadata = input.getOrCreateMetadata();
            metadata.setProperty("agentId", input.getAgentId());
            MemoryRecord memoryRecord = encodingSystem.encodeAndStore(
                input.getContent(), 
                category, 
                metadata,
                input.getAgentId()
            );
            
            // Step 4: Analyze beliefs
            BeliefUpdateResult beliefResult = null;
            if ((Boolean) configuration.get("enableBeliefAnalysis")) {
                LOG.info(String.format("Starting belief analysis for memory: %s, agent: %s", 
                    memoryRecord.getId(), input.getAgentId()));
                LOG.finest(String.format("Memory content for analysis: '%s'", memoryRecord.getContent()));
                
                try {
                    beliefResult = beliefAnalyzer.analyzeNewMemory(memoryRecord);
                    
                    if (beliefResult != null) {
                        LOG.info(String.format("Belief analysis completed successfully - ID: %s, New: %d, Reinforced: %d, Conflicts: %d",
                            memoryRecord.getId(),
                            beliefResult.getNewBeliefs() != null ? beliefResult.getNewBeliefs().size() : 0,
                            beliefResult.getReinforcedBeliefs() != null ? beliefResult.getReinforcedBeliefs().size() : 0,
                            beliefResult.getConflicts() != null ? beliefResult.getConflicts().size() : 0));
                        
                        // Log new beliefs created
                        if (beliefResult.getNewBeliefs() != null) {
                            for (var belief : beliefResult.getNewBeliefs()) {
                                LOG.finest(String.format("New belief created - ID: %s, Statement: '%s', Confidence: %.2f",
                                    belief.getId(), belief.getStatement(), belief.getConfidence()));
                            }
                        }
                        
                        // Log reinforced beliefs
                        if (beliefResult.getReinforcedBeliefs() != null) {
                            for (var belief : beliefResult.getReinforcedBeliefs()) {
                                LOG.finest(String.format("Belief reinforced - ID: %s, Statement: '%s'",
                                    belief.getId(), belief.getStatement()));
                            }
                        }
                        
                    } else {
                        LOG.warning(String.format("Belief analysis returned null result for memory: %s", memoryRecord.getId()));
                    }
                } catch (Exception e) {
                    LOG.severe(String.format("Error during belief analysis for memory %s: %s", 
                        memoryRecord.getId(), e.getMessage()));
                    // Continue processing even if belief analysis fails
                }
            } else {
                LOG.finest(String.format("Belief analysis is disabled in configuration for memory: %s", memoryRecord.getId()));
            }
            
            // Step 5: Create and populate result
            IngestionResult result = new IngestionResult();
            result.setMemoryId(memoryRecord.getId());
            result.setAgentId(input.getAgentId());
            result.setStatus(Status.SUCCESS);
            result.setTimestamp(Instant.now());
            result.setCategory(category);
            result.setEncodedSuccessfully(true);
            result.setBeliefUpdateResult(beliefResult);
            
            // Update statistics
            successfulIngestions.incrementAndGet();
            agentIngestionCounts.merge(input.getAgentId(), 1L, Long::sum);
            
            if (input.getSource() != null) {
                sourceDistribution.merge(input.getSource(), 1L, Long::sum);
            }
            
            if (category != null && category.getPrimary() != null) {
                categoryDistribution.merge(category.getPrimary(), 1L, Long::sum);
            }
            
            // Record processing time
            long duration = System.currentTimeMillis() - startTime;
            processingTimes.merge("ingest", (double) duration, 
                (oldAvg, newVal) -> (oldAvg + newVal) / 2);
            
            return result;
            
        } catch (InvalidInputException e) {
            failedIngestions.incrementAndGet();
            validationFailures.incrementAndGet();
            throw e;
        } catch (Exception e) {
            failedIngestions.incrementAndGet();
            
            // Create error result
            IngestionResult errorResult = new IngestionResult();
            errorResult.setAgentId(input.getAgentId());
            errorResult.setStatus(Status.ERROR);
            errorResult.setTimestamp(Instant.now());
            errorResult.setErrorMessage("Ingestion failed: " + e.getMessage());
            errorResult.setEncodedSuccessfully(false);
            
            return errorResult;
        }
    }
    
    @Override
    public void validateInput(MemoryInput input) throws InvalidInputException {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        
        if (!(Boolean) configuration.get("enableValidation")) {
            return; // Skip validation if disabled
        }
        
        List<String> errors = new ArrayList<>();
        
        // Validate required fields
        for (String field : requiredFields) {
            switch (field) {
                case "agentId":
                    if (input.getAgentId() == null || input.getAgentId().trim().isEmpty()) {
                        errors.add("Agent ID is required and cannot be empty");
                    } else if (input.getAgentId().length() > maxAgentIdLength) {
                        errors.add("Agent ID exceeds maximum length of " + maxAgentIdLength);
                    }
                    break;
                case "content":
                    if (input.getContent() == null || input.getContent().trim().isEmpty()) {
                        errors.add("Content is required and cannot be empty");
                    } else {
                        if (input.getContent().length() < minContentLength) {
                            errors.add("Content must be at least " + minContentLength + " characters");
                        }
                        if (input.getContent().length() > maxContentLength) {
                            errors.add("Content exceeds maximum length of " + maxContentLength);
                        }
                    }
                    break;
            }
        }
        
        // Validate agent ID format
        if (input.getAgentId() != null && !isValidAgentId(input.getAgentId())) {
            errors.add("Agent ID contains invalid characters (only alphanumeric, underscore, and hyphen allowed)");
        }
        
        // Validate source if specified
        if (input.getSource() != null && !allowedSources.isEmpty() && 
            !allowedSources.contains(input.getSource())) {
            errors.add("Source '" + input.getSource() + "' is not in the allowed sources list");
        }
        
        // Validate timestamp if specified
        if (input.getTimestamp() != null) {
            Instant now = Instant.now();
            Instant oneYearAgo = now.minusSeconds(365 * 24 * 60 * 60);
            Instant oneHourFromNow = now.plusSeconds(60 * 60);
            
            if (input.getTimestamp().isBefore(oneYearAgo)) {
                errors.add("Timestamp cannot be more than one year in the past");
            }
            if (input.getTimestamp().isAfter(oneHourFromNow)) {
                errors.add("Timestamp cannot be more than one hour in the future");
            }
        }
        
        // Validate metadata if present
        if (input.getMetadata() != null) {
            validateMetadata(input.getMetadata(), errors);
        }
        
        if (!errors.isEmpty()) {
            throw new InvalidInputException("Input validation failed: " + String.join(", ", errors));
        }
    }
    
    @Override
    public IngestionResult dryRunIngest(MemoryInput input) throws InvalidInputException {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        
        long startTime = System.currentTimeMillis();
        dryRunExecutions.incrementAndGet();
        
        try {
            // Step 1: Validate input
            validateInput(input);
            
            // Step 2: Perform categorization (but don't store)
            CategoryLabel category = null;
            if ((Boolean) configuration.get("enableCategorization")) {
                category = categorizationEngine.categorize(input.getContent(), input.getMetadata());
            }
            
            // Step 3: Create dry run result
            IngestionResult result = new IngestionResult();
            result.setAgentId(input.getAgentId());
            result.setStatus(Status.SUCCESS);
            result.setTimestamp(Instant.now());
            result.setCategory(category);
            result.setEncodedSuccessfully(false); // Not actually encoded
            result.setDryRun(true);
            
            // Add preview information
            Map<String, Object> previewData = new HashMap<>();
            previewData.put("contentLength", input.getContent().length());
            previewData.put("effectiveTimestamp", input.getEffectiveTimestamp());
            previewData.put("hasMetadata", input.hasMetadata());
            
            if (category != null) {
                previewData.put("primaryCategory", category.getPrimary());
                previewData.put("subcategory", category.getSecondary());
                previewData.put("categoryConfidence", category.getConfidence());
                previewData.put("extractedTags", category.getTags().size());
            }
            
            result.setPreviewData(previewData);
            
            // Record processing time
            long duration = System.currentTimeMillis() - startTime;
            processingTimes.merge("dryRun", (double) duration, 
                (oldAvg, newVal) -> (oldAvg + newVal) / 2);
            
            return result;
            
        } catch (InvalidInputException e) {
            validationFailures.incrementAndGet();
            throw e;
        } catch (Exception e) {
            // Create error result for dry run
            IngestionResult errorResult = new IngestionResult();
            errorResult.setAgentId(input.getAgentId());
            errorResult.setStatus(Status.ERROR);
            errorResult.setTimestamp(Instant.now());
            errorResult.setErrorMessage("Dry run failed: " + e.getMessage());
            errorResult.setEncodedSuccessfully(false);
            errorResult.setDryRun(true);
            
            return errorResult;
        }
    }
    
    @Override
    public Map<String, Object> getIngestionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic counts
        stats.put("totalIngestions", totalIngestions.get());
        stats.put("successfulIngestions", successfulIngestions.get());
        stats.put("failedIngestions", failedIngestions.get());
        stats.put("validationFailures", validationFailures.get());
        stats.put("dryRunExecutions", dryRunExecutions.get());
        
        // Success rate
        long total = totalIngestions.get();
        double successRate = total > 0 ? (double) successfulIngestions.get() / total : 0.0;
        stats.put("successRate", successRate);
        
        // Uptime
        stats.put("uptime", Instant.now().getEpochSecond() - startTime.getEpochSecond());
        
        // Distribution statistics
        stats.put("agentIngestionCounts", new HashMap<>(agentIngestionCounts));
        stats.put("sourceDistribution", new HashMap<>(sourceDistribution));
        stats.put("categoryDistribution", new HashMap<>(categoryDistribution));
        
        // Performance metrics
        stats.put("averageProcessingTimes", new HashMap<>(processingTimes));
        
        // Most active agent
        String mostActiveAgent = agentIngestionCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none");
        stats.put("mostActiveAgent", mostActiveAgent);
        
        // Most common source
        String mostCommonSource = sourceDistribution.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none");
        stats.put("mostCommonSource", mostCommonSource);
        
        // Most common category
        String mostCommonCategory = categoryDistribution.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none");
        stats.put("mostCommonCategory", mostCommonCategory);
        
        // Configuration
        stats.put("configuration", new HashMap<>(configuration));
        
        return stats;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Check that all required components are available and healthy
            if (categorizationEngine == null || !categorizationEngine.isHealthy()) {
                LOG.warning("Categorization engine is not healthy");
                return false;
            }
            
            if (encodingSystem == null || !encodingSystem.isHealthy()) {
                LOG.warning("Memory encoding system is not healthy");
                return false;
            }
            
            if (beliefAnalyzer == null || !beliefAnalyzer.isHealthy()) {
                LOG.warning("Belief reinforcement conflict analyzer is not healthy");
                return false;
            }
            
            // Check configuration integrity
            if (configuration == null || configuration.isEmpty()) {
                LOG.warning("Configuration is null or empty");
                return false;
            }
            
            // Check for reasonable resource usage
            Runtime runtime = Runtime.getRuntime();
            long freeMemory = runtime.freeMemory();
            long totalMemory = runtime.totalMemory();
            double memoryUsage = (double) (totalMemory - freeMemory) / totalMemory;
            
            // Consider unhealthy if using more than 95% of allocated memory
            if (memoryUsage > 0.95) {
                LOG.warning("Memory usage is too high: " + (memoryUsage * 100) + "%");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validates agent ID format.
     */
    private boolean isValidAgentId(String agentId) {
        // Allow alphanumeric characters, underscore, and hyphen
        return agentId.matches("^[a-zA-Z0-9_-]+$");
    }
    
    /**
     * Validates metadata structure and content.
     */
    private void validateMetadata(ai.headkey.memory.dto.Metadata metadata, List<String> errors) {
        // Validate importance if specified
        if (metadata.getImportance() != null) {
            Double importance = metadata.getImportance();
            if (importance != null && (importance < 0 || importance > 10)) {
                errors.add("Importance must be between 0 and 10");
            }
        }
        
        // Validate tags if specified
        if (metadata.getTags() != null) {
            if (metadata.getTags().size() > 20) {
                errors.add("Maximum of 20 tags allowed");
            }
            
            for (String tag : metadata.getTags()) {
                if (tag == null || tag.trim().isEmpty()) {
                    errors.add("Tags cannot be null or empty");
                    break;
                }
                if (tag.length() > 50) {
                    errors.add("Tag length cannot exceed 50 characters: " + tag);
                    break;
                }
            }
        }
        
        // Validate additional data size
        if (metadata.getProperties() != null && metadata.getProperties().size() > 50) {
            errors.add("Maximum of 50 additional data entries allowed");
        }
        
        // Validate source length
        if (metadata.getSource() != null && metadata.getSource().length() > 100) {
            errors.add("Source length cannot exceed 100 characters");
        }
    }
    
    /**
     * Configures the ingestion module parameters.
     * 
     * @param parameters Map of parameter names to values
     */
    public void configure(Map<String, Object> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        
        synchronized (lock) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                switch (key) {
                    case "minContentLength":
                        if (value instanceof Number) {
                            minContentLength = ((Number) value).intValue();
                            configuration.put(key, minContentLength);
                        }
                        break;
                    case "maxContentLength":
                        if (value instanceof Number) {
                            maxContentLength = ((Number) value).intValue();
                            configuration.put(key, maxContentLength);
                        }
                        break;
                    case "maxAgentIdLength":
                        if (value instanceof Number) {
                            maxAgentIdLength = ((Number) value).intValue();
                            configuration.put(key, maxAgentIdLength);
                        }
                        break;
                    default:
                        configuration.put(key, value);
                        break;
                }
            }
        }
    }
    
    /**
     * Gets the current configuration.
     * 
     * @return Map of configuration parameters
     */
    public Map<String, Object> getConfiguration() {
        return new HashMap<>(configuration);
    }
    
    /**
     * Adds allowed sources for validation.
     * 
     * @param sources Set of allowed source values
     */
    public void setAllowedSources(Set<String> sources) {
        if (sources == null) {
            throw new IllegalArgumentException("Sources cannot be null");
        }
        
        synchronized (lock) {
            allowedSources.clear();
            allowedSources.addAll(sources);
        }
    }
    
    /**
     * Gets the currently allowed sources.
     * 
     * @return Set of allowed source values
     */
    public Set<String> getAllowedSources() {
        return new HashSet<>(allowedSources);
    }
    
    /**
     * Resets all statistics counters.
     */
    public void resetStatistics() {
        synchronized (lock) {
            totalIngestions.set(0);
            successfulIngestions.set(0);
            failedIngestions.set(0);
            validationFailures.set(0);
            dryRunExecutions.set(0);
            agentIngestionCounts.clear();
            sourceDistribution.clear();
            categoryDistribution.clear();
            processingTimes.clear();
        }
    }
}