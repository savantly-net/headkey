package ai.headkey.memory.interfaces;

import ai.headkey.memory.dto.FilterOptions;
import ai.headkey.memory.dto.MemoryRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for the Retrieval & Response Engine (RRE).
 * 
 * The RRE is responsible for querying the memory store and providing results
 * to agents and external systems. It handles both search and retrieval operations
 * as well as optional response composition from retrieved memories.
 * 
 * The interface supports various search modalities including full-text search,
 * semantic similarity search, and structured filtering. It abstracts the
 * underlying search implementation (search indexes, vector databases, etc.)
 * and provides a unified interface for memory retrieval.
 * 
 * The RRE ensures that retrieval is efficient and relevant, employing context
 * filtering so that agents only receive information appropriate to their
 * situation and maintaining relevance over time.
 * 
 * @since 1.0
 */
public interface RetrievalResponseEngine {
    
    /**
     * Searches the memory store for relevant entries given a query.
     * 
     * This is the core search function that finds memory records matching
     * the provided query. The search may employ multiple techniques:
     * - Full-text matching on memory content
     * - Semantic similarity using vector embeddings
     * - Metadata and tag matching
     * - Category-based filtering
     * - Relevance scoring and ranking
     * 
     * Results are returned sorted by relevance with the most pertinent
     * memories first. The search respects any applied filters and limits.
     * 
     * @param query The search query (keywords, natural language, or structured query)
     * @param filters Optional filters to narrow the search scope
     * @param limit Maximum number of results to return (must be > 0)
     * @return List of MemoryRecord results matching the query, sorted by relevance
     * @throws IllegalArgumentException if query is null/empty or limit <= 0
     * @throws SearchException if the search operation fails
     * 
     * @since 1.0
     */
    List<MemoryRecord> retrieveRelevant(String query, FilterOptions filters, int limit);
    
    /**
     * Performs semantic similarity search using vector embeddings.
     * 
     * Finds memories that are semantically similar to the query content
     * using vector similarity algorithms. This is particularly effective
     * for finding conceptually related information even when exact
     * keywords don't match.
     * 
     * @param queryContent The content to find similar memories for
     * @param filters Optional filters to apply to the search
     * @param limit Maximum number of results to return
     * @param similarityThreshold Minimum similarity score threshold (0.0 to 1.0)
     * @return List of semantically similar MemoryRecords with similarity scores
     * @throws IllegalArgumentException if parameters are invalid
     * @throws SearchException if the similarity search fails
     * 
     * @since 1.0
     */
    List<SimilarityResult> findSimilar(String queryContent, FilterOptions filters, 
                                      int limit, double similarityThreshold);
    
    /**
     * Performs a multi-modal search combining different search techniques.
     * 
     * Executes multiple search strategies in parallel and combines the results
     * using a weighted ranking algorithm. This provides comprehensive coverage
     * by leveraging both exact matching and semantic similarity.
     * 
     * @param query The search query
     * @param filters Optional filters to apply
     * @param limit Maximum number of results to return
     * @param searchModes Map of search modes to their weights (e.g., "fulltext": 0.6, "semantic": 0.4)
     * @return List of MemoryRecords ranked by combined relevance scores
     * @throws IllegalArgumentException if parameters are invalid
     * @throws SearchException if the multi-modal search fails
     * 
     * @since 1.0
     */
    List<MemoryRecord> multiModalSearch(String query, FilterOptions filters, 
                                       int limit, Map<String, Double> searchModes);
    
    /**
     * Generates a response or summary from retrieved memories for a given query.
     * 
     * Takes the raw memory results from a search and composes them into a
     * human-readable response or summary. This may involve:
     * - Extracting key information from multiple memories
     * - Resolving conflicts between different memories
     * - Synthesizing information into a coherent answer
     * - Formatting the response appropriately
     * - Citing source memories
     * 
     * This method is optional - some systems may prefer to handle response
     * composition externally using the raw memory results.
     * 
     * @param query The original query or question
     * @param memories List of MemoryRecords relevant to the query
     * @param responseFormat Optional format specification (e.g., "summary", "detailed", "bullet-points")
     * @return A composed response string using the provided memories
     * @throws IllegalArgumentException if query is null or memories is empty
     * @throws ResponseCompositionException if response generation fails
     * 
     * @since 1.0
     */
    String composeResponse(String query, List<MemoryRecord> memories, String responseFormat);
    
    /**
     * Generates a response with citation information.
     * 
     * Creates a response that includes references to the source memories
     * used in composing the answer, enabling traceability and verification.
     * 
     * @param query The original query
     * @param memories List of MemoryRecords used for the response
     * @param responseFormat Response format specification
     * @param includeCitations Whether to include memory ID citations
     * @return CitedResponse containing the response text and source citations
     * @throws IllegalArgumentException if parameters are invalid
     * @throws ResponseCompositionException if response generation fails
     * 
     * @since 1.0
     */
    CitedResponse composeResponseWithCitations(String query, List<MemoryRecord> memories, 
                                              String responseFormat, boolean includeCitations);
    
    /**
     * Retrieves memories based on contextual similarity to recent memories.
     * 
     * Finds memories that are contextually related to recently accessed or
     * created memories, useful for maintaining conversational context or
     * suggesting related information.
     * 
     * @param contextMemoryIds List of memory IDs to use as context
     * @param filters Optional additional filters
     * @param limit Maximum number of results to return
     * @return List of contextually related memories
     * @throws IllegalArgumentException if contextMemoryIds is null or empty
     * @throws SearchException if contextual search fails
     * 
     * @since 1.0
     */
    List<MemoryRecord> findContextuallyRelated(List<String> contextMemoryIds, 
                                              FilterOptions filters, int limit);
    
