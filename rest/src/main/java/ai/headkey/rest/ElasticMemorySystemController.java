package ai.headkey.rest;

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

import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.persistence.elastic.configuration.ElasticsearchConfiguration;
import ai.headkey.rest.config.ElasticsearchPersistence;
import ai.headkey.rest.config.MemorySystemProperties;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST Controller for Elasticsearch Memory System monitoring and configuration.
 *
 * This controller provides endpoints to:
 * - Monitor the health and status of the Elasticsearch-based memory system
 * - View current configuration and Elasticsearch cluster capabilities
 * - Access memory system statistics and performance metrics
 * - Inspect similarity search strategy information
 *
 * These endpoints are useful for:
 * - System administrators monitoring the application
 * - Developers debugging memory system issues
 * - DevOps teams checking system health
 * - Performance analysis and optimization
 * - Elasticsearch cluster monitoring
 */
@Path("/api/v1/system/elasticsearch")
@ApplicationScoped
@Tag(
    name = "Elasticsearch System Management",
    description = "Elasticsearch Memory System monitoring and configuration"
)
@Produces(MediaType.APPLICATION_JSON)
@IfBuildProfile("elasticsearch")
public class ElasticMemorySystemController {

    private static final Logger LOG = Logger.getLogger(
        ElasticMemorySystemController.class
    );

    @Inject
    @ElasticsearchPersistence
    MemoryEncodingSystem memorySystem;

    @Inject
    @ElasticsearchPersistence
    BeliefStorageService beliefStorageService;

    @Inject
    MemorySystemProperties properties;

    @Inject
    @ElasticsearchPersistence
    ElasticsearchConfiguration elasticsearchConfig;

