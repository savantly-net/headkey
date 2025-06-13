package ai.headkey.rest;

import ai.headkey.memory.dto.IngestionResult;
import ai.headkey.memory.dto.MemoryInput;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.exceptions.InvalidInputException;
import ai.headkey.memory.implementations.InMemoryMemorySystemFactory;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.rest.dto.MemoryIngestionRequest;
import ai.headkey.rest.dto.MemoryIngestionResponse;
import ai.headkey.rest.dto.MemorySearchRequest;
import ai.headkey.rest.dto.MemorySearchResponse;
import ai.headkey.rest.service.MemoryDtoMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * REST Controller for Memory Ingestion operations.
 *
 * This controller exposes the InformationIngestionModule functionality through
 * RESTful endpoints, providing a clean API for clients to ingest memories,
 * perform dry runs, and check system health.
 *
 * All endpoints follow REST best practices and return standardized JSON responses
 * with appropriate HTTP status codes.
 */
@Path("/api/v1/memory")
@ApplicationScoped
@Tag(
    name = "Memory Ingestion",
    description = "Operations for ingesting and managing memories"
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MemoryIngestionController {

    private static final Logger LOG = Logger.getLogger(
        MemoryIngestionController.class
    );

    @Inject
    InformationIngestionModule ingestionModule;

    @Inject
    MemoryEncodingSystem memoryEncodingSystem;

    @Inject
    MemoryDtoMapper mapper;

    /**
     * Default constructor for CDI.
     */
    public MemoryIngestionController() {
        LOG.info(
            "MemoryIngestionController instantiated - dependencies will be injected by CDI"
        );
    }

    /**
     * Ingests a new memory into the system.
     *
     * This endpoint processes the provided content through the complete ingestion pipeline:
     * validation, categorization, encoding, storage, and belief updates.
     */
    @POST
    @Path("/ingest")
    @Operation(
        summary = "Ingest a new memory",
        description = "Processes and stores new information in the memory system. " +
        "The content will be categorized, encoded, and stored with belief updates."
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "201",
                description = "Memory successfully ingested",
                content = @Content(
                    schema = @Schema(
                        implementation = MemoryIngestionResponse.class
                    )
                )
            ),
            @APIResponse(
                responseCode = "400",
                description = "Invalid input data",
                content = @Content(
                    schema = @Schema(
                        implementation = MemoryIngestionResponse.class
                    )
                )
            ),
            @APIResponse(
                responseCode = "500",
                description = "Internal server error",
                content = @Content(
                    schema = @Schema(
                        implementation = MemoryIngestionResponse.class
                    )
                )
            ),
        }
    )
    public Response ingestMemory(@Valid MemoryIngestionRequest request) {
        Instant startTime = Instant.now();
        LOG.infof("Ingesting memory for agent: %s", request.getAgentId());

        try {
            // Validate the request
            List<String> validationErrors = mapper.validateRequest(request);
            if (!validationErrors.isEmpty()) {
                LOG.warnf(
                    "Validation failed for request: %s",
                    validationErrors
                );
                MemoryIngestionResponse errorResponse =
                    mapper.createValidationErrorResponse(validationErrors);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
            }

            // Convert to internal DTO
            MemoryInput memoryInput = mapper.toMemoryInput(request);

            // Perform ingestion
            IngestionResult result;
            if (request.isDryRunEffective()) {
                result = ingestionModule.dryRunIngest(memoryInput);
                LOG.infof(
                    "Dry run completed for agent: %s",
                    request.getAgentId()
                );
            } else {
                result = ingestionModule.ingest(memoryInput);
                LOG.infof(
                    "Memory ingested successfully with ID: %s",
                    result.getMemoryId()
                );
            }

            // Set processing time
            result.setProcessingTime(startTime);

            // Convert to response DTO
            MemoryIngestionResponse response = mapper.toMemoryIngestionResponse(
                result
            );

            // Return appropriate status code
            Response.Status status = request.isDryRunEffective()
                ? Response.Status.OK
                : Response.Status.CREATED;

            return Response.status(status).entity(response).build();
        } catch (InvalidInputException e) {
            LOG.errorf(
                e,
                "Invalid input for memory ingestion: %s",
                e.getMessage()
            );
            MemoryIngestionResponse errorResponse = mapper.createErrorResponse(
                "Input validation failed: " + e.getMessage(),
                e
            );
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
        } catch (IllegalArgumentException e) {
            LOG.errorf(
                e,
                "Illegal argument in memory ingestion: %s",
                e.getMessage()
            );
            MemoryIngestionResponse errorResponse = mapper.createErrorResponse(
                "Invalid request: " + e.getMessage(),
                e
            );
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Unexpected error during memory ingestion: %s",
                e.getMessage()
            );
            MemoryIngestionResponse errorResponse = mapper.createErrorResponse(
                "Internal server error occurred during ingestion",
                e
            );
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
        }
    }

    /**
     * Performs a dry run of memory ingestion without storing data.
     *
     * This endpoint allows clients to preview the categorization and processing
     * results without actually storing the memory.
     */
    @POST
    @Path("/dry-run")
    @Operation(
        summary = "Perform a dry run of memory ingestion",
        description = "Processes content through validation and categorization without storing. " +
        "Useful for previewing results and testing input validity."
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "Dry run completed successfully",
                content = @Content(
                    schema = @Schema(
                        implementation = MemoryIngestionResponse.class
                    )
                )
            ),
            @APIResponse(
                responseCode = "400",
                description = "Invalid input data",
                content = @Content(
                    schema = @Schema(
                        implementation = MemoryIngestionResponse.class
                    )
                )
            ),
            @APIResponse(
                responseCode = "500",
                description = "Internal server error",
                content = @Content(
                    schema = @Schema(
                        implementation = MemoryIngestionResponse.class
                    )
                )
            ),
        }
    )
    public Response dryRunIngest(@Valid MemoryIngestionRequest request) {
        Instant startTime = Instant.now();
        LOG.infof("Performing dry run for agent: %s", request.getAgentId());

        try {
            // Validate the request
            List<String> validationErrors = mapper.validateRequest(request);
            if (!validationErrors.isEmpty()) {
                LOG.warnf(
                    "Validation failed for dry run: %s",
                    validationErrors
                );
                MemoryIngestionResponse errorResponse =
                    mapper.createValidationErrorResponse(validationErrors);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
            }

            // Convert to internal DTO
            MemoryInput memoryInput = mapper.toMemoryInput(request);

            // Perform dry run
            IngestionResult result = ingestionModule.dryRunIngest(memoryInput);
            result.setProcessingTime(startTime);

            LOG.infof(
                "Dry run completed successfully for agent: %s",
                request.getAgentId()
            );

            // Convert to response DTO
            MemoryIngestionResponse response = mapper.toMemoryIngestionResponse(
                result
            );

            return Response.ok(response).build();
        } catch (InvalidInputException e) {
            LOG.errorf(e, "Invalid input for dry run: %s", e.getMessage());
            MemoryIngestionResponse errorResponse = mapper.createErrorResponse(
                "Input validation failed: " + e.getMessage(),
                e
            );
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Unexpected error during dry run: %s",
                e.getMessage()
            );
            MemoryIngestionResponse errorResponse = mapper.createErrorResponse(
                "Internal server error occurred during dry run",
                e
            );
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
        }
    }

    /**
     * Gets statistics about the ingestion module's performance and operations.
     */
    @GET
    @Path("/statistics")
    @Operation(
        summary = "Get ingestion statistics",
        description = "Returns metrics and statistics about memory ingestion operations"
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "Statistics retrieved successfully",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @APIResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    public Response getStatistics() {
        try {
            Map<String, Object> statistics =
                ingestionModule.getIngestionStatistics();
            LOG.debug("Retrieved ingestion statistics");
            return Response.ok(statistics).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving statistics: %s", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(
                "error",
                "Failed to retrieve statistics: " + e.getMessage()
            );
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
        }
    }

    /**
     * Health check endpoint for the memory ingestion system.
     */
    @GET
    @Path("/health")
    @Operation(
        summary = "Check system health",
        description = "Verifies that the memory ingestion system and its dependencies are healthy"
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "System is healthy",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @APIResponse(
                responseCode = "503",
                description = "System is unhealthy",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
        }
    )
    public Response healthCheck() {
        try {
            boolean healthy = ingestionModule.isHealthy();
            Map<String, Object> healthStatus = new HashMap<>();
            healthStatus.put("healthy", healthy);
            healthStatus.put("timestamp", Instant.now());
            healthStatus.put("service", "memory-ingestion");

            if (healthy) {
                healthStatus.put("status", "UP");
                LOG.debug("Health check passed");
                return Response.ok(healthStatus).build();
            } else {
                healthStatus.put("status", "DOWN");
                healthStatus.put(
                    "message",
                    "One or more dependencies are unhealthy"
                );
                LOG.warn("Health check failed");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(healthStatus)
                    .build();
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error during health check: %s", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("healthy", false);
            errorResponse.put("status", "DOWN");
            errorResponse.put(
                "error",
                "Health check failed: " + e.getMessage()
            );
            errorResponse.put("timestamp", Instant.now());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(errorResponse)
                .build();
        }
    }

    /**
     * Validates input without processing it.
     */
    @POST
    @Path("/validate")
    @Operation(
        summary = "Validate memory input",
        description = "Validates the input data without processing or storing it"
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "Input is valid",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @APIResponse(
                responseCode = "400",
                description = "Input is invalid",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
        }
    )
    public Response validateInput(@Valid MemoryIngestionRequest request) {
        try {
            List<String> validationErrors = mapper.validateRequest(request);
            Map<String, Object> validationResult = new HashMap<>();

            if (validationErrors.isEmpty()) {
                // Additional validation using the ingestion module
                MemoryInput memoryInput = mapper.toMemoryInput(request);
                ingestionModule.validateInput(memoryInput);

                validationResult.put("valid", true);
                validationResult.put("message", "Input is valid");
                LOG.debugf(
                    "Input validation passed for agent: %s",
                    request.getAgentId()
                );
                return Response.ok(validationResult).build();
            } else {
                validationResult.put("valid", false);
                validationResult.put("errors", validationErrors);
                LOG.infof("Input validation failed: %s", validationErrors);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(validationResult)
                    .build();
            }
        } catch (InvalidInputException e) {
            Map<String, Object> validationResult = new HashMap<>();
            validationResult.put("valid", false);
            validationResult.put("error", e.getMessage());
            LOG.infof("Input validation failed: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(validationResult)
                .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error during input validation: %s", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("error", "Validation failed: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
        }
    }

    /**
     * Searches for memories using similarity search.
     *
     * This endpoint allows clients to search for stored memories that are
     * similar to the provided query content. It uses the underlying memory
     * encoding system's similarity search capabilities.
     */
    @POST
    @Path("/search")
    @Operation(
        summary = "Search memories by similarity",
        description = "Searches for memories similar to the provided query content using " +
        "semantic similarity or text matching depending on the configured strategy."
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "Search completed successfully",
                content = @Content(
                    schema = @Schema(
                        implementation = MemorySearchResponse.class
                    )
                )
            ),
            @APIResponse(
                responseCode = "400",
                description = "Invalid search parameters",
                content = @Content(
                    schema = @Schema(
                        implementation = MemorySearchResponse.class
                    )
                )
            ),
            @APIResponse(
                responseCode = "500",
                description = "Internal server error",
                content = @Content(
                    schema = @Schema(
                        implementation = MemorySearchResponse.class
                    )
                )
            ),
        }
    )
    public Response searchMemories(@Valid MemorySearchRequest request) {
        Instant startTime = Instant.now();
        LOG.infof(
            "Searching memories for agent: %s, query: %s",
            request.getAgentId(),
            request.getQuery()
        );

        try {
            // Validate the request
            if (!request.isValid()) {
                LOG.warnf("Invalid search request: %s", request);
                MemorySearchResponse errorResponse = new MemorySearchResponse(
                    "Invalid search parameters",
                    request.getQuery(),
                    request.getAgentId()
                );
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
            }

            // Perform the search using the memory encoding system
            List<MemoryRecord> memories = memoryEncodingSystem.searchSimilar(
                request.getQuery(),
                request.getLimit(),
                request.getAgentId()
            );

            // Convert to response format
            MemorySearchResponse response = new MemorySearchResponse(
                new ArrayList<>(),
                request.getQuery(),
                request.getAgentId()
            );

            // Add search results with relevance scores
            // Note: The MemoryEncodingSystem doesn't return relevance scores directly,
            // so we'll use a placeholder scoring mechanism for now
            for (int i = 0; i < memories.size(); i++) {
                MemoryRecord memory = memories.get(i);
                // Calculate a simple relevance score based on position (most relevant first)
                double relevanceScore = Math.max(0.1, 1.0 - (i * 0.1));
                response.addResult(memory, relevanceScore);
            }

            // Set processing time and metadata
            response.setProcessingTime(startTime);
            response.setMessage(
                String.format(
                    "Found %d memories matching query",
                    memories.size()
                )
            );

            // Add search metadata
            Map<String, Object> searchMetadata = new HashMap<>();
            searchMetadata.put(
                "searchStrategy",
                memoryEncodingSystem.getClass().getSimpleName()
            );
            searchMetadata.put("requestedLimit", request.getLimit());
            searchMetadata.put("actualResults", memories.size());
            if (request.getSimilarityThreshold() != null) {
                searchMetadata.put(
                    "similarityThreshold",
                    request.getSimilarityThreshold()
                );
            }
            response.setSearchMetadata(searchMetadata);

            LOG.infof(
                "Search completed successfully for agent: %s, found %d memories",
                request.getAgentId(),
                memories.size()
            );

            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            LOG.errorf(e, "Invalid search parameters: %s", e.getMessage());
            MemorySearchResponse errorResponse = new MemorySearchResponse(
                "Invalid search parameters: " + e.getMessage(),
                request.getQuery(),
                request.getAgentId()
            );
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Unexpected error during memory search: %s",
                e.getMessage()
            );
            MemorySearchResponse errorResponse = new MemorySearchResponse(
                "Internal server error occurred during search",
                request.getQuery(),
                request.getAgentId()
            );
            errorResponse.setProcessingTime(startTime);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
        }
    }
}
