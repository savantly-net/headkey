package ai.headkey.rest;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.IngestionResult;
import ai.headkey.memory.dto.MemoryInput;
import ai.headkey.memory.enums.Status;
import ai.headkey.memory.exceptions.InvalidInputException;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.rest.dto.MemoryIngestionRequest;
import ai.headkey.rest.dto.MemoryIngestionResponse;
import ai.headkey.rest.service.MemoryDtoMapper;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MemoryIngestionController.
 * 
 * Tests cover all REST endpoints including success cases, validation failures,
 * and error handling scenarios. Follows TDD principles with comprehensive
 * test coverage for both happy path and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class MemoryIngestionControllerTest {
    
    @Mock
    InformationIngestionModule ingestionModule;
    
    private MemoryIngestionController controller;
    private MemoryDtoMapper mapper;
    
    @BeforeEach
    void setUp() {
        mapper = new MemoryDtoMapper();
        controller = new MemoryIngestionController();
        // Manually inject mocked dependencies for testing
        controller.ingestionModule = ingestionModule;
        controller.mapper = mapper;
    }
    
    @Test
    void testIngestMemory_Success() throws Exception {
        // Given
        MemoryIngestionRequest request = new MemoryIngestionRequest("agent-123", "Test memory content");
        
        CategoryLabel category = new CategoryLabel("personal", null, new HashSet<>(Arrays.asList("fact", "statement")), 0.85);
        IngestionResult mockResult = new IngestionResult("memory-456", category, true);
        mockResult.setStatus(Status.SUCCESS);
        mockResult.setAgentId("agent-123");
        mockResult.setTimestamp(Instant.now());
        
        when(ingestionModule.ingest(any(MemoryInput.class))).thenReturn(mockResult);
        
        // When
        Response response = controller.ingestMemory(request);
        
        // Then
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof MemoryIngestionResponse);
        
        MemoryIngestionResponse responseEntity = (MemoryIngestionResponse) response.getEntity();
        assertTrue(responseEntity.isSuccess());
        assertEquals("memory-456", responseEntity.getMemoryId());
        assertEquals("agent-123", responseEntity.getAgentId());
        assertEquals("personal", responseEntity.getCategory().getName());
        assertEquals(0.85, responseEntity.getCategory().getConfidence());
        assertTrue(responseEntity.getEncoded());
        
        // Verify the ingestion module was called with correct parameters
        ArgumentCaptor<MemoryInput> inputCaptor = ArgumentCaptor.forClass(MemoryInput.class);
        verify(ingestionModule).ingest(inputCaptor.capture());
        
        MemoryInput capturedInput = inputCaptor.getValue();
        assertEquals("agent-123", capturedInput.getAgentId());
        assertEquals("Test memory content", capturedInput.getContent());
    }
    
    @Test
    void testIngestMemory_DryRun() throws Exception {
        // Given
        MemoryIngestionRequest request = new MemoryIngestionRequest("agent-123", "Test content for dry run");
        request.setDryRun(true);
        
        CategoryLabel category = new CategoryLabel("test", null, new HashSet<>(Arrays.asList("preview")), 0.75);
        IngestionResult mockResult = new IngestionResult();
        mockResult.setCategory(category);
        mockResult.setStatus(Status.SUCCESS);
        mockResult.setDryRun(true);
        mockResult.setEncoded(false);
        
        Map<String, Object> previewData = new HashMap<>();
        previewData.put("category_confidence", 0.75);
        previewData.put("estimated_storage_size", "145 bytes");
        mockResult.setPreviewData(previewData);
        
        when(ingestionModule.dryRunIngest(any(MemoryInput.class))).thenReturn(mockResult);
        
        // When
        Response response = controller.ingestMemory(request);
        
        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        MemoryIngestionResponse responseEntity = (MemoryIngestionResponse) response.getEntity();
        assertTrue(responseEntity.isSuccess());
        assertTrue(responseEntity.getDryRun());
        assertFalse(responseEntity.getEncoded());
        assertNull(responseEntity.getMemoryId());
        assertNotNull(responseEntity.getPreviewData());
        assertEquals("test", responseEntity.getCategory().getName());
        
        verify(ingestionModule).dryRunIngest(any(MemoryInput.class));
        verify(ingestionModule, never()).ingest(any(MemoryInput.class));
    }
    
    @Test
    void testIngestMemory_ValidationFailure() throws Exception {
        // Given
        MemoryIngestionRequest request = new MemoryIngestionRequest();
        request.setAgentId("");  // Invalid - empty agent ID
        request.setContent("");  // Invalid - empty content
        
        // When
        Response response = controller.ingestMemory(request);
        
        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        MemoryIngestionResponse responseEntity = (MemoryIngestionResponse) response.getEntity();
        assertFalse(responseEntity.isSuccess());
        assertNotNull(responseEntity.getErrorMessage());
        assertTrue(responseEntity.getErrorMessage().contains("Validation failed"));
        assertNotNull(responseEntity.getErrorDetails());
        assertTrue(responseEntity.getErrorDetails().containsKey("validation_errors"));
        
        verify(ingestionModule, never()).ingest(any(MemoryInput.class));
        verify(ingestionModule, never()).dryRunIngest(any(MemoryInput.class));
    }
    
    @Test
    void testIngestMemory_InvalidInputException() throws Exception {
        // Given
        MemoryIngestionRequest request = new MemoryIngestionRequest("agent-123", "Test content");
        
        when(ingestionModule.ingest(any(MemoryInput.class)))
            .thenThrow(new InvalidInputException("Content contains invalid characters"));
        
        // When
        Response response = controller.ingestMemory(request);
        
        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        MemoryIngestionResponse responseEntity = (MemoryIngestionResponse) response.getEntity();
        assertFalse(responseEntity.isSuccess());
        assertTrue(responseEntity.getErrorMessage().contains("Input validation failed"));
        assertTrue(responseEntity.getErrorMessage().contains("invalid characters"));
    }
    
    @Test
    void testIngestMemory_InternalServerError() throws Exception {
        // Given
        MemoryIngestionRequest request = new MemoryIngestionRequest("agent-123", "Test content");
        
        when(ingestionModule.ingest(any(MemoryInput.class)))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // When
        Response response = controller.ingestMemory(request);
        
        // Then
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        
        MemoryIngestionResponse responseEntity = (MemoryIngestionResponse) response.getEntity();
        assertFalse(responseEntity.isSuccess());
        assertTrue(responseEntity.getErrorMessage().contains("Internal server error"));
        assertNotNull(responseEntity.getErrorDetails());
    }
    
    @Test
    void testDryRunIngest_Success() throws Exception {
        // Given
        MemoryIngestionRequest request = new MemoryIngestionRequest("agent-456", "Dry run test content");
        
        CategoryLabel category = new CategoryLabel("analysis", null, new HashSet<>(Arrays.asList("test", "preview")), 0.92);
        IngestionResult mockResult = new IngestionResult();
        mockResult.setCategory(category);
        mockResult.setStatus(Status.SUCCESS);
        mockResult.setDryRun(true);
        mockResult.setEncoded(false);
        
        when(ingestionModule.dryRunIngest(any(MemoryInput.class))).thenReturn(mockResult);
        
        // When
        Response response = controller.dryRunIngest(request);
        
        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        MemoryIngestionResponse responseEntity = (MemoryIngestionResponse) response.getEntity();
        assertTrue(responseEntity.isSuccess());
        assertTrue(responseEntity.getDryRun());
        assertFalse(responseEntity.getEncoded());
        assertEquals("analysis", responseEntity.getCategory().getName());
        assertEquals(0.92, responseEntity.getCategory().getConfidence());
        
        verify(ingestionModule).dryRunIngest(any(MemoryInput.class));
        verify(ingestionModule, never()).ingest(any(MemoryInput.class));
    }
    
    @Test
    void testGetStatistics_Success() {
        // Given
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("total_memories_ingested", 1234);
        mockStats.put("success_rate", 0.98);
        mockStats.put("average_processing_time_ms", 125.5);
        mockStats.put("most_common_category", "personal");
        
        when(ingestionModule.getIngestionStatistics()).thenReturn(mockStats);
        
        // When
        Response response = controller.getStatistics();
        
        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseEntity = (Map<String, Object>) response.getEntity();
        assertEquals(1234, responseEntity.get("total_memories_ingested"));
        assertEquals(0.98, responseEntity.get("success_rate"));
        assertEquals(125.5, responseEntity.get("average_processing_time_ms"));
        assertEquals("personal", responseEntity.get("most_common_category"));
        
        verify(ingestionModule).getIngestionStatistics();
    }
    
    @Test
    void testGetStatistics_Error() {
        // Given
        when(ingestionModule.getIngestionStatistics())
            .thenThrow(new RuntimeException("Statistics service unavailable"));
        
        // When
        Response response = controller.getStatistics();
        
        // Then
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseEntity = (Map<String, Object>) response.getEntity();
        assertTrue(responseEntity.containsKey("error"));
        assertTrue(responseEntity.get("error").toString().contains("Statistics service unavailable"));
    }
    
    @Test
    void testHealthCheck_Healthy() {
        // Given
        when(ingestionModule.isHealthy()).thenReturn(true);
        
        // When
        Response response = controller.healthCheck();
        
        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseEntity = (Map<String, Object>) response.getEntity();
        assertTrue((Boolean) responseEntity.get("healthy"));
        assertEquals("UP", responseEntity.get("status"));
        assertEquals("memory-ingestion", responseEntity.get("service"));
        assertNotNull(responseEntity.get("timestamp"));
        
        verify(ingestionModule).isHealthy();
    }
    
    @Test
    void testHealthCheck_Unhealthy() {
        // Given
        when(ingestionModule.isHealthy()).thenReturn(false);
        
        // When
        Response response = controller.healthCheck();
        
        // Then
        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseEntity = (Map<String, Object>) response.getEntity();
        assertFalse((Boolean) responseEntity.get("healthy"));
        assertEquals("DOWN", responseEntity.get("status"));
        assertTrue(responseEntity.get("message").toString().contains("dependencies are unhealthy"));
    }
    
    @Test
    void testHealthCheck_Exception() {
        // Given
        when(ingestionModule.isHealthy()).thenThrow(new RuntimeException("Health check failed"));
        
        // When
        Response response = controller.healthCheck();
        
        // Then
        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseEntity = (Map<String, Object>) response.getEntity();
        assertFalse((Boolean) responseEntity.get("healthy"));
        assertEquals("DOWN", responseEntity.get("status"));
        assertTrue(responseEntity.get("error").toString().contains("Health check failed"));
    }
    
    @Test
    void testValidateInput_Valid() throws Exception {
        // Given
        MemoryIngestionRequest request = new MemoryIngestionRequest("agent-123", "Valid content");
        request.setSource("test");
        
        doNothing().when(ingestionModule).validateInput(any(MemoryInput.class));
        
        // When
        Response response = controller.validateInput(request);
        
        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseEntity = (Map<String, Object>) response.getEntity();
        assertTrue((Boolean) responseEntity.get("valid"));
        assertEquals("Input is valid", responseEntity.get("message"));
        
        verify(ingestionModule).validateInput(any(MemoryInput.class));
    }
    
    @Test
    void testValidateInput_Invalid() throws Exception {
        // Given
        MemoryIngestionRequest request = new MemoryIngestionRequest("agent-123", "Invalid content");
        
        doThrow(new InvalidInputException("Content contains forbidden words"))
            .when(ingestionModule).validateInput(any(MemoryInput.class));
        
        // When
        Response response = controller.validateInput(request);
        
        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseEntity = (Map<String, Object>) response.getEntity();
        assertFalse((Boolean) responseEntity.get("valid"));
        assertTrue(responseEntity.get("error").toString().contains("forbidden words"));
    }
    
    @Test
    void testValidateInput_RequestValidationFailure() throws Exception {
        // Given - request with missing required fields
        MemoryIngestionRequest request = new MemoryIngestionRequest();
        
        // When
        Response response = controller.validateInput(request);
        
        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseEntity = (Map<String, Object>) response.getEntity();
        assertFalse((Boolean) responseEntity.get("valid"));
        assertTrue(responseEntity.containsKey("errors"));
        
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) responseEntity.get("errors");
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.contains("Agent ID")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("Content")));
        
        // Should not call the ingestion module for invalid requests
        verify(ingestionModule, never()).validateInput(any(MemoryInput.class));
    }
    
    @Test
    void testIngestMemory_WithMetadata() throws Exception {
        // Given
        MemoryIngestionRequest request = new MemoryIngestionRequest("agent-123", "Content with metadata");
        request.setSource("user_input");
        request.setTimestamp(Instant.now().minusSeconds(300));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("importance", "high");
        metadata.put("tags", Arrays.asList("urgent", "personal"));
        metadata.put("confidence", 0.95);
        request.setMetadata(metadata);
        
        CategoryLabel category = new CategoryLabel("important", null, new HashSet<>(Arrays.asList("high-priority")), 0.90);
        IngestionResult mockResult = new IngestionResult("memory-789", category, true);
        mockResult.setStatus(Status.SUCCESS);
        mockResult.setUpdatedBeliefIds(Arrays.asList("belief-1", "belief-2"));
        
        when(ingestionModule.ingest(any(MemoryInput.class))).thenReturn(mockResult);
        
        // When
        Response response = controller.ingestMemory(request);
        
        // Then
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        
        MemoryIngestionResponse responseEntity = (MemoryIngestionResponse) response.getEntity();
        assertTrue(responseEntity.isSuccess());
        assertEquals("memory-789", responseEntity.getMemoryId());
        assertEquals(2, responseEntity.getUpdatedBeliefs().size());
        assertTrue(responseEntity.getUpdatedBeliefs().contains("belief-1"));
        assertTrue(responseEntity.getUpdatedBeliefs().contains("belief-2"));
        
        // Verify metadata was passed correctly
        ArgumentCaptor<MemoryInput> inputCaptor = ArgumentCaptor.forClass(MemoryInput.class);
        verify(ingestionModule).ingest(inputCaptor.capture());
        
        MemoryInput capturedInput = inputCaptor.getValue();
        assertNotNull(capturedInput.getMetadata());
        assertEquals("high", capturedInput.getMetadata().getProperty("importance"));
        assertEquals("user_input", capturedInput.getSource());
        assertNotNull(capturedInput.getTimestamp());
    }
    
    @Test
    void testIngestMemory_LongContent() throws Exception {
        // Given - content that exceeds maximum length
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 10001; i++) {
            longContent.append("a");
        }
        
        MemoryIngestionRequest request = new MemoryIngestionRequest("agent-123", longContent.toString());
        
        // When
        Response response = controller.ingestMemory(request);
        
        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        MemoryIngestionResponse responseEntity = (MemoryIngestionResponse) response.getEntity();
        assertFalse(responseEntity.isSuccess());
        assertTrue(responseEntity.getErrorMessage().contains("Validation failed"));
        
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) responseEntity.getErrorDetails().get("validation_errors");
        assertTrue(errors.stream().anyMatch(error -> error.contains("exceed") && error.contains("10000")));
        
        verify(ingestionModule, never()).ingest(any(MemoryInput.class));
    }
}