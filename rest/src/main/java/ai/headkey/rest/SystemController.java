package ai.headkey.rest;

import ai.headkey.rest.config.MemorySystemProperties;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * Unified System Controller that delegates to persistence-specific implementations.
 *
 * This controller provides a unified API endpoint that automatically delegates
 * to the appropriate persistence-specific controller based on the current
 * system configuration. It maintains backward compatibility with existing
 * API consumers while supporting multiple persistence backends.
 *
 * The controller automatically detects the active persistence type and
 * delegates requests to:
 * - JpaMemorySystemController for PostgreSQL persistence
 * - ElasticMemorySystemController for Elasticsearch persistence
 *
 * This design follows the Strategy pattern, allowing the system to switch
 * between different persistence implementations without breaking existing
 * API contracts.
 */
@Path("/api/v1/system")
@ApplicationScoped
@Tag(
    name = "System Management",
    description = "Unified Memory System monitoring and configuration"
)
@Produces(MediaType.APPLICATION_JSON)
public class SystemController {

    private static final Logger LOG = Logger.getLogger(SystemController.class);

    @Inject
    MemorySystemProperties properties;

    @Inject
    Instance<JpaMemorySystemController> jpaController;

    @Inject
    Instance<ElasticMemorySystemController> elasticController;

    /**
     * Comprehensive health check for the active memory system.
     *
     * This endpoint automatically delegates to the appropriate persistence-specific
     * health check based on the current system configuration.
     *
     * @return JSON response with health status and diagnostic information
     */
    @GET
    @Path("/health")
    @Operation(
        summary = "Comprehensive memory system health check",
        description = "Returns detailed health information about the active memory system, automatically delegating to the appropriate persistence implementation"
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "Health check completed successfully",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @APIResponse(
                responseCode = "503",
                description = "System is unhealthy or no persistence implementation available",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
        }
    )
    public Response healthCheck() {
        try {
            Response delegatedResponse = delegateToActivePersistence(
                () -> jpaController.get().healthCheck(),
                () -> elasticController.get().healthCheck()
            );

            if (delegatedResponse != null) {
                return delegatedResponse;
            }

            return createNoPersistenceResponse("health check");
        } catch (Exception e) {
            LOG.errorf(e, "Health check delegation failed: %s", e.getMessage());
            return createErrorResponse("Health check failed: " + e.getMessage());
        }
    }

    /**
     * Get comprehensive system configuration information.
     *
     * @return JSON response containing system configuration details
     */
    @GET
    @Path("/config")
    @Operation(
        summary = "Get memory system configuration",
        description = "Returns detailed configuration information, automatically delegating to the appropriate persistence implementation"
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "Configuration retrieved successfully",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
        }
    )
    public Response getConfiguration() {
        try {
            Response delegatedResponse = delegateToActivePersistence(
                () -> jpaController.get().getConfiguration(),
                () -> elasticController.get().getConfiguration()
            );

            if (delegatedResponse != null) {
                return delegatedResponse;
            }

            return createNoPersistenceResponse("configuration");
        } catch (Exception e) {
            LOG.errorf(e, "Configuration delegation failed: %s", e.getMessage());
            return createErrorResponse("Configuration retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Get database/cluster capabilities.
     *
     * @return JSON response containing database or cluster capabilities
     */
    @GET
    @Path("/database/capabilities")
    @Operation(
        summary = "Get database/cluster capabilities",
        description = "Returns information about database or cluster capabilities, automatically delegating to the appropriate persistence implementation"
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "Capabilities retrieved successfully",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
        }
    )
    public Response getDatabaseCapabilities() {
        try {
            Response delegatedResponse = delegateToActivePersistence(
                () -> jpaController.get().getDatabaseCapabilities(),
                () -> elasticController.get().getClusterCapabilities()
            );

            if (delegatedResponse != null) {
                return delegatedResponse;
            }

            return createNoPersistenceResponse("database capabilities");
        } catch (Exception e) {
            LOG.errorf(e, "Database capabilities delegation failed: %s", e.getMessage());
            return createErrorResponse("Database capabilities retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Get comprehensive system statistics.
     *
     * @return JSON response containing system statistics
     */
    @GET
    @Path("/statistics")
    @Operation(
        summary = "Get memory system statistics",
        description = "Returns comprehensive statistics about the active memory system, automatically delegating to the appropriate persistence implementation"
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "Statistics retrieved successfully",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
        }
    )
    public Response getStatistics() {
        try {
            Response delegatedResponse = delegateToActivePersistence(
                () -> jpaController.get().getStatistics(),
                () -> elasticController.get().getStatistics()
            );

            if (delegatedResponse != null) {
                return delegatedResponse;
            }

            return createNoPersistenceResponse("statistics");
        } catch (Exception e) {
            LOG.errorf(e, "Statistics delegation failed: %s", e.getMessage());
            return createErrorResponse("Statistics retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Get information about the active persistence implementation.
     *
     * @return JSON response containing persistence implementation details
     */
    @GET
    @Path("/persistence")
    @Operation(
        summary = "Get active persistence implementation information",
        description = "Returns information about the currently active persistence implementation"
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "Persistence information retrieved successfully",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
        }
    )
    public Response getPersistenceInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            if (!jpaController.isUnsatisfied()) {
                info.put("activePersistence", "postgresql");
                info.put("implementation", "JPA with Hibernate");
                info.put("databaseType", "PostgreSQL");
                info.put("capabilities", Map.of(
                    "vectorSearch", true,
                    "transactional", true,
                    "relational", true,
                    "fullTextSearch", false
                ));
            } else if (!elasticController.isUnsatisfied()) {
                info.put("activePersistence", "elasticsearch");
                info.put("implementation", "Elasticsearch Client");
                info.put("databaseType", "Elasticsearch");
                info.put("capabilities", Map.of(
                    "vectorSearch", true,
                    "transactional", false,
                    "relational", false,
                    "fullTextSearch", true,
                    "distributed", true
                ));
            } else {
                info.put("activePersistence", "none");
                info.put("error", "No persistence implementation available");
            }

            info.put("timestamp", Instant.now());

            return Response.ok(info).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving persistence info: %s", e.getMessage());
            return createErrorResponse("Persistence info retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Delegates to the appropriate active persistence implementation.
     *
     * @param jpaOperation The operation to execute if JPA is active
     * @param elasticOperation The operation to execute if Elasticsearch is active
     * @return Response from the active implementation, or null if none available
     */
    private Response delegateToActivePersistence(
            PersistenceOperation jpaOperation,
            PersistenceOperation elasticOperation) {

        if (!jpaController.isUnsatisfied()) {
            LOG.debug("Delegating to JPA persistence implementation");
            return jpaOperation.execute();
        } else if (!elasticController.isUnsatisfied()) {
            LOG.debug("Delegating to Elasticsearch persistence implementation");
            return elasticOperation.execute();
        }

        LOG.warn("No persistence implementation available for delegation");
        return null;
    }

    private Response createNoPersistenceResponse(String operation) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "No persistence implementation available for " + operation);
        error.put("availablePersistence", "none");
        error.put("timestamp", Instant.now());
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
            .entity(error)
            .build();
    }

    private Response createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", Instant.now());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(error)
            .build();
    }

    /**
     * Functional interface for persistence operations.
     */
    @FunctionalInterface
    private interface PersistenceOperation {
        Response execute();
    }
}
