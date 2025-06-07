package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.FilterOptions;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.memory.interfaces.RetrievalResponseEngine;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the Retrieval & Response Engine (RRE).
 * 
 * This implementation provides comprehensive search and retrieval capabilities
 * using text-based matching, simple similarity scoring, and response composition.
 * It includes caching, statistics tracking, and various search modalities.
 * 
 * Note: This implementation uses simple algorithms and is designed for development,
 * testing, and demonstration purposes. A production system would likely use more
 * sophisticated search technologies like Elasticsearch or vector databases.
 * 
 * @since 1.0
 */
public class InMemoryRetrievalResponseEngine implements RetrievalResponseEngine {
    
    private final MemoryEncodingSystem memorySystem;
    private final Map<String, Object> searchParameters;
    private final Map<String, List<MemoryRecord>> searchCache;
    private final Map<String, List<String>> recentAccessByAgent;
    private final Object lock = new Object();
    
    // Search parameters
    private double relevanceThreshold = 0.1;
    private double semanticWeight = 0.6;
    private double textWeight = 0.4;
    private int maxCacheSize = 1000;
    private long cacheExpiryMinutes = 30;
    
    // Statistics tracking
    private long totalSearches = 0;
    private long totalSimilaritySearches = 0;
    private long totalMultiModalSearches = 0;
    private long totalResponsesGenerated = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private final Map<String, Long> queryFrequency = new ConcurrentHashMap<>();
    private final Map<String, Long> agentSearchCounts = new ConcurrentHashMap<>();
    private final Map<String, Double> averageResponseTimes = new ConcurrentHashMap<>();
    private final Instant startTime;
    
    /**
     * Creates a new in-memory retrieval engine.
     * 
     * @param memorySystem The memory encoding system to search
     */
    public InMemoryRetrievalResponseEngine(MemoryEncodingSystem memorySystem) {
        this.memorySystem = memorySystem;
        this.searchParameters = new ConcurrentHashMap<>();
        this.searchCache = new ConcurrentHashMap<>();
        this.recentAccessByAgent = new ConcurrentHashMap<>();
        this.startTime = Instant.now();
        
        initializeDefaultSearchParameters();
    }
    
    /**
     * Initializes default search parameters.
     */
    private void initializeDefaultSearchParameters() {
        searchParameters.put("relevanceThreshold", relevanceThreshold);
        searchParameters.put("semanticWeight", semanticWeight);
        searchParameters.put("textWeight", textWeight);
        searchParameters.put("maxCacheSize", maxCacheSize);
        searchParameters.put("cacheExpiryMinutes", cacheExpiryMinutes);
    }
    
    @Override
    public List<MemoryRecord> retrieveRelevant(String query, FilterOptions filters, int limit) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            totalSearches++;
            
            // Update query frequency
            String normalizedQuery = query.toLowerCase().trim();
            queryFrequency.merge(normalizedQuery, 1L, Long::sum);
            
            // Update agent search count
            if (filters != null && filters.getAgentId() != null) {
                agentSearchCounts.merge(filters.getAgentId(), 1L, Long::sum);
            }
            
            // Check cache first
            String cacheKey = generateCacheKey(query, filters, limit);
            List<MemoryRecord> cachedResults = searchCache.get(cacheKey);
            if (cachedResults != null) {
                cacheHits++;
                updateRecentAccess(filters != null ? filters.getAgentId() : null, cachedResults);
                return new ArrayList<>(cachedResults);
            }
            
            cacheMisses++;
            
            // Get base memory set
            List<MemoryRecord> memories = getFilteredMemories(filters);
            
            // Perform text-based search
            List<ScoredMemory> scoredMemories = new ArrayList<>();
            String[] queryTerms = normalizedQuery.split("\\s+");
            
            for (MemoryRecord memory : memories) {
                double score = calculateTextRelevanceScore(memory, queryTerms);
                if (score > relevanceThreshold) {
                    scoredMemories.add(new ScoredMemory(memory, score));
                }
            }
            
            // Sort by score and apply limit
            List<MemoryRecord> results = scoredMemories.stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .map(sm -> sm.memory)
                .collect(Collectors.toList());
            
            // Update access tracking
            updateRecentAccess(filters != null ? filters.getAgentId() : null, results);
            
            // Cache results
            if (searchCache.size() < maxCacheSize) {
                searchCache.put(cacheKey, new ArrayList<>(results));
            }
            
