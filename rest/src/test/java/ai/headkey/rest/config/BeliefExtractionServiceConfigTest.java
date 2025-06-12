package ai.headkey.rest.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ai.headkey.memory.implementations.SimplePatternBeliefExtractionService;
import ai.headkey.memory.langchain4j.LangChain4JBeliefExtractionService;
import ai.headkey.memory.spi.BeliefExtractionService;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

/**
 * Unit tests for BeliefExtractionService configuration in MemorySystemConfig.
 * 
 * These tests verify that the correct BeliefExtractionService implementation
 * is created based on available dependencies and environment configuration.
 */
@ExtendWith(MockitoExtension.class)
class BeliefExtractionServiceConfigTest {

    @Mock
    private Instance<ChatModel> mockChatModelInstance;
    
    @Mock
    private ChatModel mockChatModel;
    
    private MemorySystemConfig config;
    
    @BeforeEach
    void setUp() {
        config = new MemorySystemConfig();
        // Use reflection to set the chatModel field since it's package-private
        try {
            var field = MemorySystemConfig.class.getDeclaredField("chatModel");
            field.setAccessible(true);
            field.set(config, mockChatModelInstance);
        } catch (Exception e) {
            fail("Failed to set chatModel field: " + e.getMessage());
        }
    }
    
    @Test
    void testBeliefExtractionServiceWithChatModelAndApiKey() {
        // Given: ChatModel is available and API key is set
        when(mockChatModelInstance.isUnsatisfied()).thenReturn(false);
        when(mockChatModelInstance.get()).thenReturn(mockChatModel);
        
        // Set environment variable for this test
        String originalApiKey = System.getenv("OPENAI_API_KEY");
        try {
            // We can't actually set environment variables in Java tests,
            // but we can test the fallback behavior
            
            // When: Creating belief extraction service
            BeliefExtractionService service = config.beliefExtractionService();
            
            // Then: Should get the appropriate service based on availability
            assertNotNull(service);
            // Service info should be accessible
            assertTrue(service.getServiceInfo().containsKey("serviceType"));
            
        } finally {
            // Clean up would go here if we could modify env vars
        }
    }
    
    @Test
    void testBeliefExtractionServiceWithoutChatModel() {
        // Given: ChatModel is not available
        when(mockChatModelInstance.isUnsatisfied()).thenReturn(true);
        
        // When: Creating belief extraction service
        BeliefExtractionService service = config.beliefExtractionService();
        
        // Then: Should fallback to SimplePatternBeliefExtractionService
        assertNotNull(service);
        assertInstanceOf(SimplePatternBeliefExtractionService.class, service);
        
        // Verify service info
        Map<String, Object> info = service.getServiceInfo();
        assertEquals("SimplePatternBeliefExtractionService", info.get("serviceType"));
    }
    
    @Test
    void testBeliefExtractionServiceWithChatModelException() {
        // Given: ChatModel is available but throws exception
        when(mockChatModelInstance.isUnsatisfied()).thenReturn(false);
        when(mockChatModelInstance.get()).thenThrow(new RuntimeException("ChatModel initialization failed"));
        
        // When: Creating belief extraction service
        BeliefExtractionService service = config.beliefExtractionService();
        
        // Then: Should fallback to SimplePatternBeliefExtractionService
        assertNotNull(service);
        assertInstanceOf(SimplePatternBeliefExtractionService.class, service);
    }
    
    @Test
    void testBeliefExtractionServiceHealthCheck() {
        // Given: Fallback service is created
        when(mockChatModelInstance.isUnsatisfied()).thenReturn(true);
        
        // When: Creating belief extraction service
        BeliefExtractionService service = config.beliefExtractionService();
        
        // Then: Service should be healthy
        assertTrue(service.isHealthy());
    }
    
    @Test
    void testBeliefExtractionServiceApiCompatibility() {
        // Given: Any service implementation
        when(mockChatModelInstance.isUnsatisfied()).thenReturn(true);
        BeliefExtractionService service = config.beliefExtractionService();
        
        // When: Testing API compatibility
        // Then: Should support all required methods
        assertDoesNotThrow(() -> {
            service.extractBeliefs("test content", "agent1", null);
        });
        
        assertDoesNotThrow(() -> {
            service.calculateSimilarity("statement1", "statement2");
        });
        
        assertDoesNotThrow(() -> {
            service.areConflicting("statement1", "statement2", "cat1", "cat2");
        });
        
        assertDoesNotThrow(() -> {
            service.extractCategory("test statement");
        });
        
        assertDoesNotThrow(() -> {
            service.calculateConfidence("content", "statement", null);
        });
    }
    
    @Test
    void testBeliefStorageServiceCreation() {
        // When: Creating belief storage service
        var storageService = config.beliefStorageService();
        
        // Then: Should create valid storage service
        assertNotNull(storageService);
        assertTrue(storageService.getClass().getSimpleName().contains("BeliefStorageService"));
    }
    
    @Test
    void testBeliefReinforcementConflictAnalyzerCreation() {
        // Given: Mock services
        when(mockChatModelInstance.isUnsatisfied()).thenReturn(true);
        BeliefExtractionService extractionService = config.beliefExtractionService();
        var storageService = config.beliefStorageService();
        
        // When: Creating belief reinforcement conflict analyzer
        var analyzer = config.beliefReinforcementConflictAnalyzer(extractionService, storageService);
        
        // Then: Should create valid analyzer
        assertNotNull(analyzer);
        assertTrue(analyzer.getClass().getSimpleName().contains("BeliefReinforcementConflictAnalyzer"));
    }
    
    @Test
    void testServiceIntegration() {
        // Given: Complete service configuration
        when(mockChatModelInstance.isUnsatisfied()).thenReturn(true);
        
        // When: Creating integrated services
        BeliefExtractionService extractionService = config.beliefExtractionService();
        var storageService = config.beliefStorageService();
        var analyzer = config.beliefReinforcementConflictAnalyzer(extractionService, storageService);
        
        // Then: All services should be compatible
        assertNotNull(extractionService);
        assertNotNull(storageService);
        assertNotNull(analyzer);
        
        // Services should have proper info
        assertFalse(extractionService.getServiceInfo().isEmpty());
    }
}