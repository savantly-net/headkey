package ai.headkey.rest.service;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.IngestionResult;
import ai.headkey.memory.dto.MemoryInput;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.enums.Status;
import ai.headkey.rest.dto.MemoryIngestionRequest;
import ai.headkey.rest.dto.MemoryIngestionResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper utility for converting between REST DTOs and internal memory system DTOs.
 * 
 * This class provides conversion methods to transform external REST API data structures
 * to internal domain objects and vice versa, maintaining separation of concerns between
 * the REST layer and the core memory system.
 */
public class MemoryDtoMapper {
    
    /**
     * Converts a REST API MemoryIngestionRequest to an internal MemoryInput.
     * 
     * @param request The REST request DTO
     * @return The internal MemoryInput DTO
     * @throws IllegalArgumentException if the request is null or invalid
     */
    public MemoryInput toMemoryInput(MemoryIngestionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("MemoryIngestionRequest cannot be null");
        }
        
        if (!request.isValid()) {
            throw new IllegalArgumentException("MemoryIngestionRequest is not valid");
        }
        
        // Convert metadata map to Metadata object
        Metadata metadata = null;
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            metadata = new Metadata();
            for (Map.Entry<String, Object> entry : request.getMetadata().entrySet()) {
                metadata.setProperty(entry.getKey(), entry.getValue());
            }
        }
        
        return new MemoryInput(
            request.getAgentId(),
            request.getContent(),
            request.getSource(),
            request.getTimestamp(),
            metadata
        );
    }
    
    /**
     * Converts an internal IngestionResult to a REST API MemoryIngestionResponse.
     * 
     * @param result The internal ingestion result
     * @return The REST response DTO
     * @throws IllegalArgumentException if the result is null
     */
    public MemoryIngestionResponse toMemoryIngestionResponse(IngestionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("IngestionResult cannot be null");
        }
        
        MemoryIngestionResponse response = new MemoryIngestionResponse();
        
        // Set basic fields
        response.setSuccess(result.getStatus() == Status.SUCCESS);
        response.setMemoryId(result.getMemoryId());
        response.setAgentId(result.getAgentId());
        response.setEncoded(result.isEncoded());
        response.setDryRun(result.isDryRun());
        response.setTimestamp(result.getTimestamp());
        response.setProcessingTimeMs(result.getProcessingTimeMs());
        
        // Convert category
        if (result.getCategory() != null) {
            response.setCategory(toCategoryResponse(result.getCategory()));
        }
        
        // Set updated beliefs
        if (result.getUpdatedBeliefIds() != null) {
            response.setUpdatedBeliefs(result.getUpdatedBeliefIds());
        }
        
        // Set preview data for dry runs
        if (result.getPreviewData() != null) {
            response.setPreviewData(result.getPreviewData());
        }
        
        // Handle error cases
        if (result.getStatus() == Status.ERROR) {
            response.setErrorMessage(result.getErrorMessage());
            
            // Add additional error details if available
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status", result.getStatus().toString());
            if (result.getErrorMessage() != null) {
                errorDetails.put("message", result.getErrorMessage());
            }
            response.setErrorDetails(errorDetails);
        }
        
        return response;
    }
    
    /**
     * Converts an internal CategoryLabel to a REST API CategoryResponse.
     * 
     * @param categoryLabel The internal category label
     * @return The REST category response
     */
    private MemoryIngestionResponse.CategoryResponse toCategoryResponse(CategoryLabel categoryLabel) {
        if (categoryLabel == null) {
            return null;
        }
        
        List<String> tags = null;
        if (categoryLabel.getTags() != null) {
            tags = categoryLabel.getTags().stream()
                    .collect(Collectors.toList());
        }
        
        return new MemoryIngestionResponse.CategoryResponse(
            categoryLabel.getPrimary(),
            categoryLabel.getConfidence(), 
            tags
        );
    }
    
    /**
     * Creates an error response for exceptions.
     * 
     * @param errorMessage The error message
     * @param exception The exception that occurred (optional)
     * @return An error response DTO
     */
    public MemoryIngestionResponse createErrorResponse(String errorMessage, Exception exception) {
        MemoryIngestionResponse response = MemoryIngestionResponse.error(errorMessage);
        
        if (exception != null) {
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("exception_type", exception.getClass().getSimpleName());
            errorDetails.put("exception_message", exception.getMessage());
            
            // Add stack trace for debugging (consider making this configurable)
            if (exception.getStackTrace().length > 0) {
                errorDetails.put("location", 
                    exception.getStackTrace()[0].getClassName() + "." + 
                    exception.getStackTrace()[0].getMethodName() + ":" + 
                    exception.getStackTrace()[0].getLineNumber());
            }
            
            response.setErrorDetails(errorDetails);
        }
        
        return response;
    }
    
    /**
     * Creates a validation error response for invalid requests.
     * 
     * @param validationErrors List of validation error messages
     * @return A validation error response DTO
     */
    public MemoryIngestionResponse createValidationErrorResponse(List<String> validationErrors) {
        String errorMessage = "Validation failed: " + String.join(", ", validationErrors);
        MemoryIngestionResponse response = MemoryIngestionResponse.error(errorMessage);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("validation_errors", validationErrors);
        errorDetails.put("error_type", "VALIDATION_ERROR");
        response.setErrorDetails(errorDetails);
        
        return response;
    }
    
    /**
     * Validates that a MemoryIngestionRequest contains all required fields.
     * 
     * @param request The request to validate
     * @return List of validation error messages (empty if valid)
     */
    public List<String> validateRequest(MemoryIngestionRequest request) {
        List<String> errors = new java.util.ArrayList<>();
        
        if (request == null) {
            errors.add("Request cannot be null");
            return errors;
        }
        
        if (request.getAgentId() == null || request.getAgentId().trim().isEmpty()) {
            errors.add("Agent ID is required and cannot be empty");
        } else if (request.getAgentId().length() > 100) {
            errors.add("Agent ID cannot exceed 100 characters");
        }
        
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            errors.add("Content is required and cannot be empty");
        } else if (request.getContent().length() > 10000) {
            errors.add("Content cannot exceed 10000 characters");
        }
        
        // Validate source if provided
        if (request.getSource() != null && request.getSource().length() > 255) {
            errors.add("Source cannot exceed 255 characters");
        }
        
        return errors;
    }
}