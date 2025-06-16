package ai.headkey.rest;

import ai.headkey.rest.service.BeliefPersistenceDiagnosticService;
import ai.headkey.rest.service.BeliefPersistenceDiagnosticService.BeliefCreationMonitor;
import ai.headkey.rest.service.BeliefPersistenceDiagnosticService.BeliefPersistenceTestResult;
import ai.headkey.rest.service.BeliefPersistenceDiagnosticService.DiagnosticReport;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * REST Controller for diagnostic operations to troubleshoot belief persistence issues.
 *
 * This controller provides endpoints to:
 * - Generate comprehensive diagnostic reports
 * - Test belief persistence in isolation
 * - Monitor belief creation in real-time
 * - Check database health and configuration
 * - Investigate collection fetch warnings
 *
 * These endpoints are intended for debugging production issues and should be
 * secured or disabled in production environments.
 */
@Path("/api/v1/diagnostics")
@ApplicationScoped
@Tag(
    name = "Diagnostics",
    description = "Belief persistence troubleshooting and diagnostics"
)
@Produces(MediaType.APPLICATION_JSON)
@IfBuildProfile("diagnostics")
public class DiagnosticController {

    private static final Logger LOG = Logger.getLogger(
        DiagnosticController.class
    );

    // Store monitoring sessions
    private final Map<String, BeliefCreationMonitor> activeMonitors =
        new ConcurrentHashMap<>();

    @Inject
    BeliefPersistenceDiagnosticService diagnosticService;

