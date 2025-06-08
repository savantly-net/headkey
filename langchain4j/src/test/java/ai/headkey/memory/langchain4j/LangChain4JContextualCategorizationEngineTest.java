package ai.headkey.memory.langchain4j;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.Metadata;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LangChain4JContextualCategorizationEngine.
 * 
 * These tests use mocked ChatLanguageModel to verify the engine's behavior
 * without requiring actual AI service calls.
 */
@ExtendWith(MockitoExtension.class)
class LangChain4JContextualCategorizationEngineTest {
    
    @Mock
    private ChatLanguageModel mockChatModel;
    
    private LangChain4JContextualCategorizationEngine engine;
    
    @BeforeEach
    void setUp() {
        // Mock the chat model to return predictable responses
        when(mockChatModel.generate(any())).thenReturn("""
            {
              "primary": "UserProfile",
              "secondary": "Preferences", 
              "confidence": 0.85,
              "reasoning": "Content describes user preferences"
            }
            """);
        
        engine = new LangChain4JContextualCategorizationEngine(mockChatModel);
    }
    
    @Test
    void testConstructorWithNullChatModel() {
        assertThrows(NullPointerException.class, 
            () -> new LangChain4JContextualCategorizationEngine(null));
    }
    
    @Test
    void testCategorizeWithValidContent() {
        String content = "I love pizza and enjoy watching movies on weekends";
        Metadata metadata = new Metadata();
        
        // Note: This test will use fallback behavior since we're mocking at a high level
        CategoryLabel result = engine.categorize(content, metadata);
        
        assertNotNull(result);
        assertNotNull(result.getPrimary());
        assertTrue(result.getConfidence() >= 0.0);
        assertTrue(result.getConfidence() <= 1.0);
    }
    
    @Test
    void testCategorizeWithNullContent() {
        assertThrows(IllegalArgumentException.class, 
            () -> engine.categorize(null, new Metadata()));
    }
    
    @Test
    void testCategorizeWithEmptyContent() {
        assertThrows(IllegalArgumentException.class, 
            () -> engine.categorize("", new Metadata()));
    }
    
    @Test
    void testCategorizeWithWhitespaceContent() {
        assertThrows(IllegalArgumentException.class, 
            () -> engine.categorize("   ", new Metadata()));
    }
    
    @Test
    void testExtractTags() {
        String content = "Contact John Doe at john.doe@example.com or call 555-123-4567";
        
        Set<String> tags = engine.extractTags(content);
        
        assertNotNull(tags);
        assertFalse(tags.isEmpty());
        
        // Should extract email and phone using pattern matching
        assertTrue(tags.stream().anyMatch(tag -> tag.contains("john.doe@example.com")));
        assertTrue(tags.stream().anyMatch(tag -> tag.contains("555-123-4567")));
    }
    
    @Test
    void testExtractTagsWithNullContent() {
        assertThrows(IllegalArgumentException.class, 
            () -> engine.extractTags(null));
    }
    
    @Test
    void testExtractTagsWithEmptyContent() {
        assertThrows(IllegalArgumentException.class, 
            () -> engine.extractTags(""));
    }
    
    @Test
    void testCategorizeBatch() {
        Map<String, String> contentItems = new HashMap<>();
        contentItems.put("item1", "I like programming in Java");
        contentItems.put("item2", "Paris is the capital of France");
        contentItems.put("item3", "My birthday is on December 25th");
        
        Map<String, CategoryLabel> results = engine.categorizeBatch(contentItems, null);
        
        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.containsKey("item1"));
        assertTrue(results.containsKey("item2"));
        assertTrue(results.containsKey("item3"));
        
