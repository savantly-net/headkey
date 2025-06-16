package ai.headkey.memory.interfaces;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;

import java.util.List;

/**
 * Service Provider Interface for belief extraction from memory content.
 * 
 * This interface defines the contract for extracting structured beliefs
 * from unstructured memory content. Implementations can range from simple
 * pattern matching to sophisticated AI-powered analysis using NLP models,
 * knowledge graphs, or large language models.
 * 
 * The interface supports different extraction strategies:
 * - Rule-based pattern matching (regex, keyword detection)
 * - Natural Language Processing (NER, dependency parsing, sentiment analysis)
 * - AI-powered analysis (LLMs, embedding-based similarity, reasoning engines)
 * - Hybrid approaches combining multiple techniques
 * 
 * @since 1.0
 */
public interface BeliefExtractionService {

    /**
     * Extracts potential beliefs from memory content.
     * 
     * This method analyzes the given memory content and extracts structured
     * beliefs that can be stored and managed in the belief system. The
     * extraction process should identify:
     * - Factual statements that can be verified or contradicted
     * - Preferences and opinions expressed by the agent
     * - Relationships between entities
     * - Temporal or spatial information
     * - Negations or contradictions
     * 
     * @param content The memory content to analyze
     * @param agentId The ID of the agent who owns this memory
     * @param category Optional category information to guide extraction
     * @return List of extracted beliefs with confidence scores
     * @throws BeliefExtractionException if extraction fails
     */
    List<ExtractedBelief> extractBeliefs(String content, String agentId, CategoryLabel category);

    /**
     * Calculates similarity between two belief statements.
     * 
     * This method determines how similar two belief statements are,
     * which is crucial for conflict detection and belief reinforcement.
     * The similarity calculation can use various approaches:
     * - Textual similarity (edit distance, n-grams, TF-IDF)
     * - Semantic similarity (word embeddings, sentence transformers)
     * - Structural similarity (parsing trees, entity relationships)
     * 
     * @param statement1 First belief statement
     * @param statement2 Second belief statement
     * @return Similarity score between 0.0 (completely different) and 1.0 (identical)
     */
    double calculateSimilarity(String statement1, String statement2);

    /**
     * Determines if two belief statements are conflicting.
     * 
     * This method identifies when two beliefs contradict each other,
     * which is essential for maintaining consistency in the belief system.
     * Conflicts can arise from:
     * - Direct negation ("likes coffee" vs "doesn't like coffee")
     * - Mutually exclusive statements ("lives in Paris" vs "lives in London")
     * - Contradictory facts ("born in 1990" vs "born in 1985")
     * 
     * @param statement1 First belief statement
     * @param statement2 Second belief statement
     * @param category1 Category of first belief (optional)
     * @param category2 Category of second belief (optional)
     * @return true if the statements conflict, false otherwise
     */
    boolean areConflicting(String statement1, String statement2, String category1, String category2);

    /**
     * Extracts the semantic category or type of a belief statement.
     * 
     * This method classifies beliefs into semantic categories to enable
     * better organization and targeted conflict resolution strategies.
     * Common categories include:
     * - "preference" (likes, dislikes, favorites)
     * - "fact" (objective statements about the world)
     * - "relationship" (connections between entities)
     * - "location" (spatial information)
     * - "temporal" (time-related information)
     * - "opinion" (subjective judgments)
     * 
     * @param statement The belief statement to categorize
     * @return The semantic category of the belief
     */
    String extractCategory(String statement);

    /**
     * Calculates the confidence level for an extracted belief.
     * 
     * This method assigns a confidence score to beliefs based on various
     * factors such as:
     * - Linguistic certainty markers ("I'm sure", "maybe", "definitely")
     * - Source reliability and credibility
     * - Consistency with existing knowledge
     * - Strength of evidence in the content
     * 
     * @param content The original memory content
     * @param statement The extracted belief statement
     * @param context Additional context information
     * @return Confidence score between 0.0 (very uncertain) and 1.0 (very certain)
     */
    double calculateConfidence(String content, String statement, ExtractionContext context);

    /**
     * Checks if the extraction service is healthy and ready to process requests.
     * 
     * This method performs health checks specific to the extraction implementation:
     * - Model availability (for AI-based extractors)
     * - API connectivity (for external services)
     * - Resource availability (memory, CPU)
     * - Configuration validity
     * 
     * @return true if the service is healthy and ready
     */
    boolean isHealthy();

    /**
     * Gets metadata about the extraction service capabilities.
     * 
     * This method returns information about the service implementation:
     * - Service type and version
     * - Supported languages
     * - Extraction capabilities
     * - Performance characteristics
     * - Configuration parameters
     * 
     * @return Map containing service metadata
     */
    java.util.Map<String, Object> getServiceInfo();

    /**
     * Represents a belief extracted from content analysis.
     */
    class ExtractedBelief {
        private final String statement;
        private final String agentId;
        private final String category;
        private final double confidence;
        private final boolean positive;
        private final java.util.Set<String> tags;
        private final java.util.Map<String, Object> metadata;

        public ExtractedBelief(String statement, String agentId, String category, 
                              double confidence, boolean positive) {
            this.statement = statement;
            this.agentId = agentId;
            this.category = category;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
            this.positive = positive;
            this.tags = new java.util.HashSet<>();
            this.metadata = new java.util.HashMap<>();
        }

        public String getStatement() { return statement; }
        public String getAgentId() { return agentId; }
        public String getCategory() { return category; }
        public double getConfidence() { return confidence; }
        public boolean isPositive() { return positive; }
        public java.util.Set<String> getTags() { return new java.util.HashSet<>(tags); }
        public java.util.Map<String, Object> getMetadata() { return new java.util.HashMap<>(metadata); }

        public void addTag(String tag) {
            if (tag != null && !tag.trim().isEmpty()) {
                tags.add(tag.trim());
            }
        }

        public void addMetadata(String key, Object value) {
            if (key != null && !key.trim().isEmpty()) {
                metadata.put(key, value);
            }
        }

        @Override
        public String toString() {
            return "ExtractedBelief{" +
                    "statement='" + statement + '\'' +
                    ", agentId='" + agentId + '\'' +
                    ", category='" + category + '\'' +
                    ", confidence=" + confidence +
                    ", positive=" + positive +
                    '}';
        }
    }

    /**
     * Context information for belief extraction.
     */
    class ExtractionContext {
        private final MemoryRecord sourceMemory;
        private final java.util.List<String> existingBeliefs;
        private final java.util.Map<String, Object> additionalContext;

        public ExtractionContext(MemoryRecord sourceMemory) {
            this.sourceMemory = sourceMemory;
            this.existingBeliefs = new java.util.ArrayList<>();
            this.additionalContext = new java.util.HashMap<>();
        }

        public MemoryRecord getSourceMemory() { return sourceMemory; }
        public java.util.List<String> getExistingBeliefs() { return new java.util.ArrayList<>(existingBeliefs); }
        public java.util.Map<String, Object> getAdditionalContext() { return new java.util.HashMap<>(additionalContext); }

        public void addExistingBelief(String belief) {
            if (belief != null && !belief.trim().isEmpty()) {
                existingBeliefs.add(belief);
            }
        }

        public void addContext(String key, Object value) {
            if (key != null && !key.trim().isEmpty()) {
                additionalContext.put(key, value);
            }
        }
    }

    /**
     * Exception thrown when belief extraction fails.
     */
    class BeliefExtractionException extends RuntimeException {
        public BeliefExtractionException(String message) {
            super(message);
        }

        public BeliefExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}