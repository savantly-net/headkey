package ai.headkey.persistence.services;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefConflict;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.persistence.entities.BeliefConflictEntity;
import ai.headkey.persistence.entities.BeliefEntity;
import ai.headkey.persistence.mappers.BeliefConflictMapper;
import ai.headkey.persistence.mappers.BeliefMapper;
import ai.headkey.persistence.repositories.BeliefConflictRepository;
import ai.headkey.persistence.repositories.BeliefRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of BeliefStorageService using PostgreSQL database.
 *
 * This implementation provides persistent storage for beliefs and conflicts
 * using JPA/Hibernate with PostgreSQL as the backend database. It includes
 * optimizations for performance, proper transaction management, and
 * comprehensive query capabilities.
 *
 * Key features:
 * - Full JPA/Hibernate integration
 * - PostgreSQL-specific optimizations
 * - Batch operations for performance
 * - Full-text search capabilities
 * - Comprehensive indexing strategy
 * - Transaction management
 * - Connection pooling support
 *
 * This service integrates with LangChain4J for AI-powered similarity
 * calculations and semantic search capabilities.
 *
 * @since 1.0
 */
public class JpaBeliefStorageService implements BeliefStorageService {

    private final BeliefRepository beliefRepository;
    private final BeliefConflictRepository conflictRepository;

    // Statistics tracking
    private long totalStoreOperations = 0;
    private long totalQueryOperations = 0;
    private long totalSearchOperations = 0;
    private final Instant createdAt;

    /**
     * Constructor with repository dependencies.
     */
    public JpaBeliefStorageService(
        BeliefRepository beliefRepository,
        BeliefConflictRepository conflictRepository
    ) {
        this.beliefRepository = beliefRepository;
        this.conflictRepository = conflictRepository;
        this.createdAt = Instant.now();
    }

    // ========== Basic CRUD Operations ==========

