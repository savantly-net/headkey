package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.*;
import ai.headkey.memory.enums.Status;
import ai.headkey.memory.interfaces.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic integration test to verify the in-memory memory system works.
 */
public class BasicIntegrationTest {
    
    private InMemoryMemorySystemFactory.MemorySystem memorySystem;
    private InformationIngestionModule ingestionModule;
    
    @BeforeEach
    void setUp() {
        InMemoryMemorySystemFactory factory = InMemoryMemorySystemFactory.forTesting();
        memorySystem = factory.createCompleteSystem();
        ingestionModule = memorySystem.getIngestionModule();
    }
    
    @Test
    void testBasicIngestion() throws Exception {
        // Create a simple memory input
        MemoryInput input = new MemoryInput("test-agent", "Hello world, this is a test memory");
        input.setSource("conversation");
        

        
        // Ingest the memory
        IngestionResult result = ingestionModule.ingest(input);
        
        // Verify the result
        assertNotNull(result);
        assertEquals(Status.SUCCESS, result.getStatus());
        assertNotNull(result.getMemoryId());
        assertEquals("test-agent", result.getAgentId());
        assertTrue(result.isEncodedSuccessfully());
        assertNotNull(result.getCategory());
    }
    
    @Test
    void testMemoryRetrieval() throws Exception {
        // Store a memory first
        MemoryInput input = new MemoryInput("test-agent", "Test content for retrieval");
        input.setSource("conversation");
        IngestionResult result = ingestionModule.ingest(input);
        
        // Retrieve the memory
        MemoryEncodingSystem encodingSystem = memorySystem.getMemoryEncodingSystem();
        var retrievedMemory = encodingSystem.getMemory(result.getMemoryId());
        
        assertTrue(retrievedMemory.isPresent());
        assertEquals("Test content for retrieval", retrievedMemory.get().getContent());
        assertEquals("test-agent", retrievedMemory.get().getAgentId());
    }
    
    @Test
    void testCategorization() throws Exception {
        ContextualCategorizationEngine categorizationEngine = memorySystem.getCategorizationEngine();
        
        // Test categorization
        Metadata metadata = new Metadata();
        CategoryLabel category = categorizationEngine.categorize("I love pizza", metadata);
        
        assertNotNull(category);
        assertNotNull(category.getPrimary());
        assertTrue(category.getConfidence() > 0);
    }
    
    @Test
    void testSystemHealth() {
        // Test that all components are healthy
        assertTrue(memorySystem.isHealthy());
        assertTrue(ingestionModule.isHealthy());
        assertTrue(memorySystem.getCategorizationEngine().isHealthy());
        assertTrue(memorySystem.getMemoryEncodingSystem().isHealthy());
        assertTrue(memorySystem.getBeliefAnalyzer().isHealthy());
        assertTrue(memorySystem.getForgettingAgent().isHealthy());
        assertTrue(memorySystem.getRetrievalEngine().isHealthy());
    }
    
    @Test
    void testDryRun() throws Exception {
        MemoryInput input = new MemoryInput("test-agent", "Dry run test content");
        input.setSource("conversation");
        
        IngestionResult dryRunResult = ingestionModule.dryRunIngest(input);
        
        assertNotNull(dryRunResult);
        assertEquals(Status.SUCCESS, dryRunResult.getStatus());
        assertTrue(dryRunResult.isDryRun());
        assertFalse(dryRunResult.isEncodedSuccessfully());
        assertNotNull(dryRunResult.getCategory());
    }
}