package ai.headkey.memory.langchain4j.services.ai;

import ai.headkey.memory.langchain4j.dto.TagResponse;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI service interface for LangChain4j tag extraction integration.
 * 
 * This interface defines the AI service contract using LangChain4j annotations
 * for structured interaction with Large Language Models for tag and entity extraction.
 * 
 * Consumers can provide custom implementations of this interface to customize
 * the AI interaction behavior while maintaining compatibility with the
 * LangChain4jTagExtractionService.
 * 
 * @since 1.0
 */
public interface LangChain4jTagAiService {
    
    /**
     * Extract semantic tags and entities from the provided content.
     * 
     * @param content The content to analyze for tag extraction
     * @param metadata Additional context information as JSON string
     * @return TagResponse containing extracted tags and categorized entities
     */
    @UserMessage("""
        Extract semantic tags and entities from the following content.
        Focus on identifying:
        - Named entities (people, places, organizations)
        - Key concepts and topics
        - Temporal expressions
        - Important keywords
        - Relationships
        - Technologies and tools
        - Professional terms
        
        Content: {{content}}
        Context Metadata: {{metadata}}
        
        Return a JSON object with:
        - "tags": array of extracted tags/entities
        - "entities": object with categorized entities (person, place, organization, etc.)
        
        Example response:
        {
          "tags": ["John Doe", "software engineer", "Python", "San Francisco"],
          "entities": {
            "person": ["John Doe"],
            "profession": ["software engineer"],
            "technology": ["Python"],
            "location": ["San Francisco"]
          }
        }
        
        Rules:
        1. Extract meaningful tags that represent key information
        2. Categorize entities by type (person, place, organization, technology, etc.)
        3. Include both specific entities and general concepts
        4. Avoid duplicate tags
        5. Focus on information that would be useful for search and categorization
        """)
    TagResponse extractTags(@V("content") String content, @V("metadata") String metadata);
    
    /**
     * Extract tags with a focus on specific entity types.
     * 
     * @param content The content to analyze for tag extraction
     * @param entityTypes Comma-separated list of entity types to focus on
     * @return TagResponse containing extracted tags and categorized entities
     */
    @UserMessage("""
        Extract semantic tags and entities from the following content, focusing specifically on the requested entity types.
        
        Content: {{content}}
        Focus on these entity types: {{entityTypes}}
        
        Return a JSON object with:
        - "tags": array of extracted tags/entities relevant to the focused types
        - "entities": object with categorized entities matching the focused types
        
        Example response:
        {
          "tags": ["John Doe", "software engineer", "Python"],
          "entities": {
            "person": ["John Doe"],
            "profession": ["software engineer"],
            "technology": ["Python"]
          }
        }
        
        Rules:
        1. Prioritize extraction of the specified entity types
        2. Still include other relevant tags if they provide valuable context
        3. Ensure entity categorization matches the focused types
        4. Maintain high relevance to the requested focus areas
        """)
    TagResponse extractTagsWithFocus(@V("content") String content, @V("entityTypes") String entityTypes);
}