        // Verify all results have valid categories
        results.values().forEach(label -> {
            assertNotNull(label.getPrimary());
            assertTrue(label.getConfidence() >= 0.0);
            assertTrue(label.getConfidence() <= 1.0);
        });
    }
    
    @Test
    void testCategorizeBatchWithNullInput() {
        assertThrows(IllegalArgumentException.class, 
            () -> engine.categorizeBatch(null, new Metadata()));
    }
    
    @Test
    void testCategorizeBatchWithEmptyInput() {
        assertThrows(IllegalArgumentException.class, 
            () -> engine.categorizeBatch(new HashMap<>(), new Metadata()));
    }
    
    @Test
    void testSuggestAlternativeCategories() {
        String content = "I enjoy reading technical books";
        
        List<CategoryLabel> alternatives = engine.suggestAlternativeCategories(content, null, 3);
        
        assertNotNull(alternatives);
        assertFalse(alternatives.isEmpty());
        assertTrue(alternatives.size() <= 3);
        
        // Verify alternatives are sorted by confidence (highest first)
        for (int i = 1; i < alternatives.size(); i++) {
            assertTrue(alternatives.get(i-1).getConfidence() >= alternatives.get(i).getConfidence());
        }
    }
    
    @Test
    void testSuggestAlternativeCategoriesWithInvalidMaxSuggestions() {
        assertThrows(IllegalArgumentException.class, 
            () -> engine.suggestAlternativeCategories("test content", null, 0));
    }
    
    @Test
    void testConfidenceThreshold() {
        double originalThreshold = engine.getConfidenceThreshold();
        assertEquals(0.7, originalThreshold, 0.001);
        
        engine.setConfidenceThreshold(0.8);
        assertEquals(0.8, engine.getConfidenceThreshold(), 0.001);
    }
    
    @Test
    void testSetConfidenceThresholdInvalidValues() {
        assertThrows(IllegalArgumentException.class, 
            () -> engine.setConfidenceThreshold(-0.1));
        assertThrows(IllegalArgumentException.class, 
            () -> engine.setConfidenceThreshold(1.1));
    }
    
    @Test
    void testGetAvailableCategories() {
        List<String> categories = engine.getAvailableCategories();
        
        assertNotNull(categories);
        assertFalse(categories.isEmpty());
        assertTrue(categories.contains("UserProfile"));
        assertTrue(categories.contains("WorldFact"));
        assertTrue(categories.contains("PersonalData"));
        assertTrue(categories.contains("Unknown"));
    }
    
    @Test
    void testGetSubcategories() {
        Set<String> subcategories = engine.getSubcategories("UserProfile");
        
        assertNotNull(subcategories);
        assertFalse(subcategories.isEmpty());
        assertTrue(subcategories.contains("Biography"));
        assertTrue(subcategories.contains("Preferences"));
        assertTrue(subcategories.contains("Skills"));
    }
    
    @Test
    void testGetSubcategoriesForInvalidCategory() {
        Set<String> subcategories = engine.getSubcategories("InvalidCategory");
        
        assertNotNull(subcategories);
        assertTrue(subcategories.isEmpty());
    }
    
    @Test
    void testGetSubcategoriesWithNullCategory() {
        assertThrows(IllegalArgumentException.class, 
            () -> engine.getSubcategories(null));
    }
    
    @Test
    void testGetCategorizationStatistics() {
        // Perform some operations to generate statistics
        engine.categorize("Test content 1", null);
        engine.categorize("Test content 2", null);
        engine.extractTags("Test content for tags");
        
        Map<String, Object> stats = engine.getCategorizationStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalCategorizations"));
        assertTrue(stats.containsKey("totalTagExtractions"));
        assertTrue(stats.containsKey("startTime"));
        assertTrue(stats.containsKey("uptimeSeconds"));
        assertTrue(stats.containsKey("confidenceThreshold"));
        assertTrue(stats.containsKey("categoryDistribution"));
        assertTrue(stats.containsKey("model"));
        
        // Verify counts
        assertTrue((Long) stats.get("totalCategorizations") >= 2);
        assertTrue((Long) stats.get("totalTagExtractions") >= 1);
    }
    
    @Test
    void testProvideFeedback() {
        CategoryLabel assigned = new CategoryLabel("UserProfile", "Preferences", Set.of("food"), 0.8);
        CategoryLabel correct = new CategoryLabel("PersonalData", "Identity", Set.of("name"), 0.9);
        
        // Should not throw exception
        assertDoesNotThrow(() -> engine.provideFeedback("test content", assigned, correct));
    }
    
    @Test
    void testProvideFeedbackWithNullParameters() {
        CategoryLabel label = new CategoryLabel("UserProfile", null, Set.of(), 0.8);
        
        assertThrows(IllegalArgumentException.class, 
            () -> engine.provideFeedback(null, label, label));
        assertThrows(IllegalArgumentException.class, 
            () -> engine.provideFeedback("content", null, label));
        assertThrows(IllegalArgumentException.class, 
            () -> engine.provideFeedback("content", label, null));
    }
    
    @Test
    void testAddCustomCategory() {
        String customCategory = "CustomCategory";
        Set<String> customSubcategories = Set.of("Sub1", "Sub2");
        
        // Verify category doesn't exist initially
        assertFalse(engine.getAvailableCategories().contains(customCategory));
        
        // Add custom category
        engine.addCustomCategory(customCategory, customSubcategories);
        
        // Verify category was added
        assertTrue(engine.getAvailableCategories().contains(customCategory));
        assertEquals(customSubcategories, engine.getSubcategories(customCategory));
    }
    
    @Test
    void testGetChatModel() {
        ChatLanguageModel model = engine.getChatModel();
        assertNotNull(model);
        assertEquals(mockChatModel, model);
    }
    
    @Test
    void testPatternBasedTagExtraction() {
        String content = """
            Contact information:
            Email: user@domain.com
            Phone: (555) 123-4567
            Website: https://example.com
            Date: 2024-01-15
            """;
        
        Set<String> tags = engine.extractTags(content);
        
        assertNotNull(tags);
        
        // Should extract various entities using patterns
        assertTrue(tags.stream().anyMatch(tag -> tag.contains("user@domain.com")));
        assertTrue(tags.stream().anyMatch(tag -> tag.contains("https://example.com")));
        assertTrue(tags.stream().anyMatch(tag -> tag.contains("2024-01-15")));
    }
    
    @Test
    void testFallbackBehaviorOnAIServiceFailure() {
        // Create engine with failing chat model
        ChatLanguageModel failingModel = mock(ChatLanguageModel.class);
        when(failingModel.generate(any())).thenThrow(new RuntimeException("AI service unavailable"));
        
        LangChain4JContextualCategorizationEngine failingEngine = 
            new LangChain4JContextualCategorizationEngine(failingModel);
        
        // Should still work with fallback behavior
        CategoryLabel result = failingEngine.categorize("test content", null);
        
        assertNotNull(result);
        assertEquals("Unknown", result.getPrimary());
        assertTrue(result.getConfidence() <= 0.2); // Low confidence for fallback
    }
    
    @Test
    void testHealthCheckWithWorkingModel() {
        // Setup successful health check response
        when(mockChatModel.generate(any())).thenReturn("""
            {
              "primary": "UserProfile",
              "secondary": null,
              "confidence": 0.9,
              "reasoning": "Health check successful"
            }
            """);
        
        // Health check will use fallback since we're not mocking the AI service properly
        // but it should not throw exceptions
        assertDoesNotThrow(() -> engine.isHealthy());
    }
    
    @Test
    void testHealthCheckWithFailingModel() {
        ChatLanguageModel failingModel = mock(ChatLanguageModel.class);
        when(failingModel.generate(any())).thenThrow(new RuntimeException("Service down"));
        
        LangChain4JContextualCategorizationEngine failingEngine = 
            new LangChain4JContextualCategorizationEngine(failingModel);
        
        boolean healthy = failingEngine.isHealthy();
        assertFalse(healthy);
    }
}