    /**
     * Generate a comprehensive diagnostic report for belief persistence.
     *
     * This endpoint examines:
     * - Database connectivity and table existence
     * - Recent belief creation activity
     * - Collection fetch issues (HHH90003004 warning)
     * - Storage service health
     * - Transaction states
     */
    @GET
    @Path("/beliefs/report")
    @Operation(
        summary = "Generate comprehensive belief persistence diagnostic report",
        description = "Analyzes database state, recent activity, and potential issues with belief persistence"
    )
    @APIResponse(
        responseCode = "200",
        description = "Diagnostic report generated successfully"
    )
    @APIResponse(
        responseCode = "500",
        description = "Error generating diagnostic report"
    )
    public Response generateDiagnosticReport() {
        LOG.info("Generating belief persistence diagnostic report");

        try {
            DiagnosticReport report =
                diagnosticService.generateDiagnosticReport();

            // Add summary assessment
            Map<String, Object> response = Map.of(
                "report",
                report,
                "summary",
                createReportSummary(report),
                "recommendations",
                generateRecommendations(report),
                "generatedAt",
                Instant.now()
            );

            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Failed to generate diagnostic report: %s",
                e.getMessage()
            );
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    Map.of(
                        "error",
                        "Failed to generate diagnostic report: " +
                        e.getMessage()
                    )
                )
                .build();
        }
    }

    /**
     * Test belief persistence in isolation to verify the storage mechanism.
     *
     * This creates a test belief directly and verifies it can be stored and retrieved,
     * helping isolate whether the issue is in the belief creation flow or storage layer.
     */
    @POST
    @Path("/beliefs/test-persistence")
    @Operation(
        summary = "Test belief persistence in isolation",
        description = "Creates a test belief and verifies it can be stored and retrieved to isolate persistence issues"
    )
    @APIResponse(
        responseCode = "200",
        description = "Persistence test completed"
    )
    @APIResponse(responseCode = "500", description = "Persistence test failed")
    public Response testBeliefPersistence() {
        LOG.info("Starting isolated belief persistence test");

        try {
            BeliefPersistenceTestResult result =
                diagnosticService.testBeliefPersistence();

            Map<String, Object> response = Map.of(
                "result",
                result,
                "success",
                result.beliefCreated &&
                result.beliefRetrievable &&
                result.storageServiceWorking,
                "analysis",
                analyzeTestResult(result),
                "testedAt",
                Instant.now()
            );

            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.errorf(e, "Belief persistence test failed: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    Map.of(
                        "error",
                        "Persistence test failed: " + e.getMessage()
                    )
                )
                .build();
        }
    }

    /**
     * Start monitoring belief creation activity.
     *
     * This begins tracking belief creation over a specified duration to help
     * identify if beliefs are being created but not persisted.
     */
    @POST
    @Path("/beliefs/monitor/start")
    @Operation(
        summary = "Start monitoring belief creation activity",
        description = "Begins tracking belief creation to identify persistence vs creation issues"
    )
    @APIResponse(
        responseCode = "200",
        description = "Monitoring started successfully"
    )
    @APIResponse(
        responseCode = "400",
        description = "Invalid monitoring duration"
    )
    public Response startBeliefCreationMonitoring(
        @Parameter(
            description = "Monitoring duration in minutes",
            required = true
        ) @QueryParam("durationMinutes") @DefaultValue("10") int durationMinutes
    ) {
        if (durationMinutes < 1 || durationMinutes > 60) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    Map.of("error", "Duration must be between 1 and 60 minutes")
                )
                .build();
        }

        LOG.infof(
            "Starting belief creation monitoring for %d minutes",
            durationMinutes
        );

        try {
            BeliefCreationMonitor monitor =
                diagnosticService.startBeliefCreationMonitoring(
                    durationMinutes
                );
            String monitorId = "monitor-" + System.currentTimeMillis();
            activeMonitors.put(monitorId, monitor);

            Map<String, Object> response = Map.of(
                "monitorId",
                monitorId,
                "monitor",
                monitor,
                "message",
                "Monitoring started. Use the monitorId to check results."
            );

            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Failed to start belief monitoring: %s",
                e.getMessage()
            );
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    Map.of(
                        "error",
                        "Failed to start monitoring: " + e.getMessage()
                    )
                )
                .build();
        }
    }

    /**
     * Check results of belief creation monitoring.
     *
     * This returns the current state of a monitoring session, including
     * how many beliefs have been created since monitoring started.
     */
    @GET
    @Path("/beliefs/monitor/{monitorId}")
    @Operation(
        summary = "Check belief creation monitoring results",
        description = "Returns current state and results of an active monitoring session"
    )
    @APIResponse(
        responseCode = "200",
        description = "Monitoring results retrieved"
    )
    @APIResponse(
        responseCode = "404",
        description = "Monitor session not found"
    )
    public Response getBeliefCreationMonitoringResults(
        @Parameter(
            description = "Monitor session ID",
            required = true
        ) @PathParam("monitorId") String monitorId
    ) {
        BeliefCreationMonitor monitor = activeMonitors.get(monitorId);
        if (monitor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(
                    Map.of("error", "Monitor session not found: " + monitorId)
                )
                .build();
        }

        try {
            // Update monitor with current state
            BeliefCreationMonitor updatedMonitor =
                diagnosticService.completeBeliefCreationMonitoring(monitor);
            activeMonitors.put(monitorId, updatedMonitor);

            boolean isComplete = updatedMonitor.endTime != null;

            Map<String, Object> response = Map.of(
                "monitorId",
                monitorId,
                "monitor",
                updatedMonitor,
                "complete",
                isComplete,
                "analysis",
                analyzeMonitorResults(updatedMonitor),
                "checkedAt",
                Instant.now()
            );

            // Clean up completed monitors
            if (isComplete) {
                activeMonitors.remove(monitorId);
            }

            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Failed to get monitoring results: %s",
                e.getMessage()
            );
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    Map.of(
                        "error",
                        "Failed to get monitoring results: " + e.getMessage()
                    )
                )
                .build();
        }
    }

    /**
     * Get quick health status of the belief persistence system.
     */
    @GET
    @Path("/beliefs/health")
    @Operation(
        summary = "Quick health check for belief persistence",
        description = "Returns basic health indicators for the belief persistence system"
    )
    @APIResponse(responseCode = "200", description = "Health check completed")
    public Response getBeliefPersistenceHealth() {
        try {
            DiagnosticReport report =
                diagnosticService.generateDiagnosticReport();

            boolean healthy =
                report.databaseConnected &&
                report.beliefsTableExists &&
                report.storageServiceHealthy &&
                report.errorMessage == null;

            Map<String, Object> health = Map.of(
                "healthy",
                healthy,
                "databaseConnected",
                report.databaseConnected,
                "tablesExist",
                report.beliefsTableExists,
                "storageServiceHealthy",
                report.storageServiceHealthy,
                "totalBeliefs",
                report.totalBeliefs,
                "recentBeliefs",
                report.recentBeliefs,
                "checkedAt",
                Instant.now()
            );

            return Response.ok(health).build();
        } catch (Exception e) {
            LOG.errorf(e, "Health check failed: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    Map.of(
                        "healthy",
                        false,
                        "error",
                        e.getMessage(),
                        "checkedAt",
                        Instant.now()
                    )
                )
                .build();
        }
    }

    private Map<String, Object> createReportSummary(DiagnosticReport report) {
        boolean critical =
            !report.databaseConnected ||
            !report.beliefsTableExists ||
            !report.storageServiceHealthy;
        boolean warning =
            report.orphanedEvidence > 0 ||
            report.orphanedTags > 0 ||
            report.collectionFetchWarnings.contains("issue");

        String status = critical
            ? "CRITICAL"
            : (warning ? "WARNING" : "HEALTHY");

        return Map.of(
            "status",
            status,
            "critical",
            critical,
            "warning",
            warning,
            "totalBeliefs",
            report.totalBeliefs,
            "recentActivity",
            report.recentBeliefs > 0 ? "Active" : "None"
        );
    }

    private Map<String, String> generateRecommendations(
        DiagnosticReport report
    ) {
        Map<String, String> recommendations = new ConcurrentHashMap<>();

        if (!report.databaseConnected) {
            recommendations.put(
                "database",
                "Check database connection configuration and ensure PostgreSQL is running"
            );
        }

        if (!report.beliefsTableExists) {
            recommendations.put(
                "schema",
                "Verify Hibernate schema generation is enabled and working"
            );
        }

        if (report.totalBeliefs == 0 && report.recentBeliefs == 0) {
            recommendations.put(
                "persistence",
                "No beliefs found - check if belief creation is working or if beliefs are being created in a different database"
            );
        }

        if (report.orphanedEvidence > 0) {
            recommendations.put(
                "orphanedEvidence",
                "Clean up orphaned evidence records that reference non-existent beliefs"
            );
        }

        if (report.collectionFetchWarnings.contains("issue")) {
            recommendations.put(
                "collections",
                "Consider optimizing collection fetch strategies to avoid HHH90003004 warnings"
            );
        }

        if (report.recentBeliefs == 0 && report.totalBeliefs > 0) {
            recommendations.put(
                "activity",
                "Beliefs exist but none created recently - check if belief creation flow is active"
            );
        }

        return recommendations;
    }

    private Map<String, Object> analyzeTestResult(
        BeliefPersistenceTestResult result
    ) {
        boolean allGood =
            result.beliefCreated &&
            result.beliefRetrievable &&
            result.storageServiceWorking;

        String analysis = allGood
            ? "All persistence mechanisms working correctly"
            : !result.beliefCreated
                ? "Failed to create belief - check repository implementation"
                : !result.beliefRetrievable
                    ? "Belief created but not retrievable - possible transaction issue"
                    : !result.storageServiceWorking
                        ? "Storage service integration issue"
                        : "Unknown persistence issue";

        return Map.of(
            "success",
            allGood,
            "analysis",
            analysis,
            "persistenceWorking",
            result.beliefCreated && result.beliefRetrievable,
            "storageServiceWorking",
            result.storageServiceWorking
        );
    }

    private Map<String, Object> analyzeMonitorResults(
        BeliefCreationMonitor monitor
    ) {
        boolean hasActivity = monitor.beliefsCreated > 0;
        double beliefsPerMinute = monitor.actualDurationMinutes > 0
            ? (double) monitor.beliefsCreated / monitor.actualDurationMinutes
            : 0;

        String analysis = hasActivity
            ? String.format(
                "Beliefs are being created at %.2f per minute",
                beliefsPerMinute
            )
            : "No belief creation detected during monitoring period";

        return Map.of(
            "hasActivity",
            hasActivity,
            "beliefsPerMinute",
            beliefsPerMinute,
            "analysis",
            analysis,
            "recommendation",
            hasActivity
                ? "Beliefs are being created - check if they're persisting correctly"
                : "No belief creation detected - check if belief extraction/analysis is working"
        );
    }
}