    @Override
    public Belief storeBelief(Belief belief) {
        if (belief == null) {
            throw new IllegalArgumentException("Belief cannot be null");
        }
        if (belief.getId() == null || belief.getId().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Belief ID cannot be null or empty"
            );
        }
        if (
            belief.getAgentId() == null || belief.getAgentId().trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                "Agent ID cannot be null or empty"
            );
        }

        try {
            // Check if belief already exists
            Optional<BeliefEntity> existingEntity = beliefRepository.findById(
                belief.getId()
            );

            BeliefEntity entityToSave;
            if (existingEntity.isPresent()) {
                // Update existing entity
                entityToSave = existingEntity.get();
                BeliefMapper.updateEntity(entityToSave, belief);
            } else {
                // Create new entity
                entityToSave = BeliefMapper.toEntity(belief);
            }

            // Save to database
            BeliefEntity savedEntity = beliefRepository.save(entityToSave);
            totalStoreOperations++;

            return BeliefMapper.toDto(savedEntity);
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to store belief: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public List<Belief> storeBeliefs(List<Belief> beliefs) {
        if (beliefs == null) {
            throw new IllegalArgumentException("Beliefs list cannot be null");
        }

        if (beliefs.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<BeliefEntity> entities = beliefs
                .stream()
                .map(belief -> {
                    Optional<BeliefEntity> existing = beliefRepository.findById(
                        belief.getId()
                    );
                    return BeliefMapper.toEntityPreservingJpaFields(
                        belief,
                        existing.orElse(null)
                    );
                })
                .collect(Collectors.toList());

            List<BeliefEntity> savedEntities = beliefRepository.saveAll(
                entities
            );
            totalStoreOperations += beliefs.size();

            return savedEntities
                .stream()
                .map(BeliefMapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to store beliefs: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public Optional<Belief> getBeliefById(String beliefId) {
        if (beliefId == null || beliefId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Belief ID cannot be null or empty"
            );
        }

        try {
            totalQueryOperations++;
            Optional<BeliefEntity> entity = beliefRepository.findById(beliefId);
            return entity.map(BeliefMapper::toDto);
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to retrieve belief: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public List<Belief> getBeliefsById(Set<String> beliefIds) {
        if (beliefIds == null) {
            throw new IllegalArgumentException("Belief IDs set cannot be null");
        }

        if (beliefIds.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            totalQueryOperations++;
            List<BeliefEntity> entities = beliefRepository.findByIds(beliefIds);
            return entities
                .stream()
                .map(BeliefMapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to retrieve beliefs: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public boolean deleteBelief(String beliefId) {
        if (beliefId == null || beliefId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Belief ID cannot be null or empty"
            );
        }

        try {
            boolean deleted = beliefRepository.deleteById(beliefId);
            if (deleted) {
                totalStoreOperations++;
            }
            return deleted;
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to delete belief: " + e.getMessage(),
                e
            );
        }
    }

    // ========== Query Operations ==========

    @Override
    public List<Belief> getBeliefsForAgent(
        String agentId,
        boolean includeInactive
    ) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Agent ID cannot be null or empty"
            );
        }

        try {
            totalQueryOperations++;
            List<BeliefEntity> entities = beliefRepository.findByAgent(
                agentId,
                includeInactive
            );
            return entities
                .stream()
                .map(BeliefMapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to retrieve beliefs for agent: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public List<Belief> getBeliefsInCategory(
        String category,
        String agentId,
        boolean includeInactive
    ) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Category cannot be null or empty"
            );
        }

        try {
            totalQueryOperations++;
            List<BeliefEntity> entities = beliefRepository.findByCategory(
                category,
                agentId,
                includeInactive
            );
            return entities
                .stream()
                .map(BeliefMapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to retrieve beliefs in category: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public List<Belief> getAllActiveBeliefs() {
        try {
            totalQueryOperations++;
            List<BeliefEntity> entities = beliefRepository.findAllActive();
            return entities
                .stream()
                .map(BeliefMapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to retrieve all active beliefs: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public List<Belief> getAllBeliefs() {
        try {
            totalQueryOperations++;
            List<BeliefEntity> entities = beliefRepository.findAll();
            return entities
                .stream()
                .map(BeliefMapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to retrieve all beliefs: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public List<Belief> getLowConfidenceBeliefs(
        double confidenceThreshold,
        String agentId
    ) {
        if (confidenceThreshold < 0.0 || confidenceThreshold > 1.0) {
            throw new IllegalArgumentException(
                "Confidence threshold must be between 0.0 and 1.0"
            );
        }

        try {
            totalQueryOperations++;
            List<BeliefEntity> entities =
                beliefRepository.findLowConfidenceBeliefs(
                    confidenceThreshold,
                    agentId
                );
            return entities
                .stream()
                .map(BeliefMapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to retrieve low confidence beliefs: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public List<Belief> searchBeliefs(
        String searchText,
        String agentId,
        int limit
    ) {
        if (searchText == null || searchText.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Search text cannot be null or empty"
            );
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }

        try {
            totalSearchOperations++;
            List<BeliefEntity> entities = beliefRepository.searchByText(
                searchText,
                agentId,
                limit
            );
            return entities
                .stream()
                .map(BeliefMapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to search beliefs: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public List<SimilarBelief> findSimilarBeliefs(
        String statement,
        String agentId,
        double similarityThreshold,
        int limit
    ) {
        if (statement == null || statement.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Statement cannot be null or empty"
            );
        }
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException(
                "Similarity threshold must be between 0.0 and 1.0"
            );
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }

        try {
            totalSearchOperations++;

            // Use repository method that returns similarity scores
            List<BeliefRepository.SimilarityResult> results =
                beliefRepository.findSimilarBeliefsWithScores(
                    statement,
                    agentId,
                    similarityThreshold,
                    limit
                );

            // Convert to SimilarBelief objects with actual similarity scores from database
            return results
                .stream()
                .map(result ->
                    new SimilarBelief(
                        BeliefMapper.toDto(result.getEntity()),
                        result.getSimilarityScore()
                    )
                )
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to find similar beliefs: " + e.getMessage(),
                e
            );
        }
    }

    // ========== Conflict Management ==========

    @Override
    public BeliefConflict storeConflict(BeliefConflict conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict cannot be null");
        }
        if (
            conflict.getConflictId() == null ||
            conflict.getConflictId().trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                "Conflict ID cannot be null or empty"
            );
        }

        try {
            // Check if conflict already exists
            Optional<BeliefConflictEntity> existingEntity =
                conflictRepository.findById(conflict.getConflictId());

            BeliefConflictEntity entityToSave;
            if (existingEntity.isPresent()) {
                // Update existing entity
                entityToSave = existingEntity.get();
                BeliefConflictMapper.updateEntity(entityToSave, conflict);
            } else {
                // Create new entity
                entityToSave = BeliefConflictMapper.toEntity(conflict);
            }

            BeliefConflictEntity savedEntity = conflictRepository.save(
                entityToSave
            );
            totalStoreOperations++;

            return BeliefConflictMapper.toDto(savedEntity);
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to store conflict: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public Optional<BeliefConflict> getConflictById(String conflictId) {
        if (conflictId == null || conflictId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Conflict ID cannot be null or empty"
            );
        }

        try {
            totalQueryOperations++;
            Optional<BeliefConflictEntity> entity = conflictRepository.findById(
                conflictId
            );
            return entity.map(BeliefConflictMapper::toDto);
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to retrieve conflict: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public List<BeliefConflict> getUnresolvedConflicts(String agentId) {
        try {
            totalQueryOperations++;
            List<BeliefConflictEntity> entities =
                conflictRepository.findUnresolved(agentId);
            return entities
                .stream()
                .map(BeliefConflictMapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to retrieve unresolved conflicts: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public boolean removeConflict(String conflictId) {
        if (conflictId == null || conflictId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Conflict ID cannot be null or empty"
            );
        }

        try {
            boolean deleted = conflictRepository.deleteById(conflictId);
            if (deleted) {
                totalStoreOperations++;
            }
            return deleted;
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to remove conflict: " + e.getMessage(),
                e
            );
        }
    }

    // ========== Statistics and Analytics ==========

    @Override
    public Map<String, Object> getStorageStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalBeliefs", beliefRepository.count());
            stats.put("activeBeliefs", beliefRepository.countActive());
            stats.put("totalConflicts", conflictRepository.count());
            stats.put(
                "unresolvedConflicts",
                conflictRepository.countUnresolved(null)
            );
            stats.put("totalStoreOperations", totalStoreOperations);
            stats.put("totalQueryOperations", totalQueryOperations);
            stats.put("totalSearchOperations", totalSearchOperations);
            stats.put(
                "uptime",
                Instant.now().toEpochMilli() - createdAt.toEpochMilli()
            );

            // Calculate agent count
            stats.put("agentCount", getAgentCount());

            // Database-specific statistics
            stats.put("storageType", "postgresql_jpa");
            stats.put("persistenceProvider", "hibernate");

            // Database information
            Map<String, Object> databaseInfo = new HashMap<>();
            databaseInfo.put("productName", "PostgreSQL");
            databaseInfo.put("persistenceProvider", "Hibernate");
            databaseInfo.put("connectionPooling", "HikariCP");
            stats.put("databaseInfo", databaseInfo);

            return stats;
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to retrieve storage statistics: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Gets the count of distinct agents that have beliefs in the system.
     */
    private long getAgentCount() {
        try {
            return beliefRepository.countDistinctAgents();
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public Map<String, Long> getBeliefDistributionByCategory(String agentId) {
        try {
            totalQueryOperations++;
            List<BeliefRepository.CategoryDistribution> distributions =
                beliefRepository.getBeliefDistributionByCategory(agentId);

            return distributions
                .stream()
                .collect(
                    Collectors.toMap(
                        BeliefRepository.CategoryDistribution::getCategory,
                        BeliefRepository.CategoryDistribution::getCount
                    )
                );
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to get belief distribution by category: " +
                e.getMessage(),
                e
            );
        }
    }

    @Override
    public Map<String, Long> getBeliefDistributionByConfidence(String agentId) {
        try {
            totalQueryOperations++;
            List<BeliefRepository.ConfidenceDistribution> distributions =
                beliefRepository.getBeliefDistributionByConfidence(agentId);

            return distributions
                .stream()
                .collect(
                    Collectors.toMap(
                        BeliefRepository.ConfidenceDistribution::getConfidenceBucket,
                        BeliefRepository.ConfidenceDistribution::getCount
                    )
                );
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to get belief distribution by confidence: " +
                e.getMessage(),
                e
            );
        }
    }

    @Override
    public long countBeliefsForAgent(String agentId, boolean includeInactive) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Agent ID cannot be null or empty"
            );
        }

        try {
            totalQueryOperations++;
            return beliefRepository.countByAgent(agentId, includeInactive);
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to count beliefs for agent: " + e.getMessage(),
                e
            );
        }
    }

    // ========== Maintenance Operations ==========

    @Override
    public Map<String, Object> optimizeStorage() {
        Map<String, Object> results = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();

            // Flush pending changes
            beliefRepository.flush();
            conflictRepository.flush();

            // Clear persistence context to free memory
            beliefRepository.clear();
            conflictRepository.clear();

            long duration = System.currentTimeMillis() - startTime;

            results.put("operation", "optimize");
            results.put("duration", duration);
            results.put("optimizedAt", Instant.now());
            results.put("success", true);
            results.put(
                "note",
                "JPA optimization completed - flushed and cleared persistence context"
            );

            return results;
        } catch (Exception e) {
            results.put("success", false);
            results.put("error", e.getMessage());
            return results;
        }
    }

    @Override
    public Map<String, Object> validateIntegrity() {
        Map<String, Object> results = new HashMap<>();
        List<String> issues = new ArrayList<>();

        try {
            // Check for orphaned conflicts (conflicts referencing non-existent beliefs)
            List<BeliefConflictEntity> allConflicts =
                conflictRepository.findUnresolved(null);
            for (BeliefConflictEntity conflict : allConflicts) {
                for (String beliefId : conflict.getConflictingBeliefIds()) {
                    if (!beliefRepository.existsById(beliefId)) {
                        issues.add(
                            "Orphaned conflict " +
                            conflict.getId() +
                            " references non-existent belief " +
                            beliefId
                        );
                    }
                }
            }

            results.put("operation", "validate");
            results.put("validatedAt", Instant.now());
            results.put("issuesFound", issues.size());
            results.put("issues", issues);
            results.put("healthy", issues.isEmpty());

            return results;
        } catch (Exception e) {
            results.put("success", false);
            results.put("error", e.getMessage());
            return results;
        }
    }

    @Override
    public Map<String, Object> createBackup(
        String backupId,
        Map<String, Object> options
    ) {
        Map<String, Object> results = new HashMap<>();

        try {
            // For JPA implementation, backup would typically involve:
            // 1. Database backup using pg_dump or similar tools
            // 2. Export to file formats
            // 3. Cloud storage integration

            results.put("backupId", backupId);
            results.put("backupType", "jpa_postgresql");
            results.put("createdAt", Instant.now());
            results.put("beliefCount", beliefRepository.count());
            results.put("conflictCount", conflictRepository.count());
            results.put("success", false);
            results.put(
                "note",
                "JPA backup requires external database backup tools (pg_dump, etc.)"
            );

            return results;
        } catch (Exception e) {
            results.put("success", false);
            results.put("error", e.getMessage());
            return results;
        }
    }

    // ========== Health and Monitoring ==========

    @Override
    public boolean isHealthy() {
        try {
            // Test database connectivity by performing a simple query
            beliefRepository.count();
            conflictRepository.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new HashMap<>();

        try {
            boolean healthy = isHealthy();
            health.put("status", healthy ? "healthy" : "unhealthy");
            health.put("checkedAt", Instant.now());
            health.put("storageType", "postgresql_jpa");

            if (healthy) {
                health.put("beliefCount", beliefRepository.count());
                health.put("conflictCount", conflictRepository.count());
                health.put("databaseConnectivity", "ok");
            }

            health.put("statisticsSnapshot", getStorageStatistics());
        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
        }

        return health;
    }

    @Override
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("serviceType", "JpaBeliefStorageService");
        info.put("version", "1.0");
        info.put(
            "description",
            "JPA-based belief storage using PostgreSQL database"
        );
        info.put("persistence", "postgresql");
        info.put("scalability", "high");
        info.put("threadSafety", "full");
        info.put("backupSupport", "external_tools_required");
        info.put("transactionSupport", "full_acid");
        info.put(
            "queryCapabilities",
            java.util.Arrays.asList(
                "id-lookup",
                "agent-filter",
                "category-filter",
                "text-search",
                "similarity-search",
                "confidence-filtering",
                "batch-operations"
            )
        );
        info.put("createdAt", createdAt);

        return info;
    }

	@Override
	public long getTotalBeliefs() {
        try {
            return beliefRepository.count();
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to count total beliefs: " + e.getMessage(),
                e
            );
        }
	}

	@Override
	public long getActiveBeliefs() {
        try {
            return beliefRepository.countActive();
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to count active beliefs: " + e.getMessage(),
                e
            );
        }
	}

	@Override
	public long getTotalConflicts() {
        try {
            return conflictRepository.count();
        } catch (Exception e) {
            throw new BeliefStorageException(
                "Failed to count total conflicts: " + e.getMessage(),
                e
            );
        }
	}
}
