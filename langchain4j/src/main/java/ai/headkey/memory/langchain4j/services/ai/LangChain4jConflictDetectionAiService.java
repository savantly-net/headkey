package ai.headkey.memory.langchain4j.services.ai;

import ai.headkey.memory.langchain4j.dto.ConflictDetectionResponse;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI service interface for conflict detection between belief statements using LangChain4j.
 * 
 * This interface defines the AI service contract for detecting conflicts
 * between belief statements using Large Language Models to identify
 * contradictions, mutual exclusions, and logical inconsistencies.
 * 
 * @since 1.0
 */
public interface LangChain4jConflictDetectionAiService {
    
    /**
     * Detects conflicts between two belief statements.
     * 
     * @param statement1 The first statement
     * @param statement2 The second statement
     * @param categories The categories of both statements
     * @return ConflictDetectionResponse containing conflict analysis
     */
    @UserMessage("""
        Determine if these two statements conflict with each other:
        
        Statement 1: {{statement1}}
        Statement 2: {{statement2}}
        Categories: {{categories}}
        
        Consider these types of conflicts:
        - Direct contradictions (likes X vs dislikes X)
        - Mutually exclusive statements (lives in A vs lives in B)
        - Logical inconsistencies
        - Temporal conflicts (was born in 1990 vs was born in 1985)
        
        Respond with JSON: {
          "conflicting": true/false,
          "confidence": 0.85,
          "conflictType": "direct_contradiction|mutual_exclusion|logical_inconsistency|temporal_conflict|none",
          "reasoning": "explanation"
        }
        """)
    ConflictDetectionResponse detectConflict(@V("statement1") String statement1,
                                           @V("statement2") String statement2,
                                           @V("categories") String categories);
}