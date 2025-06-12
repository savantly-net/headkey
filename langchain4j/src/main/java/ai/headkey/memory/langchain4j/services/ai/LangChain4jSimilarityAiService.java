package ai.headkey.memory.langchain4j.services.ai;

import ai.headkey.memory.langchain4j.dto.SimilarityResponse;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI service interface for semantic similarity calculation using LangChain4j.
 * 
 * This interface defines the AI service contract for calculating semantic
 * similarity between belief statements using Large Language Models.
 * 
 * @since 1.0
 */
public interface LangChain4jSimilarityAiService {
    
    /**
     * Calculates semantic similarity between two statements.
     * 
     * @param statement1 The first statement
     * @param statement2 The second statement
     * @return SimilarityResponse containing similarity score and reasoning
     */
    @UserMessage("""
        Calculate the semantic similarity between these two statements:
        
        Statement 1: {{statement1}}
        Statement 2: {{statement2}}
        
        Consider:
        - Semantic meaning and intent
        - Shared concepts and entities
        - Overall message similarity
        - Context and implications
        
        Respond with JSON: {"similarityScore": 0.75, "reasoning": "explanation"}
        Score should be between 0.0 (completely different) and 1.0 (identical meaning).
        """)
    SimilarityResponse calculateSimilarity(@V("statement1") String statement1,
                                         @V("statement2") String statement2);
}