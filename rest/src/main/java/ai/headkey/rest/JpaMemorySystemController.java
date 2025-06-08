package ai.headkey.rest;

import ai.headkey.memory.implementations.JpaMemoryEncodingSystem;
import ai.headkey.memory.implementations.JpaMemorySystemFactory;
import ai.headkey.memory.strategies.jpa.JpaSimilaritySearchStrategyFactory;
import ai.headkey.rest.config.MemorySystemProperties;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for JPA Memory System monitoring and configuration.
 * 
 * This controller provides endpoints to:
 * - Monitor the health and status of the JPA-based memory system
 * - View current configuration and database capabilities
 * - Access memory system statistics and performance metrics
 * - Inspect similarity search strategy information
 * 
 * These endpoints are useful for:
 * - System administrators monitoring the application
 * - Developers debugging memory system issues
 * - DevOps teams checking system health
 * - Performance analysis and optimization
 */
@Path("/api/v1/system")
@ApplicationScoped
@Tag(name = "System Management", description = "JPA Memory System monitoring and configuration")
@Produces(MediaType.APPLICATION_JSON)
public class JpaMemorySystemController {
    
    private static final Logger LOG = Logger.getLogger(JpaMemorySystemController.class);
    
    @Inject
    JpaMemoryEncodingSystem memorySystem;
    
    @Inject
    MemorySystemProperties properties;
    
    @Inject
    EntityManager entityManager;
    
