package ai.headkey.memory.implementations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.exceptions.MemoryNotFoundException;
import ai.headkey.memory.exceptions.StorageException;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;

/**
 * In-memory implementation of the Memory Encoding System (MES).
 * 
 * This implementation provides a simple, fast, in-memory storage solution
 * for development, testing, and demonstration purposes. It uses concurrent
 * data structures to ensure thread safety and provides basic search capabilities
 * using text matching.
 * 
 * Note: This implementation does not persist data beyond the application lifecycle
 * and is not suitable for production use where data persistence is required.
 * 
 * @since 1.0
 */
public class InMemoryMemoryEncodingSystem implements MemoryEncodingSystem {
    
    private final Map<String, MemoryRecord> memories;
    private final Map<String, Set<String>> agentMemories;
    private final Map<String, Set<String>> categoryMemories;
    private final AtomicLong idGenerator;
    private final Object lock = new Object();
    
    // Statistics tracking
    private long totalOperations = 0;
    private long totalSearches = 0;
    private long totalUpdates = 0;
    private long totalDeletes = 0;
    private final Instant startTime;
    
    /**
     * Creates a new in-memory storage system.
     */
    public InMemoryMemoryEncodingSystem() {
        this.memories = new ConcurrentHashMap<>();
        this.agentMemories = new ConcurrentHashMap<>();
        this.categoryMemories = new ConcurrentHashMap<>();
        this.idGenerator = new AtomicLong(1);
        this.startTime = Instant.now();
    }
    