    /**
     * Performs an exploratory search to discover related topics.
     * 
     * Given a starting query or concept, explores the memory space to find
     * related topics, themes, or areas of knowledge that might be of interest.
     * Useful for knowledge discovery and exploration workflows.
     * 
     * @param seedQuery The starting query or concept
     * @param explorationDepth How many levels deep to explore (1-5)
     * @param agentId Optional agent filter
     * @return Map of discovered topics to related memories
     * @throws IllegalArgumentException if parameters are invalid
     * @throws SearchException if exploration fails
     * 
     * @since 1.0
     */
    Map<String, List<MemoryRecord>> exploreRelatedTopics(String seedQuery, 
                                                         int explorationDepth, String agentId);
    
    /**
     * Gets search suggestions based on partial query input.
     * 
     * Provides query completion and suggestion capabilities by analyzing
     * existing memory content and common search patterns.
     * 
     * @param partialQuery The incomplete query text
     * @param agentId Optional agent filter for personalized suggestions
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of suggested query completions
     * @throws IllegalArgumentException if partialQuery is null or maxSuggestions <= 0
     * 
     * @since 1.0
     */
    List<String> getSearchSuggestions(String partialQuery, String agentId, int maxSuggestions);
    
    /**
     * Retrieves recently accessed memories for an agent.
     * 
     * Returns memories that have been recently retrieved or accessed,
     * useful for maintaining working memory context or providing
     * quick access to recently used information.
     * 
     * @param agentId The agent whose recent memories to retrieve
     * @param limit Maximum number of recent memories to return
     * @return List of recently accessed memories ordered by access time
     * @throws IllegalArgumentException if agentId is null or limit <= 0
     * 
     * @since 1.0
     */
    List<MemoryRecord> getRecentlyAccessed(String agentId, int limit);
    
    /**
     * Gets search and retrieval statistics.
     * 
     * Returns comprehensive statistics about search operations including:
     * - Total number of searches performed
     * - Query frequency and patterns
     * - Search success rates
     * - Average response times
     * - Popular search terms
     * - Result relevance metrics
     * 
     * @return Map containing various search and retrieval statistics
     * 
     * @since 1.0
     */
    Map<String, Object> getSearchStatistics();
    
    /**
     * Gets search statistics for a specific agent.
     * 
     * Returns agent-specific search metrics including:
     * - Search history and patterns
     * - Preferred query types
     * - Most accessed memory categories
     * - Search success rates
     * - Response time patterns
     * 
     * @param agentId The agent to get statistics for
     * @return Map containing agent-specific search statistics
     * @throws IllegalArgumentException if agentId is null or empty
     * 
     * @since 1.0
     */
    Map<String, Object> getAgentSearchStatistics(String agentId);
    
    /**
     * Configures search parameters and weights.
     * 
     * Allows customization of search behavior by setting parameters such as:
     * - Relevance scoring weights
     * - Similarity thresholds
     * - Result ranking algorithms
     * - Search timeout values
     * - Cache settings
     * 
     * @param parameters Map of parameter names to values
     * @throws IllegalArgumentException if parameters is null
     * 
     * @since 1.0
     */
    void configureSearchParameters(Map<String, Object> parameters);
    
    /**
     * Optimizes search indexes and caches.
     * 
     * Performs maintenance operations to optimize search performance:
     * - Rebuilding search indexes
     * - Updating vector embeddings
     * - Clearing stale caches
     * - Recomputing relevance scores
     * 
     * @return Map containing optimization results and metrics
     * @throws SearchException if optimization fails
     * 
     * @since 1.0
     */
    Map<String, Object> optimizeSearchIndexes();
    
    /**
     * Checks if the retrieval engine is healthy and ready.
     * 
     * Performs a health check of the retrieval system including:
     * - Search index availability
     * - Vector database connectivity
     * - Cache system status
     * - Query processing capacity
     * 
     * @return true if the system is healthy and ready to process requests
     * 
     * @since 1.0
     */
    boolean isHealthy();
    
    /**
     * Data class representing a similarity search result with score.
     */
    public static class SimilarityResult {
        private final MemoryRecord memory;
        private final double similarityScore;
        
        public SimilarityResult(MemoryRecord memory, double similarityScore) {
            this.memory = memory;
            this.similarityScore = similarityScore;
        }
        
        public MemoryRecord getMemory() { return memory; }
        public double getSimilarityScore() { return similarityScore; }
        
        @Override
        public String toString() {
            return "SimilarityResult{memory=" + memory.getId() + ", score=" + similarityScore + "}";
        }
    }
    
    /**
     * Data class representing a response with source citations.
     */
    public static class CitedResponse {
        private final String responseText;
        private final List<String> sourceMemoryIds;
        private final Map<String, Object> metadata;
        
        public CitedResponse(String responseText, List<String> sourceMemoryIds, Map<String, Object> metadata) {
            this.responseText = responseText;
            this.sourceMemoryIds = sourceMemoryIds;
            this.metadata = metadata;
        }
        
        public String getResponseText() { return responseText; }
        public List<String> getSourceMemoryIds() { return sourceMemoryIds; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        @Override
        public String toString() {
            return "CitedResponse{text='" + responseText + "', sources=" + sourceMemoryIds.size() + "}";
        }
    }
    
    /**
     * Exception thrown when search operations fail.
     */
    public static class SearchException extends RuntimeException {
        public SearchException(String message) {
            super(message);
        }
        
        public SearchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown when response composition fails.
     */
    public static class ResponseCompositionException extends RuntimeException {
        public ResponseCompositionException(String message) {
            super(message);
        }
        
        public ResponseCompositionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}