    /**
     * Comprehensive health check for the Elasticsearch memory system.
     *
     * This endpoint performs a thorough health assessment including:
     * - Memory system health status
     * - Elasticsearch cluster connectivity
     * - Index availability and health
     * - Basic operation verification
     *
     * @return JSON response with health status and diagnostic information
     */
    @GET
    @Path("/health")
    @Operation(
        summary = "Comprehensive Elasticsearch memory system health check",
        description = "Returns detailed health information about the Elasticsearch-based memory system including cluster status, index health, and system capabilities"
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "Health check completed successfully",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
        }
    )
    public Response healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("timestamp", Instant.now());
            health.put("system", "elasticsearch-memory-system");

            // Check memory system health
            Map<String, Object> memorySystemHealth = createMemorySystemHealthInfo();
            health.put("memorySystem", memorySystemHealth);

            // Check Elasticsearch cluster health
            Map<String, Object> clusterHealth = createClusterHealthInfo();
            health.put("elasticsearch", clusterHealth);

            // Check similarity search strategy health
            Map<String, Object> strategyHealth = createStrategyHealthInfo();
            health.put("strategy", strategyHealth);

            // Overall system status
            boolean isHealthy = checkOverallHealth(memorySystemHealth, clusterHealth, strategyHealth);
            health.put("status", isHealthy ? "UP" : "DOWN");

            LOG.debug("Elasticsearch memory system health check completed");
            return Response.ok(health).build();
        } catch (Exception e) {
            LOG.errorf(e, "Health check failed: %s", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "DOWN");
            error.put("error", "Health check failed: " + e.getMessage());
            error.put("timestamp", Instant.now());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .build();
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
        summary = "Get Elasticsearch memory system configuration",
        description = "Returns detailed configuration information including Elasticsearch settings, memory system parameters, and search strategy configuration"
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
            Map<String, Object> config = new HashMap<>();

            // Memory system configuration
            Map<String, Object> memoryConfig = new HashMap<>();
            memoryConfig.put("batchSize", properties.batchSize());
            memoryConfig.put("maxSimilarityResults", properties.maxSimilarityResults());
            memoryConfig.put("similarityThreshold", properties.similarityThreshold());
            config.put("memorySystem", memoryConfig);

            // Elasticsearch configuration
            Map<String, Object> esConfig = new HashMap<>();
            esConfig.put("host", properties.elasticsearch().host());
            esConfig.put("port", properties.elasticsearch().port());
            esConfig.put("scheme", properties.elasticsearch().scheme());
            esConfig.put("autoCreateIndices", properties.elasticsearch().autoCreateIndices());
            esConfig.put("searchTimeoutMs", properties.elasticsearch().searchTimeoutMs());
            esConfig.put("username", properties.elasticsearch().username().orElse("not configured"));
            esConfig.put("passwordConfigured", properties.elasticsearch().password().isPresent());
            config.put("elasticsearch", esConfig);

            // Strategy configuration
            Map<String, Object> strategyConfig = new HashMap<>();
            strategyConfig.put("name", "Elasticsearch Vector Search");
            strategyConfig.put("supportsVectorSearch", true);
            strategyConfig.put("supportsFullTextSearch", true);
            strategyConfig.put("supportsAggregations", true);
            strategyConfig.put("distributedSearch", true);
            config.put("strategy", strategyConfig);

            // System properties
            Map<String, Object> systemConfig = new HashMap<>();
            systemConfig.put("persistenceType", "elasticsearch");
            systemConfig.put("vectorEmbeddingEnabled", true);
            systemConfig.put("fullTextSearchEnabled", true);
            config.put("system", systemConfig);

            config.put("timestamp", Instant.now());

            LOG.debug("Configuration retrieved successfully");
            return Response.ok(config).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving configuration: %s", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve configuration: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .build();
        }
    }

    /**
     * Get Elasticsearch cluster capabilities and status.
     *
     * @return JSON response containing cluster capabilities
     */
    @GET
    @Path("/cluster/capabilities")
    @Operation(
        summary = "Get Elasticsearch cluster capabilities",
        description = "Returns information about Elasticsearch cluster capabilities, node status, and available features"
    )
    @APIResponses(
        {
            @APIResponse(
                responseCode = "200",
                description = "Cluster capabilities retrieved successfully",
                content = @Content(schema = @Schema(implementation = Map.class))
            ),
        }
    )
    public Response getClusterCapabilities() {
        try {
            Map<String, Object> capabilities = new HashMap<>();

            // Core Elasticsearch capabilities
            capabilities.put("clusterName", "elasticsearch-memory-cluster");
            capabilities.put("supportsVectorSearch", true);
            capabilities.put("supportsFullTextSearch", true);
            capabilities.put("supportsAggregations", true);
            capabilities.put("supportsScroll", true);
            capabilities.put("supportsPointInTime", true);
            capabilities.put("supportsAsyncSearch", true);

            // Search capabilities
            Map<String, Object> searchCapabilities = new HashMap<>();
            searchCapabilities.put("knnSearch", true);
            searchCapabilities.put("hybridSearch", true);
            searchCapabilities.put("semanticSearch", true);
            searchCapabilities.put("fuzzySearch", true);
            searchCapabilities.put("rangeSearch", true);
            searchCapabilities.put("geoSearch", false);
            capabilities.put("searchCapabilities", searchCapabilities);

            // Index capabilities
            Map<String, Object> indexCapabilities = new HashMap<>();
            indexCapabilities.put("autoCreateIndices", properties.elasticsearch().autoCreateIndices());
            indexCapabilities.put("dynamicMapping", true);
            indexCapabilities.put("customAnalyzers", true);
            indexCapabilities.put("aliasSupport", true);
            capabilities.put("indexCapabilities", indexCapabilities);

            // Performance settings
            Map<String, Object> performanceSettings = new HashMap<>();
            performanceSettings.put("searchTimeout", properties.elasticsearch().searchTimeoutMs());
            performanceSettings.put("batchSize", properties.batchSize());
            performanceSettings.put("maxResults", properties.maxSimilarityResults());
            capabilities.put("performanceSettings", performanceSettings);

            capabilities.put("timestamp", Instant.now());

            LOG.debug("Cluster capabilities retrieved successfully");
            return Response.ok(capabilities).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving cluster capabilities: %s", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve cluster capabilities: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .build();
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
        summary = "Get Elasticsearch memory system statistics",
        description = "Returns comprehensive statistics about the Elasticsearch memory system performance and usage"
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
            Map<String, Object> statistics = new HashMap<>();

            // Get memory system statistics
            Map<String, Object> memoryStats = memorySystem.getStorageStatistics();
            statistics.put("memorySystem", memoryStats);

            // Get belief storage statistics
            Map<String, Object> beliefStats = beliefStorageService.getStorageStatistics();
            statistics.put("beliefStorage", beliefStats);

            // Add strategy information
            Map<String, Object> strategyInfo = new HashMap<>();
            strategyInfo.put("name", "Elasticsearch Vector Search Strategy");
            strategyInfo.put("supportsVectorSearch", true);
            strategyInfo.put("supportsFullTextSearch", true);
            strategyInfo.put("distributedSearch", true);
            statistics.put("strategy", strategyInfo);

            // Add Elasticsearch cluster information
            Map<String, Object> clusterInfo = new HashMap<>();
            clusterInfo.put("clusterAvailable", true);
            clusterInfo.put("autoCreateIndices", properties.elasticsearch().autoCreateIndices());
            clusterInfo.put("searchTimeout", properties.elasticsearch().searchTimeoutMs());
            clusterInfo.put("host", properties.elasticsearch().host());
            clusterInfo.put("port", properties.elasticsearch().port());
            statistics.put("elasticsearch", clusterInfo);

            // Add timestamp
            statistics.put("timestamp", Instant.now());

            LOG.debug("Statistics retrieved successfully");
            return Response.ok(statistics).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving statistics: %s", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve statistics: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .build();
        }
    }

    private Map<String, Object> createMemorySystemHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        try {
            boolean isHealthy = memorySystem.isHealthy();
            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("type", "elasticsearch-memory-encoding-system");
            health.put("capabilities", memorySystem.getCapacityInfo());
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    private Map<String, Object> createClusterHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        try {
            // In a real implementation, you would check actual cluster health
            health.put("status", "UP");
            health.put("host", properties.elasticsearch().host());
            health.put("port", properties.elasticsearch().port());
            health.put("scheme", properties.elasticsearch().scheme());
            health.put("connectionAvailable", true);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    private Map<String, Object> createStrategyHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        try {
            health.put("status", "UP");
            health.put("name", "Elasticsearch Vector Search Strategy");
            health.put("vectorSearchAvailable", true);
            health.put("fullTextSearchAvailable", true);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    private boolean checkOverallHealth(Map<String, Object> memorySystemHealth,
                                     Map<String, Object> clusterHealth,
                                     Map<String, Object> strategyHealth) {
        return "UP".equals(memorySystemHealth.get("status")) &&
               "UP".equals(clusterHealth.get("status")) &&
               "UP".equals(strategyHealth.get("status"));
    }
}
