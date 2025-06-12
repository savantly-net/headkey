package ai.headkey.memory.langchain4j.services.ai;

import ai.headkey.memory.langchain4j.dto.BeliefExtractionResponse;
import ai.headkey.memory.langchain4j.dto.CategoryExtractionResponse;
import ai.headkey.memory.langchain4j.dto.ConfidenceResponse;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI service interface for belief extraction using LangChain4j.
 * 
 * This interface defines the AI service contract using LangChain4j annotations
 * for structured interaction with Large Language Models for belief extraction,
 * categorization, and confidence calculation.
 * 
 * @since 1.0
 */
public interface LangChain4jBeliefExtractionAiService {
    
    /**
     * Extracts beliefs from content using AI analysis.
     * 
     * @param content The content to analyze
     * @param agentId The agent identifier
     * @param categoryContext The category context information
     * @return BeliefExtractionResponse containing extracted beliefs
     */
    @UserMessage("""
        Analyze the following content and extract beliefs, preferences, facts, and relationships.
        
        Content: {{content}}
        Agent ID: {{agentId}}
        Category Context: {{categoryContext}}
        
        Please respond with a JSON object containing an array of beliefs:
        {
          "beliefs": [
            {
              "statement": "extracted belief statement",
              "category": "preference|fact|relationship|location|opinion",
              "confidence": 0.85,
              "positive": true,
              "reasoning": "brief explanation",
              "tags": ["tag1", "tag2"]
            }
          ]
        }
        
        Rules:
        1. Extract explicit and implicit beliefs from the content
        2. Categories: preference, fact, relationship, location, opinion, general
        3. Confidence should reflect certainty (0.0 to 1.0)
        4. Positive indicates if the belief is affirmative (true) or negative (false)
        5. Include reasoning for each extracted belief
        6. Add relevant tags for better organization
        7. Focus on factual, verifiable, or strongly expressed beliefs
        
        Extract all meaningful beliefs but avoid over-interpretation.
        """)
    BeliefExtractionResponse extractBeliefs(@V("content") String content, 
                                           @V("agentId") String agentId,
                                           @V("categoryContext") String categoryContext);

    /**
     * Extracts the category of a belief statement.
     * 
     * @param statement The statement to categorize
     * @return CategoryExtractionResponse containing the category and confidence
     */
    @UserMessage("""
        Categorize the following statement into one of these belief categories:
        - preference: likes, dislikes, favorites, personal choices
        - fact: objective statements, verifiable information
        - relationship: connections between people, entities, or concepts
        - location: spatial information, places, geographic data
        - opinion: subjective judgments, beliefs about quality/value
        - general: statements that don't fit other categories
        
        Statement: {{statement}}
        
        Respond with JSON: {"category": "selected_category", "confidence": 0.85}
        """)
    CategoryExtractionResponse extractCategory(@V("statement") String statement);

    /**
     * Calculates confidence for an extracted belief.
     * 
     * @param content The original content
     * @param statement The extracted statement
     * @param context The extraction context
     * @return ConfidenceResponse containing confidence and reasoning
     */
    @UserMessage("""
        Calculate the confidence level for this extracted belief based on the original content.
        
        Original Content: {{content}}
        Extracted Statement: {{statement}}
        Context: {{context}}
        
        Consider these factors:
        - Linguistic certainty markers (definitely, maybe, probably)
        - Strength of evidence in the content
        - Clarity and explicitness of the statement
        - Consistency with context
        
        Respond with JSON: {"confidence": 0.75, "reasoning": "explanation"}
        """)
    ConfidenceResponse calculateConfidence(@V("content") String content,
                                         @V("statement") String statement,
                                         @V("context") String context);
}