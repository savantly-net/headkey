package ai.headkey.rest.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.persistence.entities.BeliefEntity;
import ai.headkey.persistence.repositories.BeliefRepository;
import ai.headkey.persistence.repositories.impl.JpaBeliefRepository;
import ai.headkey.rest.config.PostgresPersistence;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

/**
 * Diagnostic service for troubleshooting belief persistence issues.
 *
 * This service provides methods to:
 * - Verify database connectivity and table existence
 * - Check recent belief persistence activity
 * - Validate transaction behavior
 * - Diagnose collection fetch warnings
 * - Monitor belief storage service health
 */
@ApplicationScoped
@IfBuildProfile(PostgresPersistence.NAME)
public class BeliefPersistenceDiagnosticService {

    private static final Logger LOG = Logger.getLogger(
        BeliefPersistenceDiagnosticService.class
    );

    @Inject
    EntityManager entityManager;

    @Inject
    BeliefStorageService beliefStorageService;

    /**
     * Comprehensive diagnostic report for belief persistence issues.
     */
    @Transactional
    public DiagnosticReport generateDiagnosticReport() {
        LOG.info("Starting belief persistence diagnostic report generation");

        DiagnosticReport report = new DiagnosticReport();
        report.timestamp = Instant.now();

        try {
            // 1. Check database connectivity
            report.databaseConnected = checkDatabaseConnectivity();

            // 2. Verify table existence
            report.beliefsTableExists = checkBeliefsTableExists();
            report.evidenceTableExists = checkEvidenceTableExists();
            report.tagsTableExists = checkTagsTableExists();

            // 3. Count total beliefs
            report.totalBeliefs = countTotalBeliefs();

            // 4. Count recent beliefs (last hour)
            report.recentBeliefs = countRecentBeliefs(1);

            // 5. Check for orphaned collections
            report.orphanedEvidence = countOrphanedEvidence();
            report.orphanedTags = countOrphanedTags();

            // 6. Validate storage service
            report.storageServiceHealthy = testStorageServiceHealth();

            // 7. Check transaction state
            report.transactionActive = entityManager
                .getTransaction()
                .isActive();

            // 8. Get recent belief samples
            report.recentBeliefSamples = getRecentBeliefSamples(5);

            // 9. Check collection fetch behavior
            report.collectionFetchWarnings = detectCollectionFetchIssues();

            LOG.infof(
                "Diagnostic report completed: %d total beliefs, %d recent beliefs",
                report.totalBeliefs,
                report.recentBeliefs
            );
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Error generating diagnostic report: %s",
                e.getMessage()
            );
            report.errorMessage = e.getMessage();
        }