            // Update performance statistics
            long duration = System.currentTimeMillis() - startTime;
            averageResponseTimes.merge("retrieveRelevant", (double) duration, 
                (oldAvg, newVal) -> (oldAvg + newVal) / 2);
            
            return results;
        } catch (Exception e) {
            throw new SearchException("Failed to retrieve relevant memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<SimilarityResult> findSimilar(String queryContent, FilterOptions filters, 
                                             int limit, double similarityThreshold) {
        if (queryContent == null || queryContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Query content cannot be null or empty");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            totalSimilaritySearches++;
            
            // Get base memory set
            List<MemoryRecord> memories = getFilteredMemories(filters);
            
            // Calculate similarity scores
            List<SimilarityResult> results = new ArrayList<>();
            String normalizedQuery = queryContent.toLowerCase().trim();
            
            for (MemoryRecord memory : memories) {
                double similarity = calculateSimilarityScore(normalizedQuery, memory.getContent().toLowerCase());
                if (similarity >= similarityThreshold) {
                    results.add(new SimilarityResult(memory, similarity));
                }
            }
            
            // Sort by similarity score and apply limit
            results.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
            
            List<SimilarityResult> limitedResults = results.stream()
                .limit(limit)
                .collect(Collectors.toList());
            
            // Update access tracking
            List<MemoryRecord> accessedMemories = limitedResults.stream()
                .map(SimilarityResult::getMemory)
                .collect(Collectors.toList());
            updateRecentAccess(filters != null ? filters.getAgentId() : null, accessedMemories);
            
            // Update performance statistics
            long duration = System.currentTimeMillis() - startTime;
            averageResponseTimes.merge("findSimilar", (double) duration, 
                (oldAvg, newVal) -> (oldAvg + newVal) / 2);
            
            return limitedResults;
        } catch (Exception e) {
            throw new SearchException("Failed to find similar memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MemoryRecord> multiModalSearch(String query, FilterOptions filters, 
                                              int limit, Map<String, Double> searchModes) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        if (searchModes == null || searchModes.isEmpty()) {
            throw new IllegalArgumentException("Search modes cannot be null or empty");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            totalMultiModalSearches++;
            
            // Get base memory set
            List<MemoryRecord> memories = getFilteredMemories(filters);
            Map<String, Double> combinedScores = new HashMap<>();
            
            // Normalize weights
            double totalWeight = searchModes.values().stream().mapToDouble(Double::doubleValue).sum();
            Map<String, Double> normalizedModes = searchModes.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue() / totalWeight
                ));
            
            // Perform different search modes
            for (Map.Entry<String, Double> mode : normalizedModes.entrySet()) {
                switch (mode.getKey().toLowerCase()) {
                    case "fulltext":
                    case "text":
                        performTextSearch(query, memories, mode.getValue(), combinedScores);
                        break;
                    case "semantic":
                    case "similarity":
                        performSemanticSearch(query, memories, mode.getValue(), combinedScores);
                        break;
                    case "metadata":
                        performMetadataSearch(query, memories, mode.getValue(), combinedScores);
                        break;
                    case "category":
                        performCategorySearch(query, memories, mode.getValue(), combinedScores);
                        break;
                }
            }
            
            // Sort by combined scores and apply limit
            List<MemoryRecord> results = combinedScores.entrySet().stream()
                .filter(entry -> entry.getValue() > relevanceThreshold)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .map(id -> memories.stream()
                    .filter(m -> m.getId().equals(id))
                    .findFirst()
                    .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            // Update access tracking
            updateRecentAccess(filters != null ? filters.getAgentId() : null, results);
            
            // Update performance statistics
            long duration = System.currentTimeMillis() - startTime;
            averageResponseTimes.merge("multiModalSearch", (double) duration, 
                (oldAvg, newVal) -> (oldAvg + newVal) / 2);
            
            return results;
        } catch (Exception e) {
            throw new SearchException("Failed to perform multi-modal search: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String composeResponse(String query, List<MemoryRecord> memories, String responseFormat) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        if (memories == null || memories.isEmpty()) {
            throw new IllegalArgumentException("Memories list cannot be null or empty");
        }
        
        try {
            totalResponsesGenerated++;
            
            String format = responseFormat != null ? responseFormat.toLowerCase() : "summary";
            
            switch (format) {
                case "summary":
                    return composeSummaryResponse(query, memories);
                case "detailed":
                    return composeDetailedResponse(query, memories);
                case "bullet-points":
                case "bullets":
                    return composeBulletPointResponse(query, memories);
                case "chronological":
                    return composeChronologicalResponse(query, memories);
                default:
                    return composeSummaryResponse(query, memories);
            }
        } catch (Exception e) {
            throw new ResponseCompositionException("Failed to compose response: " + e.getMessage(), e);
        }
    }
    
    @Override
    public CitedResponse composeResponseWithCitations(String query, List<MemoryRecord> memories, 
                                                     String responseFormat, boolean includeCitations) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        if (memories == null || memories.isEmpty()) {
            throw new IllegalArgumentException("Memories list cannot be null or empty");
        }
        
        try {
            String responseText = composeResponse(query, memories, responseFormat);
            List<String> sourceIds = memories.stream()
                .map(MemoryRecord::getId)
                .collect(Collectors.toList());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("queryProcessedAt", Instant.now().toString());
            metadata.put("memoriesUsed", memories.size());
            metadata.put("responseFormat", responseFormat);
            metadata.put("includeCitations", includeCitations);
            
            if (includeCitations) {
                StringBuilder citedResponse = new StringBuilder(responseText);
                citedResponse.append("\n\nSources:\n");
                for (int i = 0; i < sourceIds.size(); i++) {
                    citedResponse.append(String.format("[%d] Memory ID: %s\n", i + 1, sourceIds.get(i)));
                }
                responseText = citedResponse.toString();
            }
            
            return new CitedResponse(responseText, sourceIds, metadata);
        } catch (Exception e) {
            throw new ResponseCompositionException("Failed to compose cited response: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MemoryRecord> findContextuallyRelated(List<String> contextMemoryIds, 
                                                      FilterOptions filters, int limit) {
        if (contextMemoryIds == null || contextMemoryIds.isEmpty()) {
            throw new IllegalArgumentException("Context memory IDs cannot be null or empty");
        }
        
        try {
            // Get context memories
            Set<String> contextIds = new HashSet<>(contextMemoryIds);
            Map<String, MemoryRecord> contextMemories = memorySystem.getMemories(contextIds);
            
            if (contextMemories.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Extract keywords and concepts from context memories
            Set<String> contextKeywords = extractKeywords(contextMemories.values());
            
            // Search for related memories
            String contextQuery = String.join(" ", contextKeywords);
            List<MemoryRecord> candidates = retrieveRelevant(contextQuery, filters, limit * 2);
            
            // Filter out the context memories themselves
            List<MemoryRecord> related = candidates.stream()
                .filter(memory -> !contextIds.contains(memory.getId()))
                .limit(limit)
                .collect(Collectors.toList());
            
            return related;
        } catch (Exception e) {
            throw new SearchException("Failed to find contextually related memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, List<MemoryRecord>> exploreRelatedTopics(String seedQuery, 
                                                               int explorationDepth, String agentId) {
        if (seedQuery == null || seedQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Seed query cannot be null or empty");
        }
        if (explorationDepth < 1 || explorationDepth > 5) {
            throw new IllegalArgumentException("Exploration depth must be between 1 and 5");
        }
        
        try {
            Map<String, List<MemoryRecord>> topicMap = new HashMap<>();
            Set<String> exploredTerms = new HashSet<>();
            
            FilterOptions filters = new FilterOptions();
            if (agentId != null) {
                filters.setAgentId(agentId);
            }
            
            // Start with seed query
            List<MemoryRecord> seedResults = retrieveRelevant(seedQuery, filters, 10);
            topicMap.put(seedQuery, seedResults);
            exploredTerms.add(seedQuery.toLowerCase());
            
            // Explore related topics
            Queue<String> explorationQueue = new LinkedList<>();
            explorationQueue.offer(seedQuery);
            
            for (int depth = 0; depth < explorationDepth && !explorationQueue.isEmpty(); depth++) {
                int queueSize = explorationQueue.size();
                
                for (int i = 0; i < queueSize; i++) {
                    String currentTopic = explorationQueue.poll();
                    List<MemoryRecord> currentResults = topicMap.get(currentTopic);
                    
                    if (currentResults != null) {
                        // Extract related terms from current results
                        Set<String> relatedTerms = extractKeywords(currentResults);
                        
                        for (String term : relatedTerms) {
                            if (!exploredTerms.contains(term.toLowerCase()) && term.length() > 3) {
                                List<MemoryRecord> termResults = retrieveRelevant(term, filters, 5);
                                if (!termResults.isEmpty()) {
                                    topicMap.put(term, termResults);
                                    explorationQueue.offer(term);
                                    exploredTerms.add(term.toLowerCase());
                                }
                            }
                        }
                    }
                }
            }
            
            return topicMap;
        } catch (Exception e) {
            throw new SearchException("Failed to explore related topics: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<String> getSearchSuggestions(String partialQuery, String agentId, int maxSuggestions) {
        if (partialQuery == null) {
            throw new IllegalArgumentException("Partial query cannot be null");
        }
        if (maxSuggestions <= 0) {
            throw new IllegalArgumentException("Max suggestions must be greater than 0");
        }
        
        try {
            String partial = partialQuery.toLowerCase().trim();
            List<String> suggestions = new ArrayList<>();
            
            // Get suggestions from query frequency
            List<String> frequentQueries = queryFrequency.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(partial))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(maxSuggestions / 2)
                .collect(Collectors.toList());
            
            suggestions.addAll(frequentQueries);
            
            // Add content-based suggestions
            if (suggestions.size() < maxSuggestions) {
                FilterOptions filters = new FilterOptions();
                if (agentId != null) {
                    filters.setAgentId(agentId);
                }
                
                List<MemoryRecord> recentMemories = getFilteredMemories(filters).stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(100)
                    .collect(Collectors.toList());
                
                Set<String> keywords = extractKeywords(recentMemories);
                List<String> contentSuggestions = keywords.stream()
                    .filter(keyword -> keyword.toLowerCase().startsWith(partial))
                    .limit(maxSuggestions - suggestions.size())
                    .collect(Collectors.toList());
                
                suggestions.addAll(contentSuggestions);
            }
            
            return suggestions;
        } catch (Exception e) {
            return new ArrayList<>(); // Return empty list on error
        }
    }
    
    @Override
    public List<MemoryRecord> getRecentlyAccessed(String agentId, int limit) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        
        try {
            List<String> recentIds = recentAccessByAgent.get(agentId);
            if (recentIds == null || recentIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<MemoryRecord> recentMemories = new ArrayList<>();
            Set<String> limitedIds = new LinkedHashSet<>(recentIds).stream()
                .limit(limit)
                .collect(Collectors.toSet());
            
            Map<String, MemoryRecord> memories = memorySystem.getMemories(limitedIds);
            
            // Maintain access order
            for (String id : recentIds) {
                if (memories.containsKey(id) && recentMemories.size() < limit) {
                    recentMemories.add(memories.get(id));
                }
            }
            
            return recentMemories;
        } catch (Exception e) {
            return new ArrayList<>(); // Return empty list on error
        }
    }
    
    @Override
    public Map<String, Object> getSearchStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalSearches", totalSearches);
        stats.put("totalSimilaritySearches", totalSimilaritySearches);
        stats.put("totalMultiModalSearches", totalMultiModalSearches);
        stats.put("totalResponsesGenerated", totalResponsesGenerated);
        stats.put("cacheHits", cacheHits);
        stats.put("cacheMisses", cacheMisses);
        stats.put("cacheHitRate", cacheHits + cacheMisses > 0 ? (double) cacheHits / (cacheHits + cacheMisses) : 0.0);
        stats.put("uptime", Instant.now().getEpochSecond() - startTime.getEpochSecond());
        
        // Top queries
        List<Map.Entry<String, Long>> topQueries = queryFrequency.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(10)
            .collect(Collectors.toList());
        
        Map<String, Long> topQueriesMap = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : topQueries) {
            topQueriesMap.put(entry.getKey(), entry.getValue());
        }
        stats.put("topQueries", topQueriesMap);
        
        // Agent search distribution
        stats.put("agentSearchCounts", new HashMap<>(agentSearchCounts));
        
        // Average response times
        stats.put("averageResponseTimes", new HashMap<>(averageResponseTimes));
        
        // Cache statistics
        stats.put("cacheSize", searchCache.size());
        stats.put("maxCacheSize", maxCacheSize);
        
        return stats;
    }
    
    @Override
    public Map<String, Object> getAgentSearchStatistics(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalSearches", agentSearchCounts.getOrDefault(agentId, 0L));
        
        // Recent access history
        List<String> recentIds = recentAccessByAgent.get(agentId);
        stats.put("recentlyAccessedCount", recentIds != null ? recentIds.size() : 0);
        
        // Agent-specific query patterns (simplified)
        long agentQueries = queryFrequency.entrySet().stream()
            .filter(entry -> {
                // In a real implementation, we'd track queries by agent
                // For this demo, we'll estimate based on agent search count
                return true;
            })
            .mapToLong(Map.Entry::getValue)
            .sum();
        
        stats.put("estimatedQueries", Math.min(agentQueries, agentSearchCounts.getOrDefault(agentId, 0L)));
        
        return stats;
    }
    
    @Override
    public void configureSearchParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        
        synchronized (lock) {
            searchParameters.putAll(parameters);
            
            // Update internal parameters
            if (parameters.containsKey("relevanceThreshold")) {
                relevanceThreshold = ((Number) parameters.get("relevanceThreshold")).doubleValue();
            }
            if (parameters.containsKey("semanticWeight")) {
                semanticWeight = ((Number) parameters.get("semanticWeight")).doubleValue();
            }
            if (parameters.containsKey("textWeight")) {
                textWeight = ((Number) parameters.get("textWeight")).doubleValue();
            }
            if (parameters.containsKey("maxCacheSize")) {
                maxCacheSize = ((Number) parameters.get("maxCacheSize")).intValue();
            }
            if (parameters.containsKey("cacheExpiryMinutes")) {
                cacheExpiryMinutes = ((Number) parameters.get("cacheExpiryMinutes")).longValue();
            }
        }
    }
    
    @Override
    public Map<String, Object> optimizeSearchIndexes() {
        try {
            long startTime = System.currentTimeMillis();
            
            synchronized (lock) {
                // Clear expired cache entries
                searchCache.clear();
                
                // Clean up recent access lists that are too long
                for (Map.Entry<String, List<String>> entry : recentAccessByAgent.entrySet()) {
                    List<String> accessList = entry.getValue();
                    if (accessList.size() > 100) {
                        entry.setValue(new ArrayList<>(accessList.subList(0, 100)));
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> result = new HashMap<>();
            result.put("optimizationTime", duration);
            result.put("cacheCleared", true);
            result.put("accessListsCleaned", recentAccessByAgent.size());
            result.put("optimizedAt", Instant.now().toString());
            
            return result;
        } catch (Exception e) {
            throw new SearchException("Failed to optimize search indexes: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            return memorySystem != null && memorySystem.isHealthy() &&
                   searchParameters != null && searchCache != null &&
                   recentAccessByAgent != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets filtered memories based on FilterOptions.
     */
    private List<MemoryRecord> getFilteredMemories(FilterOptions filters) {
        if (filters == null) {
            // Get all memories from all agents
            Map<String, Object> stats = memorySystem.getStorageStatistics();
            @SuppressWarnings("unchecked")
            Map<String, Integer> agentDist = (Map<String, Integer>) stats.get("agentDistribution");
            
            List<MemoryRecord> allMemories = new ArrayList<>();
            for (String agentId : agentDist.keySet()) {
                allMemories.addAll(memorySystem.getMemoriesForAgent(agentId, 0));
            }
            return allMemories;
        }
        
        List<MemoryRecord> memories;
        
        // Filter by agent
        if (filters.getAgentId() != null && !filters.getAgentId().trim().isEmpty()) {
            memories = memorySystem.getMemoriesForAgent(filters.getAgentId(), 0);
        } else {
            Map<String, Object> stats = memorySystem.getStorageStatistics();
            @SuppressWarnings("unchecked")
            Map<String, Integer> agentDist = (Map<String, Integer>) stats.get("agentDistribution");
            
            memories = new ArrayList<>();
            for (String agentId : agentDist.keySet()) {
                memories.addAll(memorySystem.getMemoriesForAgent(agentId, 0));
            }
        }
        
        // Filter by category
        if (filters.getCategory() != null && !filters.getCategory().trim().isEmpty()) {
            memories = memories.stream()
                .filter(memory -> filters.getCategory().equals(
                    memory.getCategory() != null ? memory.getCategory().getPrimary() : null))
                .collect(Collectors.toList());
        }
        
        // Filter by date range
        if (filters.getSince() != null || filters.getUntil() != null) {
            memories = memories.stream()
                .filter(memory -> {
                    Instant createdAt = memory.getCreatedAt();
                    if (createdAt == null) return false;
                    
                    if (filters.getSince() != null && createdAt.isBefore(filters.getSince())) {
                        return false;
                    }
                    if (filters.getUntil() != null && createdAt.isAfter(filters.getUntil())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
        }
        
        return memories;
    }
    
    /**
     * Calculates text relevance score for a memory against query terms.
     */
    private double calculateTextRelevanceScore(MemoryRecord memory, String[] queryTerms) {
        String content = memory.getContent().toLowerCase();
        String category = memory.getCategory() != null ? memory.getCategory().getPrimary() : "";
        
        double score = 0.0;
        double maxTermScore = 0.0;
        
        for (String term : queryTerms) {
            term = term.trim();
            if (term.isEmpty()) continue;
            
            double termScore = 0.0;
            
            // Exact match in content
            if (content.contains(term)) {
                termScore += 0.8;
                
                // Boost for title/beginning matches
                if (content.startsWith(term)) {
                    termScore += 0.2;
                }
            }
            
            // Category match
            if (category.toLowerCase().contains(term)) {
                termScore += 0.3;
            }
            
            // Tag matches
            if (memory.getMetadata() != null && memory.getMetadata().getTags() != null) {
                for (String tag : memory.getMetadata().getTags()) {
                    if (tag.toLowerCase().contains(term)) {
                        termScore += 0.2;
                        break;
                    }
                }
            }
            
            maxTermScore = Math.max(maxTermScore, termScore);
            score += termScore;
        }
        
        // Normalize by number of terms, but ensure some weight for partial matches
        score = (score / queryTerms.length) * 0.7 + maxTermScore * 0.3;
        
        // Boost based on relevance score
        if (memory.getRelevanceScore() != null) {
            score += memory.getRelevanceScore() * 0.1;
        }
        
        return Math.min(1.0, score);
    }
    
    /**
     * Calculates similarity score between two text strings.
     */
    private double calculateSimilarityScore(String text1, String text2) {
        // Simple Jaccard similarity
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        if (union.isEmpty()) {
            return 0.0;
        }
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * Performs text search and updates combined scores.
     */
    private void performTextSearch(String query, List<MemoryRecord> memories, 
                                  double weight, Map<String, Double> combinedScores) {
        String[] queryTerms = query.toLowerCase().split("\\s+");
        
        for (MemoryRecord memory : memories) {
            double score = calculateTextRelevanceScore(memory, queryTerms);
            combinedScores.merge(memory.getId(), score * weight, Double::sum);
        }
    }
    
    /**
     * Performs semantic search and updates combined scores.
     */
    private void performSemanticSearch(String query, List<MemoryRecord> memories, 
                                      double weight, Map<String, Double> combinedScores) {
        String normalizedQuery = query.toLowerCase();
        
        for (MemoryRecord memory : memories) {
            double similarity = calculateSimilarityScore(normalizedQuery, memory.getContent().toLowerCase());
            combinedScores.merge(memory.getId(), similarity * weight, Double::sum);
        }
    }
    
    /**
     * Performs metadata search and updates combined scores.
     */
    private void performMetadataSearch(String query, List<MemoryRecord> memories, 
                                      double weight, Map<String, Double> combinedScores) {
        String normalizedQuery = query.toLowerCase();
        
        for (MemoryRecord memory : memories) {
            double score = 0.0;
            
            if (memory.getMetadata() != null) {
                // Check tags
                if (memory.getMetadata().getTags() != null) {
                    for (String tag : memory.getMetadata().getTags()) {
                        if (tag.toLowerCase().contains(normalizedQuery)) {
                            score += 0.8;
                        }
                    }
                }
                
                // Check source
                if (memory.getMetadata().getSource() != null && 
                    memory.getMetadata().getSource().toLowerCase().contains(normalizedQuery)) {
                    score += 0.6;
                }
            }
            
            if (score > 0) {
                combinedScores.merge(memory.getId(), Math.min(1.0, score) * weight, Double::sum);
            }
        }
    }
    
    /**
     * Performs category search and updates combined scores.
     */
    private void performCategorySearch(String query, List<MemoryRecord> memories, 
                                      double weight, Map<String, Double> combinedScores) {
        String normalizedQuery = query.toLowerCase();
        
        for (MemoryRecord memory : memories) {
            if (memory.getCategory() != null) {
                double score = 0.0;
                
                // Check primary category
                if (memory.getCategory().getPrimary() != null &&
                    memory.getCategory().getPrimary().toLowerCase().contains(normalizedQuery)) {
                    score += 1.0;
                }
                
                // Check subcategory
                if (memory.getCategory().getSecondary() != null &&
                    memory.getCategory().getSecondary().toLowerCase().contains(normalizedQuery)) {
                    score += 0.8;
                }
                
                // Check category tags
                if (memory.getCategory().getTags() != null) {
                    for (String tag : memory.getCategory().getTags()) {
                        if (tag.toLowerCase().contains(normalizedQuery)) {
                            score += 0.6;
                            break;
                        }
                    }
                }
                
                if (score > 0) {
                    combinedScores.merge(memory.getId(), Math.min(1.0, score) * weight, Double::sum);
                }
            }
        }
    }
    
    /**
     * Extracts keywords from a collection of memories.
     */
    private Set<String> extractKeywords(Collection<MemoryRecord> memories) {
        Set<String> keywords = new HashSet<>();
        Set<String> stopWords = Set.of("the", "and", "for", "are", "but", "not", "you", "all", 
            "can", "had", "her", "was", "one", "our", "out", "day", "get", "has", "him", 
            "his", "how", "its", "may", "new", "now", "old", "see", "two", "who", "boy", 
            "did", "does", "let", "man", "put", "say", "she", "too", "use", "this", "that", 
            "with", "have", "from", "they", "know", "want", "been", "good", "much", "some", 
            "time", "very", "when", "come", "here", "just", "like", "long", "make", "many", 
            "over", "such", "take", "than", "them", "well", "were");
        
        for (MemoryRecord memory : memories) {
            // Extract from content
            String[] words = memory.getContent().toLowerCase().split("\\s+");
            for (String word : words) {
                word = word.replaceAll("[^a-zA-Z0-9]", "");
                if (word.length() > 3 && !stopWords.contains(word)) {
                    keywords.add(word);
                }
            }
            
            // Extract from tags
            if (memory.getMetadata() != null && memory.getMetadata().getTags() != null) {
                for (String tag : memory.getMetadata().getTags()) {
                    String cleanTag = tag.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
                    if (cleanTag.length() > 3 && !stopWords.contains(cleanTag)) {
                        keywords.add(cleanTag);
                    }
                }
            }
            
            // Extract from category
            if (memory.getCategory() != null && memory.getCategory().getPrimary() != null) {
                String category = memory.getCategory().getPrimary().toLowerCase();
                if (!stopWords.contains(category)) {
                    keywords.add(category);
                }
            }
        }
        
        return keywords.stream().limit(50).collect(Collectors.toSet()); // Limit to avoid too many keywords
    }
    
    /**
     * Composes a summary response from memories.
     */
    private String composeSummaryResponse(String query, List<MemoryRecord> memories) {
        StringBuilder response = new StringBuilder();
        response.append("Based on your query about '").append(query).append("', here's what I found:\n\n");
        
        // Group memories by relevance
        List<MemoryRecord> highRelevance = memories.stream()
            .filter(m -> m.getRelevanceScore() != null && m.getRelevanceScore() > 0.7)
            .limit(3)
            .collect(Collectors.toList());
        
        if (!highRelevance.isEmpty()) {
            response.append("Key information:\n");
            for (MemoryRecord memory : highRelevance) {
                String excerpt = memory.getContent().length() > 100 ? 
                    memory.getContent().substring(0, 97) + "..." : memory.getContent();
                response.append("- ").append(excerpt).append("\n");
            }
        } else {
            response.append("Related information:\n");
            for (int i = 0; i < Math.min(3, memories.size()); i++) {
                MemoryRecord memory = memories.get(i);
                String excerpt = memory.getContent().length() > 100 ? 
                    memory.getContent().substring(0, 97) + "..." : memory.getContent();
                response.append("- ").append(excerpt).append("\n");
            }
        }
        
        return response.toString();
    }
    
    /**
     * Composes a detailed response from memories.
     */
    private String composeDetailedResponse(String query, List<MemoryRecord> memories) {
        StringBuilder response = new StringBuilder();
        response.append("Detailed information about '").append(query).append("':\n\n");
        
        for (int i = 0; i < Math.min(5, memories.size()); i++) {
            MemoryRecord memory = memories.get(i);
            response.append("Memory ").append(i + 1).append(":\n");
            response.append(memory.getContent()).append("\n");
            
            if (memory.getCreatedAt() != null) {
                response.append("Created: ").append(memory.getCreatedAt()).append("\n");
            }
            
            if (memory.getCategory() != null && memory.getCategory().getPrimary() != null) {
                response.append("Category: ").append(memory.getCategory().getPrimary()).append("\n");
            }
            
            response.append("\n");
        }
        
        return response.toString();
    }
    
    /**
     * Composes a bullet-point response from memories.
     */
    private String composeBulletPointResponse(String query, List<MemoryRecord> memories) {
        StringBuilder response = new StringBuilder();
        response.append("Summary of '").append(query).append("':\n\n");
        
        Set<String> keyPoints = new HashSet<>();
        
        for (MemoryRecord memory : memories.stream().limit(5).collect(Collectors.toList())) {
            // Extract key sentences (simple approach)
            String[] sentences = memory.getContent().split("[.!?]+");
            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (sentence.length() > 20 && sentence.length() < 150) {
                    keyPoints.add(sentence);
                    if (keyPoints.size() >= 8) break;
                }
            }
            if (keyPoints.size() >= 8) break;
        }
        
        for (String point : keyPoints) {
            response.append("• ").append(point).append("\n");
        }
        
        return response.toString();
    }
    
    /**
     * Composes a chronological response from memories.
     */
    private String composeChronologicalResponse(String query, List<MemoryRecord> memories) {
        StringBuilder response = new StringBuilder();
        response.append("Chronological view of '").append(query).append("':\n\n");
        
        List<MemoryRecord> sortedMemories = memories.stream()
            .filter(m -> m.getCreatedAt() != null)
            .sorted(Comparator.comparing(MemoryRecord::getCreatedAt))
            .limit(5)
            .collect(Collectors.toList());
        
        for (MemoryRecord memory : sortedMemories) {
            response.append(memory.getCreatedAt().truncatedTo(ChronoUnit.DAYS)).append(": ");
            String excerpt = memory.getContent().length() > 80 ? 
                memory.getContent().substring(0, 77) + "..." : memory.getContent();
            response.append(excerpt).append("\n\n");
        }
        
        return response.toString();
    }
    
    /**
     * Updates recent access tracking for an agent.
     */
    private void updateRecentAccess(String agentId, List<MemoryRecord> memories) {
        if (agentId == null || memories.isEmpty()) {
            return;
        }
        
        synchronized (lock) {
            List<String> recentList = recentAccessByAgent.computeIfAbsent(agentId, k -> new ArrayList<>());
            
            for (MemoryRecord memory : memories) {
                // Remove if already exists to move to front
                recentList.remove(memory.getId());
                // Add to front
                recentList.add(0, memory.getId());
                
                // Update memory access time
                memory.updateLastAccessed();
            }
            
            // Keep only the most recent 50 accesses
            if (recentList.size() > 50) {
                recentList.subList(50, recentList.size()).clear();
            }
        }
    }
    
    /**
     * Generates a cache key for search results.
     */
    private String generateCacheKey(String query, FilterOptions filters, int limit) {
        StringBuilder key = new StringBuilder();
        key.append(query.toLowerCase().trim());
        key.append("|limit:").append(limit);
        
        if (filters != null) {
            if (filters.getAgentId() != null) {
                key.append("|agent:").append(filters.getAgentId());
            }
            if (filters.getCategory() != null) {
                key.append("|category:").append(filters.getCategory());
            }
            if (filters.getSince() != null) {
                key.append("|start:").append(filters.getSince());
            }
            if (filters.getUntil() != null) {
                key.append("|end:").append(filters.getUntil());
            }
        }
        
        return key.toString();
    }
    
    /**
     * Helper class for scored memories.
     */
    private static class ScoredMemory {
        final MemoryRecord memory;
        final double score;
        
        ScoredMemory(MemoryRecord memory, double score) {
            this.memory = memory;
            this.score = score;
        }
    }
}