    /**
     * Comprehensive health check for the JPA memory system.
     * 
     * This endpoint performs a thorough health assessment including:
     * - Memory system health status
     * - Database connectivity
     * - Similarity search strategy status
     * - Basic operation verification
     */
    @GET
    @Path("/health")
    @Operation(
        summary = "Comprehensive health check",
        description = "Performs a thorough health assessment of the JPA memory system, " +
                     "including database connectivity and strategy status"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "System is healthy",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @APIResponse(
            responseCode = "503",
            description = "System is unhealthy",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public Response healthCheck() {
        Map<String, Object> healthStatus = new HashMap<>();
        boolean isHealthy = true;
        
        try {
            healthStatus.put("timestamp", Instant.now());
            healthStatus.put("service", "jpa-memory-system");
            
            // Check memory system health
            boolean memorySystemHealthy = memorySystem.isHealthy();
            healthStatus.put("memorySystem", createMemorySystemHealthInfo(memorySystemHealthy));
            
            // Check database connectivity
            boolean databaseHealthy = checkDatabaseHealth();
            healthStatus.put("database", createDatabaseHealthInfo(databaseHealthy));
            
            // Check similarity search strategy
            boolean strategyHealthy = checkStrategyHealth();
            healthStatus.put("similarityStrategy", createStrategyHealthInfo(strategyHealthy));
            
            // Overall health status
            isHealthy = memorySystemHealthy && databaseHealthy && strategyHealthy;
            healthStatus.put("healthy", isHealthy);
            healthStatus.put("status", isHealthy ? "UP" : "DOWN");
            
            if (isHealthy) {
                LOG.debug("Comprehensive health check passed");
                return Response.ok(healthStatus).build();
            } else {
                LOG.warn("Comprehensive health check failed");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                             .entity(healthStatus).build();
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Health check error: %s", e.getMessage());
            healthStatus.put("healthy", false);
            healthStatus.put("status", "DOWN");
            healthStatus.put("error", "Health check failed: " + e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                         .entity(healthStatus).build();
        }
    }
    
    /**
     * Gets the current configuration of the memory system.
     */
    @GET
    @Path("/config")
    @Operation(
        summary = "Get system configuration",
        description = "Returns the current configuration settings for the JPA memory system"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Configuration retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public Response getConfiguration() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // Memory system configuration
            Map<String, Object> memoryConfig = new HashMap<>();
            memoryConfig.put("strategy", properties.strategy());
            memoryConfig.put("batchSize", properties.batchSize());
            memoryConfig.put("maxSimilarityResults", properties.maxSimilarityResults());
            memoryConfig.put("similarityThreshold", properties.similarityThreshold());
            memoryConfig.put("enableSecondLevelCache", properties.enableSecondLevelCache());
            config.put("memory", memoryConfig);
            
            // Database configuration
            Map<String, Object> databaseConfig = new HashMap<>();
            databaseConfig.put("kind", properties.database().kind());
            databaseConfig.put("autoCreateSchema", properties.database().autoCreateSchema());
            
            Map<String, Object> poolConfig = new HashMap<>();
            poolConfig.put("minSize", properties.database().pool().minSize());
            poolConfig.put("maxSize", properties.database().pool().maxSize());
            poolConfig.put("timeoutMs", properties.database().pool().timeoutMs());
            databaseConfig.put("pool", poolConfig);
            config.put("database", databaseConfig);
            
            // Embedding configuration
            Map<String, Object> embeddingConfig = new HashMap<>();
            embeddingConfig.put("enabled", properties.embedding().enabled());
            embeddingConfig.put("dimension", properties.embedding().dimension());
            embeddingConfig.put("model", properties.embedding().model());
            config.put("embedding", embeddingConfig);
            
            // Performance configuration
            Map<String, Object> performanceConfig = new HashMap<>();
            performanceConfig.put("enableStatistics", properties.performance().enableStatistics());
            performanceConfig.put("cacheSize", properties.performance().cacheSize());
            performanceConfig.put("enableAsync", properties.performance().enableAsync());
            config.put("performance", performanceConfig);
            
            // Runtime information
            Map<String, Object> runtime = new HashMap<>();
            runtime.put("actualStrategy", memorySystem.getSimilaritySearchStrategy().getStrategyName());
            runtime.put("supportsVectorSearch", memorySystem.getSimilaritySearchStrategy().supportsVectorSearch());
            runtime.put("actualBatchSize", memorySystem.getBatchSize());
            runtime.put("actualMaxSimilarityResults", memorySystem.getMaxSimilaritySearchResults());
            runtime.put("actualSimilarityThreshold", memorySystem.getSimilarityThreshold());
            runtime.put("actualSecondLevelCache", memorySystem.isSecondLevelCacheEnabled());
            config.put("runtime", runtime);
            
            LOG.debug("Configuration retrieved successfully");
            return Response.ok(config).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving configuration: %s", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve configuration: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    /**
     * Gets database capabilities and analysis.
     */
    @GET
    @Path("/database/capabilities")
    @Operation(
        summary = "Analyze database capabilities",
        description = "Analyzes the connected database and returns its capabilities for similarity search"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Database capabilities analyzed successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public Response getDatabaseCapabilities() {
        try {
            Map<String, Object> capabilities = new HashMap<>();
            
            // Analyze database capabilities
            var dbCapabilities = JpaSimilaritySearchStrategyFactory.analyzeDatabaseCapabilities(entityManager);
            
            capabilities.put("databaseType", dbCapabilities.getDatabaseType().toString());
            capabilities.put("version", dbCapabilities.getVersion());
            capabilities.put("hasVectorSupport", dbCapabilities.hasVectorSupport());
            capabilities.put("hasFullTextSupport", dbCapabilities.hasFullTextSupport());
            
            // Get strategy recommendation
            var recommendation = JpaMemorySystemFactory.analyzeDatabase(entityManager.getEntityManagerFactory());
                capabilities.put("recommendedStrategy", recommendation.getRecommendedStrategy().getStrategyName());
                capabilities.put("currentStrategy", memorySystem.getSimilaritySearchStrategy().getStrategyName());
                capabilities.put("strategyOptimal", 
                    recommendation.getRecommendedStrategy().getStrategyName().equals(
                        memorySystem.getSimilaritySearchStrategy().getStrategyName()));
                
                LOG.debug("Database capabilities analyzed successfully");
                return Response.ok(capabilities).build();
                

            
        } catch (Exception e) {
            LOG.errorf(e, "Error analyzing database capabilities: %s", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to analyze database capabilities: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    /**
     * Gets comprehensive system statistics.
     */
    @GET
    @Path("/statistics")
    @Operation(
        summary = "Get system statistics",
        description = "Returns comprehensive statistics about the JPA memory system performance and usage"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public Response getStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // Get memory system statistics
            Map<String, Object> memoryStats = memorySystem.getStorageStatistics();
            statistics.put("memorySystem", memoryStats);
            
            // Add strategy information
            Map<String, Object> strategyInfo = new HashMap<>();
            strategyInfo.put("name", memorySystem.getSimilaritySearchStrategy().getStrategyName());
            strategyInfo.put("supportsVectorSearch", memorySystem.getSimilaritySearchStrategy().supportsVectorSearch());
            statistics.put("strategy", strategyInfo);
            
            // Add database information
            if (entityManager.getEntityManagerFactory().isOpen()) {
                Map<String, Object> dbInfo = new HashMap<>();
                dbInfo.put("entityManagerFactoryOpen", true);
                dbInfo.put("configuredDatabaseKind", properties.database().kind());
                statistics.put("database", dbInfo);
            }
            
            // Add timestamp
            statistics.put("timestamp", Instant.now());
            
            LOG.debug("Statistics retrieved successfully");
            return Response.ok(statistics).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving statistics: %s", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve statistics: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    private Map<String, Object> createMemorySystemHealthInfo(boolean healthy) {
        Map<String, Object> info = new HashMap<>();
        info.put("healthy", healthy);
        info.put("strategy", memorySystem.getSimilaritySearchStrategy().getStrategyName());
        info.put("supportsVectorSearch", memorySystem.getSimilaritySearchStrategy().supportsVectorSearch());
        return info;
    }
    
    private Map<String, Object> createDatabaseHealthInfo(boolean healthy) {
        Map<String, Object> info = new HashMap<>();
        info.put("healthy", healthy);
        info.put("entityManagerFactoryOpen", entityManager.getEntityManagerFactory().isOpen());
        info.put("configuredKind", properties.database().kind());
        return info;
    }
    
    private Map<String, Object> createStrategyHealthInfo(boolean healthy) {
        Map<String, Object> info = new HashMap<>();
        info.put("healthy", healthy);
        info.put("name", memorySystem.getSimilaritySearchStrategy().getStrategyName());
        info.put("configured", properties.strategy());
        return info;
    }
    
    private boolean checkDatabaseHealth() {
        try {
            return entityManager.getEntityManagerFactory().isOpen();
        } catch (Exception e) {
            LOG.debugf("Database health check failed: %s", e.getMessage());
            return false;
        }
    }
    
    private boolean checkStrategyHealth() {
        try {
            // Try to get strategy information
            memorySystem.getSimilaritySearchStrategy().getStrategyName();
            return true;
        } catch (Exception e) {
            LOG.debugf("Strategy health check failed: %s", e.getMessage());
            return false;
        }
    }
}