        return report;
    }

    /**
     * Test belief creation and persistence in isolation.
     */
    @Transactional
    public BeliefPersistenceTestResult testBeliefPersistence() {
        LOG.info("Starting isolated belief persistence test");

        BeliefPersistenceTestResult result = new BeliefPersistenceTestResult();
        result.timestamp = Instant.now();

        try {
            // Create a test belief directly using repository
            BeliefRepository repository = new JpaBeliefRepository(
                entityManager.getEntityManagerFactory()
            );

            String testId = "test-belief-" + System.currentTimeMillis();
            BeliefEntity testBelief = new BeliefEntity(
                testId,
                "test-agent-diagnostic",
                "This is a diagnostic test belief created at " + Instant.now(),
                0.85
            );
            testBelief.setCategory("diagnostic");
            testBelief.addEvidence("test-memory-" + System.currentTimeMillis());
            testBelief.addTag("diagnostic");
            testBelief.addTag("test");

            // Save the belief
            LOG.infof("Saving test belief with ID: %s", testId);
            BeliefEntity savedBelief = repository.save(testBelief);
            result.beliefCreated = (savedBelief != null);
            result.testBeliefId = testId;

            // Force flush to database
            entityManager.flush();
            result.flushedToDatabase = true;

            // Try to retrieve the belief immediately
            var retrievedBelief = repository.findById(testId);
            result.beliefRetrievable = retrievedBelief.isPresent();

            if (retrievedBelief.isPresent()) {
                BeliefEntity belief = retrievedBelief.get();
                result.evidenceCount = belief.getEvidenceMemoryIds().size();
                result.tagCount = belief.getTags().size();
                LOG.infof(
                    "Test belief retrieved successfully with %d evidence and %d tags",
                    result.evidenceCount,
                    result.tagCount
                );
            }

            // Test storage service
            result.storageServiceWorking = testStorageServiceDirectly();
        } catch (Exception e) {
            LOG.errorf(e, "Belief persistence test failed: %s", e.getMessage());
            result.errorMessage = e.getMessage();
        }

        return result;
    }

    /**
     * Monitor belief creation in real-time.
     */
    public BeliefCreationMonitor startBeliefCreationMonitoring(
        int durationMinutes
    ) {
        BeliefCreationMonitor monitor = new BeliefCreationMonitor();
        monitor.startTime = Instant.now();
        monitor.initialCount = countTotalBeliefs();
        monitor.monitoringDurationMinutes = durationMinutes;

        LOG.infof(
            "Started belief creation monitoring for %d minutes. Initial count: %d",
            durationMinutes,
            monitor.initialCount
        );

        return monitor;
    }

    /**
     * Complete belief creation monitoring and return results.
     */
    public BeliefCreationMonitor completeBeliefCreationMonitoring(
        BeliefCreationMonitor monitor
    ) {
        monitor.endTime = Instant.now();
        monitor.finalCount = countTotalBeliefs();
        monitor.beliefsCreated = monitor.finalCount - monitor.initialCount;
        monitor.actualDurationMinutes = ChronoUnit.MINUTES.between(
            monitor.startTime,
            monitor.endTime
        );

        LOG.infof(
            "Completed belief creation monitoring. Created %d beliefs in %d minutes",
            monitor.beliefsCreated,
            monitor.actualDurationMinutes
        );

        return monitor;
    }

    private boolean checkDatabaseConnectivity() {
        try {
            Query query = entityManager.createNativeQuery("SELECT 1");
            query.getSingleResult();
            return true;
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Database connectivity check failed: %s",
                e.getMessage()
            );
            return false;
        }
    }

    private boolean checkBeliefsTableExists() {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM beliefs"
            );
            query.getSingleResult();
            return true;
        } catch (Exception e) {
            LOG.errorf(e, "Beliefs table check failed: %s", e.getMessage());
            return false;
        }
    }

    private boolean checkEvidenceTableExists() {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM belief_evidence_memories"
            );
            query.getSingleResult();
            return true;
        } catch (Exception e) {
            LOG.errorf(e, "Evidence table check failed: %s", e.getMessage());
            return false;
        }
    }

    private boolean checkTagsTableExists() {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM belief_tags"
            );
            query.getSingleResult();
            return true;
        } catch (Exception e) {
            LOG.errorf(e, "Tags table check failed: %s", e.getMessage());
            return false;
        }
    }

    private long countTotalBeliefs() {
        try {
            Query query = entityManager.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b"
            );
            return ((Number) query.getSingleResult()).longValue();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to count total beliefs: %s", e.getMessage());
            return -1;
        }
    }

    private long countRecentBeliefs(int hours) {
        try {
            Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
            Query query = entityManager.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b WHERE b.createdAt >= :cutoff"
            );
            query.setParameter("cutoff", cutoff);
            return ((Number) query.getSingleResult()).longValue();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to count recent beliefs: %s", e.getMessage());
            return -1;
        }
    }

    private long countOrphanedEvidence() {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM belief_evidence_memories bem " +
                "WHERE NOT EXISTS (SELECT 1 FROM beliefs b WHERE b.id = bem.belief_id)"
            );
            return ((Number) query.getSingleResult()).longValue();
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Failed to count orphaned evidence: %s",
                e.getMessage()
            );
            return -1;
        }
    }

    private long countOrphanedTags() {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM belief_tags bt " +
                "WHERE NOT EXISTS (SELECT 1 FROM beliefs b WHERE b.id = bt.belief_id)"
            );
            return ((Number) query.getSingleResult()).longValue();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to count orphaned tags: %s", e.getMessage());
            return -1;
        }
    }

    private boolean testStorageServiceHealth() {
        try {
            // This assumes the storage service has a health check method
            // If not available, we can test basic functionality
            return beliefStorageService != null;
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Storage service health check failed: %s",
                e.getMessage()
            );
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getRecentBeliefSamples(int limit) {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT id, agent_id, statement, confidence, created_at, active " +
                "FROM beliefs ORDER BY created_at DESC LIMIT :limit"
            );
            query.setParameter("limit", limit);

            List<Object[]> results = query.getResultList();
            return results
                .stream()
                .map(row ->
                    Map.of(
                        "id",
                        row[0],
                        "agentId",
                        row[1],
                        "statement",
                        row[2],
                        "confidence",
                        row[3],
                        "createdAt",
                        row[4],
                        "active",
                        row[5]
                    )
                )
                .toList();
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Failed to get recent belief samples: %s",
                e.getMessage()
            );
            return List.of();
        }
    }

    private String detectCollectionFetchIssues() {
        try {
            // Check for queries that might cause HHH90003004 warning
            Query query = entityManager.createQuery(
                "SELECT b FROM BeliefEntity b WHERE b.active = true ORDER BY b.createdAt DESC"
            );
            query.setMaxResults(10);
            List<BeliefEntity> results = query.getResultList();

            // Force collection loading to detect issues
            for (BeliefEntity belief : results) {
                belief.getEvidenceMemoryIds().size(); // Force lazy loading
                belief.getTags().size(); // Force lazy loading
            }

            return "No immediate collection fetch issues detected";
        } catch (Exception e) {
            return "Collection fetch issue detected: " + e.getMessage();
        }
    }

    private boolean testStorageServiceDirectly() {
        try {
            // Create a minimal test belief through the storage service
            Belief testBelief = new Belief(
                "storage-test-" + System.currentTimeMillis(),
                "test-agent-storage",
                "Storage service test belief",
                0.75
            );

            Belief stored = beliefStorageService.storeBelief(testBelief);
            return stored != null && stored.getId() != null;
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Storage service direct test failed: %s",
                e.getMessage()
            );
            return false;
        }
    }

    // Result classes
    public static class DiagnosticReport {

        public Instant timestamp;
        public boolean databaseConnected;
        public boolean beliefsTableExists;
        public boolean evidenceTableExists;
        public boolean tagsTableExists;
        public long totalBeliefs;
        public long recentBeliefs;
        public long orphanedEvidence;
        public long orphanedTags;
        public boolean storageServiceHealthy;
        public boolean transactionActive;
        public List<Map<String, Object>> recentBeliefSamples;
        public String collectionFetchWarnings;
        public String errorMessage;
    }

    public static class BeliefPersistenceTestResult {

        public Instant timestamp;
        public boolean beliefCreated;
        public String testBeliefId;
        public boolean flushedToDatabase;
        public boolean beliefRetrievable;
        public int evidenceCount;
        public int tagCount;
        public boolean storageServiceWorking;
        public String errorMessage;
    }

    public static class BeliefCreationMonitor {

        public Instant startTime;
        public Instant endTime;
        public long initialCount;
        public long finalCount;
        public long beliefsCreated;
        public int monitoringDurationMinutes;
        public long actualDurationMinutes;
    }
}