    @Override
    public MemoryRecord encodeAndStore(String content, CategoryLabel category, Metadata meta, String agentId) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        if (meta == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        
        try {
            synchronized (lock) {
                // Generate unique ID
                String memoryId = "mem_" + idGenerator.getAndIncrement();
                
                // Create memory record
                MemoryRecord record = new MemoryRecord(memoryId, agentId, content, category, meta, Instant.now());
                record.setRelevanceScore(1.0); // New memories start with high relevance
                
                // Store in main map
                memories.put(memoryId, record);
                
                // Update agent index
                agentMemories.computeIfAbsent(agentId, k -> ConcurrentHashMap.newKeySet()).add(memoryId);
                
                // Update category index
                if (category != null && category.getPrimary() != null) {
                    categoryMemories.computeIfAbsent(category.getPrimary(), k -> ConcurrentHashMap.newKeySet()).add(memoryId);
                }
                
                totalOperations++;
                return record;
            }
        } catch (Exception e) {
            throw new StorageException("Failed to encode and store memory: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Optional<MemoryRecord> getMemory(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Memory ID cannot be null or empty");
        }
        
        try {
            MemoryRecord record = memories.get(memoryId);
            if (record != null) {
                record.updateLastAccessed();
                totalOperations++;
            }
            return Optional.ofNullable(record);
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve memory: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, MemoryRecord> getMemories(Set<String> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            throw new IllegalArgumentException("Memory IDs cannot be null or empty");
        }
        
        try {
            Map<String, MemoryRecord> result = new HashMap<>();
            for (String id : memoryIds) {
                MemoryRecord record = memories.get(id);
                if (record != null) {
                    record.updateLastAccessed();
                    result.put(id, record);
                }
            }
            totalOperations++;
            return result;
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public MemoryRecord updateMemory(MemoryRecord memoryRecord) {
        if (memoryRecord == null) {
            throw new IllegalArgumentException("Memory record cannot be null");
        }
        if (memoryRecord.getId() == null || memoryRecord.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Memory record ID cannot be null or empty");
        }
        
        try {
            synchronized (lock) {
                if (!memories.containsKey(memoryRecord.getId())) {
                    throw new MemoryNotFoundException("Memory with ID " + memoryRecord.getId() + " not found");
                }
                
                // Update version
                memoryRecord.setVersion(memoryRecord.getVersion() + 1);
                memoryRecord.updateLastAccessed();
                
                // Store updated record
                memories.put(memoryRecord.getId(), memoryRecord);
                
                // Update indexes if needed
                updateIndexes(memoryRecord);
                
                totalOperations++;
                totalUpdates++;
                return memoryRecord;
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new StorageException("Failed to update memory: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean removeMemory(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Memory ID cannot be null or empty");
        }
        
        try {
            synchronized (lock) {
                MemoryRecord record = memories.remove(memoryId);
                if (record != null) {
                    // Remove from agent index
                    Set<String> agentMems = agentMemories.get(record.getAgentId());
                    if (agentMems != null) {
                        agentMems.remove(memoryId);
                        if (agentMems.isEmpty()) {
                            agentMemories.remove(record.getAgentId());
                        }
                    }
                    
                    // Remove from category index
                    if (record.getCategory() != null && record.getCategory().getPrimary() != null) {
                        Set<String> catMems = categoryMemories.get(record.getCategory().getPrimary());
                        if (catMems != null) {
                            catMems.remove(memoryId);
                            if (catMems.isEmpty()) {
                                categoryMemories.remove(record.getCategory().getPrimary());
                            }
                        }
                    }
                    
                    totalOperations++;
                    totalDeletes++;
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            throw new StorageException("Failed to remove memory: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Set<String> removeMemories(Set<String> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            throw new IllegalArgumentException("Memory IDs cannot be null or empty");
        }
        
        try {
            Set<String> removed = new HashSet<>();
            for (String id : memoryIds) {
                if (removeMemory(id)) {
                    removed.add(id);
                }
            }
            return removed;
        } catch (Exception e) {
            throw new StorageException("Failed to remove memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MemoryRecord> searchSimilar(String queryContent, int limit, String agentId) {
        if (queryContent == null || queryContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Query content cannot be null or empty");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }
        
        try {
            String query = queryContent.toLowerCase().trim();
            List<MemoryRecord> results = new ArrayList<>();
            
            // Simple text-based similarity search
            for (MemoryRecord record : memories.values()) {
                if (record.getContent().toLowerCase().contains(query)) {
                    record.updateLastAccessed();
                    results.add(record);
                }
            }
            
            // Sort by relevance score (descending) and then by recency
            results.sort((a, b) -> {
                int scoreComparison = Double.compare(
                    b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0,
                    a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0
                );
                if (scoreComparison != 0) {
                    return scoreComparison;
                }
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
            
            totalOperations++;
            totalSearches++;
            
            return results.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            throw new StorageException("Failed to search memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MemoryRecord> getMemoriesForAgent(String agentId, int limit) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        
        try {
            Set<String> agentMemIds = agentMemories.get(agentId);
            if (agentMemIds == null || agentMemIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<MemoryRecord> results = agentMemIds.stream()
                .map(memories::get)
                .filter(Objects::nonNull)
                .peek(MemoryRecord::updateLastAccessed)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
            
            totalOperations++;
            
            if (limit > 0) {
                return results.stream().limit(limit).collect(Collectors.toList());
            }
            return results;
        } catch (Exception e) {
            throw new StorageException("Failed to get memories for agent: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MemoryRecord> getMemoriesInCategory(String category, String agentId, int limit) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }
        
        try {
            Set<String> categoryMemIds = categoryMemories.get(category);
            if (categoryMemIds == null || categoryMemIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<MemoryRecord> results = categoryMemIds.stream()
                .map(memories::get)
                .filter(Objects::nonNull)
                .filter(record -> agentId == null || agentId.equals(record.getAgentId()))
                .peek(MemoryRecord::updateLastAccessed)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
            
            totalOperations++;
            
            if (limit > 0) {
                return results.stream().limit(limit).collect(Collectors.toList());
            }
            return results;
        } catch (Exception e) {
            throw new StorageException("Failed to get memories in category: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MemoryRecord> getOldMemories(long olderThanSeconds, String agentId, int limit) {
        if (olderThanSeconds < 0) {
            throw new IllegalArgumentException("Age threshold cannot be negative");
        }
        
        try {
            Instant threshold = Instant.now().minusSeconds(olderThanSeconds);
            
            List<MemoryRecord> results = memories.values().stream()
                .filter(record -> record.getCreatedAt().isBefore(threshold))
                .filter(record -> agentId == null || agentId.equals(record.getAgentId()))
                .peek(MemoryRecord::updateLastAccessed)
                .sorted(Comparator.comparing(MemoryRecord::getCreatedAt))
                .collect(Collectors.toList());
            
            totalOperations++;
            
            if (limit > 0) {
                return results.stream().limit(limit).collect(Collectors.toList());
            }
            return results;
        } catch (Exception e) {
            throw new StorageException("Failed to get old memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> getStorageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMemories", memories.size());
        stats.put("totalAgents", agentMemories.size());
        stats.put("totalCategories", categoryMemories.size());
        stats.put("totalOperations", totalOperations);
        stats.put("totalSearches", totalSearches);
        stats.put("totalUpdates", totalUpdates);
        stats.put("totalDeletes", totalDeletes);
        stats.put("uptime", Instant.now().getEpochSecond() - startTime.getEpochSecond());
        stats.put("averageMemoryAge", calculateAverageMemoryAge());
        
        // Memory distribution by agent
        Map<String, Integer> agentDistribution = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : agentMemories.entrySet()) {
            agentDistribution.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("agentDistribution", agentDistribution);
        
        // Memory distribution by category
        Map<String, Integer> categoryDistribution = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : categoryMemories.entrySet()) {
            categoryDistribution.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("categoryDistribution", categoryDistribution);
        
        return stats;
    }
    
    @Override
    public Map<String, Object> getAgentStatistics(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        
        Map<String, Object> stats = new HashMap<>();
        Set<String> agentMemIds = agentMemories.get(agentId);
        
        if (agentMemIds == null || agentMemIds.isEmpty()) {
            stats.put("totalMemories", 0);
            stats.put("categories", new HashMap<String, Integer>());
            stats.put("averageRelevance", 0.0);
            stats.put("averageAge", 0L);
            return stats;
        }
        
        List<MemoryRecord> agentMems = agentMemIds.stream()
            .map(memories::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        stats.put("totalMemories", agentMems.size());
        
        // Category breakdown
        Map<String, Integer> categories = new HashMap<>();
        double totalRelevance = 0.0;
        long totalAge = 0L;
        
        for (MemoryRecord record : agentMems) {
            if (record.getCategory() != null && record.getCategory().getPrimary() != null) {
                categories.merge(record.getCategory().getPrimary(), 1, Integer::sum);
            }
            if (record.getRelevanceScore() != null) {
                totalRelevance += record.getRelevanceScore();
            }
            totalAge += record.getAgeInSeconds();
        }
        
        stats.put("categories", categories);
        stats.put("averageRelevance", agentMems.isEmpty() ? 0.0 : totalRelevance / agentMems.size());
        stats.put("averageAge", agentMems.isEmpty() ? 0L : totalAge / agentMems.size());
        
        return stats;
    }
    
    @Override
    public Map<String, Object> optimize(boolean vacuum) {
        Map<String, Object> result = new HashMap<>();
        
        synchronized (lock) {
            long startTime = System.currentTimeMillis();
            
            // Clean up empty indexes
            agentMemories.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            categoryMemories.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            
            if (vacuum) {
                // Perform deep cleanup - remove orphaned entries
                Set<String> validMemoryIds = new HashSet<>(memories.keySet());
                
                agentMemories.values().forEach(ids -> ids.retainAll(validMemoryIds));
                categoryMemories.values().forEach(ids -> ids.retainAll(validMemoryIds));
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            result.put("optimizationTime", duration);
            result.put("vacuum", vacuum);
            result.put("totalMemories", memories.size());
            result.put("agentIndexes", agentMemories.size());
            result.put("categoryIndexes", categoryMemories.size());
        }
        
        return result;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Basic health checks
            if (memories == null || agentMemories == null || categoryMemories == null) {
                return false;
            }
            
            // Check for severe memory pressure (basic heuristic)
            Runtime runtime = Runtime.getRuntime();
            long freeMemory = runtime.freeMemory();
            long totalMemory = runtime.totalMemory();
            double memoryUsage = (double) (totalMemory - freeMemory) / totalMemory;
            
            // Consider unhealthy if using more than 90% of allocated memory
            return memoryUsage < 0.9;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getCapacityInfo() {
        Map<String, Object> info = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        info.put("usedMemoryBytes", usedMemory);
        info.put("freeMemoryBytes", freeMemory);
        info.put("totalMemoryBytes", totalMemory);
        info.put("maxMemoryBytes", maxMemory);
        info.put("memoryUsagePercentage", (double) usedMemory / totalMemory * 100);
        info.put("memoriesStored", memories.size());
        
        // Estimate memory per record (rough approximation)
        if (!memories.isEmpty()) {
            long estimatedMemoryPerRecord = usedMemory / memories.size();
            info.put("estimatedMemoryPerRecord", estimatedMemoryPerRecord);
            info.put("estimatedCapacity", maxMemory / estimatedMemoryPerRecord);
        }
        
        return info;
    }
    
    /**
     * Updates the various indexes when a memory record is modified.
     */
    private void updateIndexes(MemoryRecord record) {
        // Update agent index
        agentMemories.computeIfAbsent(record.getAgentId(), k -> ConcurrentHashMap.newKeySet()).add(record.getId());
        
        // Update category index
        if (record.getCategory() != null && record.getCategory().getPrimary() != null) {
            categoryMemories.computeIfAbsent(record.getCategory().getPrimary(), k -> ConcurrentHashMap.newKeySet()).add(record.getId());
        }
    }
    
    /**
     * Calculates the average age of all memories in seconds.
     */
    private long calculateAverageMemoryAge() {
        if (memories.isEmpty()) {
            return 0L;
        }
        
        long totalAge = memories.values().stream()
            .mapToLong(MemoryRecord::getAgeInSeconds)
            .sum();
        
        return totalAge / memories.size();
    }
}