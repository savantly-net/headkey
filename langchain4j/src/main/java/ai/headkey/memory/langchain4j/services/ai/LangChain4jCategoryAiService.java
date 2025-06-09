package ai.headkey.memory.langchain4j.services.ai;

import ai.headkey.memory.langchain4j.dto.CategoryResponse;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI service interface for LangChain4j integration.
 * 
 * This interface defines the AI service contract using LangChain4j annotations
 * for structured interaction with Large Language Models.
 */
public interface LangChain4jCategoryAiService {
    
    @UserMessage("""
        Analyze the following content and categorize it according to the available categories.
        
        Content: {{content}}
        Available Categories: {{categories}}
        Context Metadata: {{metadata}}
        
        Please respond with a JSON object containing:
        - "primary": the main category from the available categories
        - "secondary": an optional subcategory (can be null)
        - "confidence": a confidence score between 0.0 and 1.0
        - "reasoning": brief explanation for the categorization
        
        Example response:
        {
          "primary": "UserProfile",
          "secondary": "Preferences",
          "confidence": 0.85,
          "reasoning": "Content describes user preferences for food and activities"
        }
        
        Rules:
        1. MUST use only categories from the available categories list
        2. If unsure, use "Unknown" with low confidence
        3. Confidence should reflect certainty in the categorization
        4. Secondary category is optional and should be more specific than primary
        5. Reasoning should be concise but informative
        
        If the content doesn't clearly fit any category, use "Unknown" with appropriate confidence.
        """)
    CategoryResponse categorizeContent(@V("content") String content, 
                                      @V("categories") String categories,
                                      @V("metadata") String metadata);
}