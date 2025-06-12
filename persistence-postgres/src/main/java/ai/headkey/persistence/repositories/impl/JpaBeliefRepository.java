package ai.headkey.persistence.repositories.impl;

import ai.headkey.persistence.entities.BeliefEntity;
import ai.headkey.persistence.repositories.BeliefRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * JPA implementation of BeliefRepository with comprehensive logging for debugging persistence issues.
 *
 * This implementation provides data access operations for BeliefEntity objects
 * using JPA EntityManager. It includes optimized queries, batch operations,
 * and comprehensive search capabilities for PostgreSQL backend.
 *
 * Key features:
 * - Named queries for performance optimization
 * - Batch operations for bulk updates
 * - Full-text search support
 * - Comprehensive indexing utilization
 * - Transaction management integration
 * - Detailed logging for debugging belief persistence issues
 *
 * LOGGING STRATEGY:
 * - DEBUG level: EntityManager lifecycle, transaction boundaries, query execution
 * - INFO level: Successful operations with counts and key identifiers
 * - WARN level: Collection fetch issues (HHH90003004), no-op maintenance operations
 * - ERROR level: Exceptions with full context including belief IDs and agent IDs
 * - TRACE level: Individual belief processing in batch operations
 *
 * COLLECTION FETCH WARNING (HHH90003004):
 * This repository intentionally uses LEFT JOIN FETCH for evidenceMemoryIds and tags
 * collections combined with setMaxResults() in findSimilarBeliefs() and searchByText().
 * This causes Hibernate to apply pagination in memory rather than at database level,
 * triggering HHH90003004 warnings. This is logged at WARN level for visibility.
 *
 * ENTITYMANAGER PATTERN:
 * Each method creates its own EntityManager from EntityManagerFactory and manages
 * its own transaction. This pattern ensures proper isolation but makes maintenance
 * operations like flush(), clear(), detach() no-ops (logged as warnings).
 *
 * @since 1.0
 */
public class JpaBeliefRepository implements BeliefRepository {

    private static final Logger LOG = Logger.getLogger(
        JpaBeliefRepository.class.getName()
    );

    private final EntityManagerFactory entityManagerFactory;

    /**
     * Constructor with EntityManagerFactory dependency.
     */
    public JpaBeliefRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
        LOG.info(
            "JpaBeliefRepository initialized with EntityManagerFactory pattern"
        );
        LOG.warn(
            "Collection fetch operations with pagination will trigger HHH90003004 warnings - this is expected behavior"
        );
    }

    // ========== Basic CRUD Operations ==========

    @Override
    public BeliefEntity save(BeliefEntity belief) {
        if (belief == null) {
            throw new IllegalArgumentException("Belief entity cannot be null");
        }

        LOG.debugf(
            "Starting save operation for belief ID: %s, Agent: %s",
            belief.getId(),
            belief.getAgentId()
        );
        LOG.tracef("Belief statement: %s", belief.getStatement());

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            LOG.debug("EntityManager created, starting transaction");
            em.getTransaction().begin();
            LOG.debug("Transaction started successfully");

            BeliefEntity result;
            boolean isUpdate =
                belief.getId() != null && existsById(belief.getId());

            if (isUpdate) {
                LOG.debugf(
                    "Updating existing belief with ID: %s",
                    belief.getId()
                );
                result = em.merge(belief);
                LOG.debugf("Belief merged successfully: %s", result.getId());
            } else {
                LOG.debugf("Persisting new belief with ID: %s", belief.getId());
                em.persist(belief);
                result = belief;
                LOG.debugf("Belief persisted successfully: %s", result.getId());
            }

            // Force flush to ensure data is written to database
            LOG.debug("Flushing EntityManager to database");
            em.flush();
            LOG.debug("EntityManager flushed successfully");

            LOG.debug("Committing transaction");
            em.getTransaction().commit();
            LOG.infof(
                "Belief %s saved successfully - ID: %s, Agent: %s, Active: %s",
                isUpdate ? "updated" : "created",
                result.getId(),
                result.getAgentId(),
                result.getActive()
            );

            // Log collection sizes for debugging
            LOG.debugf(
                "Belief collections - Evidence: %d, Tags: %d",
                result.getEvidenceMemoryIds().size(),
                result.getTags().size()
            );

            return result;
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Error saving belief ID: %s, Agent: %s - %s",
                belief.getId(),
                belief.getAgentId(),
                e.getMessage()
            );

            if (em.getTransaction().isActive()) {
                LOG.debug("Rolling back transaction due to error");
                em.getTransaction().rollback();
                LOG.debug("Transaction rolled back");
            }
            throw new RuntimeException(
                "Failed to save belief entity: " + e.getMessage(),
                e
            );
        } finally {
            LOG.debug("Closing EntityManager");
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> saveAll(List<BeliefEntity> beliefs) {
        if (beliefs == null) {
            throw new IllegalArgumentException("Beliefs list cannot be null");
        }

        LOG.infof(
            "Starting batch save operation for %d beliefs",
            beliefs.size()
        );

        EntityManager em = entityManagerFactory.createEntityManager();
        List<BeliefEntity> savedEntities = new ArrayList<>();

        try {
            LOG.debug("EntityManager created, starting batch transaction");
            em.getTransaction().begin();
            LOG.debug("Batch transaction started successfully");

            for (int i = 0; i < beliefs.size(); i++) {
                BeliefEntity belief = beliefs.get(i);
                LOG.tracef(
                    "Processing belief %d/%d - ID: %s, Agent: %s",
                    i + 1,
                    beliefs.size(),
                    belief.getId(),
                    belief.getAgentId()
                );

                BeliefEntity saved;
                boolean isUpdate =
                    belief.getId() != null &&
                    existsByIdInTransaction(belief.getId(), em);

                if (isUpdate) {
                    LOG.debugf(
                        "Merging existing belief %d: %s",
                        i + 1,
                        belief.getId()
                    );
                    saved = em.merge(belief);
                } else {
                    LOG.debugf(
                        "Persisting new belief %d: %s",
                        i + 1,
                        belief.getId()
                    );
                    em.persist(belief);
                    saved = belief;
                }
                savedEntities.add(saved);

                // Flush periodically for large batches
                if (i % 50 == 0) {
                    LOG.debugf(
                        "Flushing batch at position %d/%d",
                        i,
                        beliefs.size()
                    );
                    em.flush();
                    em.clear();
                    LOG.debug("Batch flush completed");
                }
            }

            LOG.debug("Flushing final batch before commit");
            em.flush();
            LOG.debug("Committing batch transaction");
            em.getTransaction().commit();
            LOG.infof(
                "Batch save completed successfully - %d beliefs saved",
                savedEntities.size()
            );
            return savedEntities;
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Error in batch save operation (processed %d/%d beliefs): %s",
                savedEntities.size(),
                beliefs.size(),
                e.getMessage()
            );

            if (em.getTransaction().isActive()) {
                LOG.debug("Rolling back batch transaction due to error");
                em.getTransaction().rollback();
                LOG.debug("Batch transaction rolled back");
            }
            throw new RuntimeException(
                "Failed to save beliefs batch: " + e.getMessage(),
                e
            );
        } finally {
            LOG.debug("Closing EntityManager for batch operation");
            em.close();
        }
    }

    @Override
    public Optional<BeliefEntity> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            LOG.debugf("FindById called with null or empty ID");
            return Optional.empty();
        }

        LOG.debugf("Finding belief by ID: %s", id);
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            LOG.tracef("Creating query for belief ID: %s with collections", id);
            TypedQuery<BeliefEntity> query = em.createQuery(
                "SELECT b FROM BeliefEntity b " +
                "LEFT JOIN FETCH b.evidenceMemoryIds " +
                "LEFT JOIN FETCH b.tags " +
                "WHERE b.id = :id",
                BeliefEntity.class
            );
            query.setParameter("id", id);
            LOG.debug("Executing findById query");
            BeliefEntity result = query.getSingleResult();
            LOG.debugf(
                "Belief found successfully - ID: %s, Agent: %s, Evidence: %d, Tags: %d",
                result.getId(),
                result.getAgentId(),
                result.getEvidenceMemoryIds().size(),
                result.getTags().size()
            );
            return Optional.of(result);
        } catch (NoResultException e) {
            LOG.debugf("No belief found with ID: %s", id);
            return Optional.empty();
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Error finding belief by ID %s: %s",
                id,
                e.getMessage()
            );
            throw new RuntimeException(
                "Failed to find belief by ID: " + e.getMessage(),
                e
            );
        } finally {
            LOG.debug("Closing EntityManager for findById");
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> findByIds(Set<String> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("IDs set cannot be null");
        }

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<BeliefEntity> query = em.createQuery(
                "SELECT DISTINCT b FROM BeliefEntity b " +
                "LEFT JOIN FETCH b.evidenceMemoryIds " +
                "LEFT JOIN FETCH b.tags " +
                "WHERE b.id IN :ids ORDER BY b.lastUpdated DESC",
                BeliefEntity.class
            );
            query.setParameter("ids", ids);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to find beliefs by IDs: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            BeliefEntity entity = em.find(BeliefEntity.class, id);
            if (entity != null) {
                em.remove(entity);
                em.getTransaction().commit();
                return true;
            }

            em.getTransaction().commit();
            return false;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException(
                "Failed to delete belief: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public void delete(BeliefEntity belief) {
        if (belief == null) {
            throw new IllegalArgumentException("Belief entity cannot be null");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            if (em.contains(belief)) {
                em.remove(belief);
            } else {
                BeliefEntity managed = em.find(
                    BeliefEntity.class,
                    belief.getId()
                );
                if (managed != null) {
                    em.remove(managed);
                }
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException(
                "Failed to delete belief entity: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public boolean existsById(String id) {
        if (id == null || id.trim().isEmpty()) {
            LOG.debugf("ExistsById called with null or empty ID");
            return false;
        }

        LOG.debugf("Checking if belief exists by ID: %s", id);
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b WHERE b.id = :id",
                Long.class
            );
            query.setParameter("id", id);
            long count = query.getSingleResult();
            boolean exists = count > 0;
            LOG.debugf(
                "Belief existence check for ID %s: %s (count: %d)",
                id,
                exists,
                count
            );
            return exists;
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Error checking belief existence for ID %s: %s",
                id,
                e.getMessage()
            );
            return false;
        } finally {
            LOG.debug("Closing EntityManager for existsById check");
            em.close();
        }
    }

    // ========== Query Operations ==========

    @Override
    public List<BeliefEntity> findByAgent(
        String agentId,
        boolean includeInactive
    ) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Agent ID cannot be null or empty"
            );
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<BeliefEntity> query = em.createQuery(
                "SELECT DISTINCT b FROM BeliefEntity b " +
                "LEFT JOIN FETCH b.evidenceMemoryIds " +
                "LEFT JOIN FETCH b.tags " +
                "WHERE b.agentId = :agentId AND (:includeInactive = true OR b.active = true) " +
                "ORDER BY b.lastUpdated DESC",
                BeliefEntity.class
            );
            query.setParameter("agentId", agentId);
            query.setParameter("includeInactive", includeInactive);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to find beliefs by agent: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> findByCategory(
        String category,
        String agentId,
        boolean includeInactive
    ) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Category cannot be null or empty"
            );
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<BeliefEntity> query = em.createQuery(
                "SELECT DISTINCT b FROM BeliefEntity b " +
                "LEFT JOIN FETCH b.evidenceMemoryIds " +
                "LEFT JOIN FETCH b.tags " +
                "WHERE b.category = :category AND (:agentId IS NULL OR b.agentId = :agentId) " +
                "AND (:includeInactive = true OR b.active = true) " +
                "ORDER BY b.lastUpdated DESC",
                BeliefEntity.class
            );
            query.setParameter("category", category);
            query.setParameter("agentId", agentId);
            query.setParameter("includeInactive", includeInactive);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to find beliefs by category: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> findAllActive() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<BeliefEntity> query = em.createQuery(
                "SELECT DISTINCT b FROM BeliefEntity b " +
                "LEFT JOIN FETCH b.evidenceMemoryIds " +
                "LEFT JOIN FETCH b.tags " +
                "WHERE b.active = true " +
                "ORDER BY b.lastUpdated DESC",
                BeliefEntity.class
            );
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to find all active beliefs: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> findAll() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<BeliefEntity> query = em.createQuery(
                "SELECT DISTINCT b FROM BeliefEntity b " +
                "LEFT JOIN FETCH b.evidenceMemoryIds " +
                "LEFT JOIN FETCH b.tags " +
                "ORDER BY b.lastUpdated DESC",
                BeliefEntity.class
            );
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to find all beliefs: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> findLowConfidenceBeliefs(
        double threshold,
        String agentId
    ) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                "Threshold must be between 0.0 and 1.0"
            );
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<BeliefEntity> query = em.createQuery(
                "SELECT DISTINCT b FROM BeliefEntity b " +
                "LEFT JOIN FETCH b.evidenceMemoryIds " +
                "LEFT JOIN FETCH b.tags " +
                "WHERE b.confidence < :threshold AND (:agentId IS NULL OR b.agentId = :agentId) " +
                "AND b.active = true " +
                "ORDER BY b.confidence ASC",
                BeliefEntity.class
            );
            query.setParameter("threshold", threshold);
            query.setParameter("agentId", agentId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to find low confidence beliefs: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> searchByText(
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

        LOG.debugf(
            "Searching beliefs by text: '%s', Agent: %s, Limit: %d",
            searchText,
            agentId,
            limit
        );

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            logCollectionFetchStrategy("searchByText", limit);
            LOG.debug(
                "Creating text search query with collection fetch - may trigger HHH90003004 warning"
            );
            TypedQuery<BeliefEntity> query = em.createQuery(
                "SELECT DISTINCT b FROM BeliefEntity b " +
                "LEFT JOIN FETCH b.evidenceMemoryIds " +
                "LEFT JOIN FETCH b.tags " +
                "WHERE LOWER(b.statement) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                "AND (:agentId IS NULL OR b.agentId = :agentId) " +
                "AND b.active = true " +
                "ORDER BY b.confidence DESC",
                BeliefEntity.class
            );
            query.setParameter("searchText", searchText);
            query.setParameter("agentId", agentId);
            query.setMaxResults(limit);

            LOG.debug(
                "Executing text search query with pagination and collection fetch"
            );
            List<BeliefEntity> results = query.getResultList();
            LOG.infof(
                "Text search completed - found %d beliefs for text: '%s'",
                results.size(),
                searchText
            );

            return results;
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Error searching beliefs by text '%s': %s",
                searchText,
                e.getMessage()
            );
            throw new RuntimeException(
                "Failed to search beliefs by text: " + e.getMessage(),
                e
            );
        } finally {
            LOG.debug("Closing EntityManager for text search");
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> findSimilarBeliefs(
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

        LOG.debugf(
            "Finding similar beliefs for statement: '%s', Agent: %s, Threshold: %.2f, Limit: %d",
            statement,
            agentId,
            similarityThreshold,
            limit
        );

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            // For now, use simple text search as a fallback
            // In a real implementation, this would use vector similarity with embeddings
            String searchText = extractKeywordsFromStatement(statement);
            LOG.debugf(
                "Extracted keywords for similarity search: '%s'",
                searchText
            );

            logCollectionFetchStrategy("findSimilarBeliefs", limit);
            LOG.warn(
                "Using collection fetch with pagination in findSimilarBeliefs - this causes HHH90003004 warning"
            );
            TypedQuery<BeliefEntity> query = em.createQuery(
                "SELECT DISTINCT b FROM BeliefEntity b " +
                "LEFT JOIN FETCH b.evidenceMemoryIds " +
                "LEFT JOIN FETCH b.tags " +
                "WHERE LOWER(b.statement) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                "AND (:agentId IS NULL OR b.agentId = :agentId) " +
                "AND b.active = true " +
                "ORDER BY b.confidence DESC",
                BeliefEntity.class
            );
            query.setParameter("searchText", searchText);
            query.setParameter("agentId", agentId);
            query.setMaxResults(limit);

            LOG.debug(
                "Executing similarity search query - expecting potential HHH90003004 warning"
            );
            List<BeliefEntity> results = query.getResultList();
            LOG.infof(
                "Similarity search completed - found %d similar beliefs for statement: '%s'",
                results.size(),
                statement
            );

            // Log collection sizes for each result to help with debugging
            for (int i = 0; i < results.size(); i++) {
                BeliefEntity belief = results.get(i);
                LOG.tracef(
                    "Similar belief %d - ID: %s, Evidence: %d, Tags: %d, Confidence: %.2f",
                    i + 1,
                    belief.getId(),
                    belief.getEvidenceMemoryIds().size(),
                    belief.getTags().size(),
                    belief.getConfidence()
                );
            }

            return results;
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Error finding similar beliefs for statement '%s': %s",
                statement,
                e.getMessage()
            );
            throw new RuntimeException(
                "Failed to find similar beliefs: " + e.getMessage(),
                e
            );
        } finally {
            LOG.debug("Closing EntityManager for similarity search");
            em.close();
        }
    }

    /**
     * Logs collection fetch strategy information for debugging HHH90003004 warnings.
     * This method helps identify when and why collection fetch warnings occur.
     */
    private void logCollectionFetchStrategy(String operation, int limit) {
        LOG.warnf(
            "COLLECTION FETCH ANALYSIS - Operation: %s, Limit: %d",
            operation,
            limit
        );
        LOG.warn(
            "This operation uses 'LEFT JOIN FETCH' with collections + setMaxResults()"
        );
        LOG.warn(
            "Expected behavior: HHH90003004 warning about in-memory pagination"
        );
        LOG.warn(
            "Impact: Hibernate loads all matching records then applies limit in memory"
        );
        LOG.warn(
            "Solution options: 1) Use projection queries 2) Separate collection loading 3) Accept warning for simplicity"
        );
    }

    /**
     * Logs current repository operation statistics for monitoring and debugging.
     * This method provides insight into database activity and performance.
     */
    public void logRepositoryStatistics() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            LOG.info("=== JpaBeliefRepository Statistics ===");

            // Total beliefs count
            TypedQuery<Long> totalQuery = em.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b",
                Long.class
            );
            long totalBeliefs = totalQuery.getSingleResult();
            LOG.infof("Total beliefs in database: %d", totalBeliefs);

            // Active beliefs count
            TypedQuery<Long> activeQuery = em.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b WHERE b.active = true",
                Long.class
            );
            long activeBeliefs = activeQuery.getSingleResult();
            LOG.infof("Active beliefs: %d", activeBeliefs);

            // Distinct agents count
            TypedQuery<Long> agentsQuery = em.createQuery(
                "SELECT COUNT(DISTINCT b.agentId) FROM BeliefEntity b",
                Long.class
            );
            long distinctAgents = agentsQuery.getSingleResult();
            LOG.infof("Distinct agents with beliefs: %d", distinctAgents);

            // Evidence records count
            TypedQuery<Long> evidenceQuery = em.createQuery(
                "SELECT COUNT(e) FROM BeliefEntity b JOIN b.evidenceMemoryIds e",
                Long.class
            );
            long evidenceCount = evidenceQuery.getSingleResult();
            LOG.infof("Total evidence records: %d", evidenceCount);

            // Tags count
            TypedQuery<Long> tagsQuery = em.createQuery(
                "SELECT COUNT(t) FROM BeliefEntity b JOIN b.tags t",
                Long.class
            );
            long tagsCount = tagsQuery.getSingleResult();
            LOG.infof("Total tag records: %d", tagsCount);

            LOG.info("=== End Repository Statistics ===");
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Error gathering repository statistics: %s",
                e.getMessage()
            );
        } finally {
            em.close();
        }
    }

    // ========== Statistics and Analytics ==========

    @Override
    public long countByAgent(String agentId, boolean includeInactive) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Agent ID cannot be null or empty"
            );
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Long> query = em.createNamedQuery(
                "BeliefEntity.countByAgent",
                Long.class
            );
            query.setParameter("agentId", agentId);
            query.setParameter("includeInactive", includeInactive);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to count beliefs by agent: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public long count() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b",
                Long.class
            );
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to count all beliefs: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public long countActive() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b WHERE b.active = true",
                Long.class
            );
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to count active beliefs: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public long countDistinctAgents() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(DISTINCT b.agentId) FROM BeliefEntity b WHERE b.active = true",
                Long.class
            );
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to count distinct agents: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public List<CategoryDistribution> getBeliefDistributionByCategory(
        String agentId
    ) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql =
                "SELECT b.category, COUNT(b) FROM BeliefEntity b " +
                "WHERE b.active = true AND b.category IS NOT NULL " +
                (agentId != null ? "AND b.agentId = :agentId " : "") +
                "GROUP BY b.category ORDER BY COUNT(b) DESC";

            TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }

            return query
                .getResultList()
                .stream()
                .map(result ->
                    new CategoryDistribution(
                        (String) result[0],
                        (Long) result[1]
                    )
                )
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to get belief distribution by category: " +
                e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public List<ConfidenceDistribution> getBeliefDistributionByConfidence(
        String agentId
    ) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql =
                "SELECT " +
                "CASE " +
                "  WHEN b.confidence >= 0.8 THEN 'high' " +
                "  WHEN b.confidence >= 0.5 THEN 'medium' " +
                "  ELSE 'low' " +
                "END, COUNT(b) " +
                "FROM BeliefEntity b " +
                "WHERE b.active = true " +
                (agentId != null ? "AND b.agentId = :agentId " : "") +
                "GROUP BY " +
                "CASE " +
                "  WHEN b.confidence >= 0.8 THEN 'high' " +
                "  WHEN b.confidence >= 0.5 THEN 'medium' " +
                "  ELSE 'low' " +
                "END";

            TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }

            return query
                .getResultList()
                .stream()
                .map(result ->
                    new ConfidenceDistribution(
                        (String) result[0],
                        (Long) result[1]
                    )
                )
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to get belief distribution by confidence: " +
                e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    // ========== Batch Operations ==========

    @Override
    public int updateBatch(List<BeliefEntity> beliefs) {
        if (beliefs == null) {
            throw new IllegalArgumentException("Beliefs list cannot be null");
        }

        if (beliefs.isEmpty()) {
            return 0;
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            int updated = 0;
            for (int i = 0; i < beliefs.size(); i++) {
                em.merge(beliefs.get(i));
                updated++;

                // Flush periodically for large batches
                if (i % 50 == 0) {
                    em.flush();
                    em.clear();
                }
            }

            em.getTransaction().commit();
            return updated;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException(
                "Failed to update beliefs in batch: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public int deactivateBatch(List<String> beliefIds) {
        if (beliefIds == null) {
            throw new IllegalArgumentException(
                "Belief IDs list cannot be null"
            );
        }

        if (beliefIds.isEmpty()) {
            return 0;
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            TypedQuery<Integer> query = em.createQuery(
                "UPDATE BeliefEntity b SET b.active = false, b.lastUpdated = CURRENT_TIMESTAMP WHERE b.id IN :ids",
                Integer.class
            );
            query.setParameter("ids", beliefIds);
            int result = query.executeUpdate();

            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException(
                "Failed to deactivate beliefs in batch: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    @Override
    public int deleteBatch(List<String> beliefIds) {
        if (beliefIds == null) {
            throw new IllegalArgumentException(
                "Belief IDs list cannot be null"
            );
        }

        if (beliefIds.isEmpty()) {
            return 0;
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            TypedQuery<Integer> query = em.createQuery(
                "DELETE FROM BeliefEntity b WHERE b.id IN :ids",
                Integer.class
            );
            query.setParameter("ids", beliefIds);
            int result = query.executeUpdate();

            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException(
                "Failed to delete beliefs in batch: " + e.getMessage(),
                e
            );
        } finally {
            em.close();
        }
    }

    // ========== Maintenance Operations ==========

    @Override
    public void flush() {
        // Note: flush() doesn't make sense with EntityManagerFactory pattern
        // since each operation uses its own EntityManager
        // This method is kept for interface compatibility but is essentially a no-op
        LOG.warn(
            "flush() called but has no effect - JpaBeliefRepository uses EntityManagerFactory pattern where each operation manages its own EntityManager"
        );
    }

    @Override
    public void clear() {
        // Note: clear() doesn't make sense with EntityManagerFactory pattern
        // since each operation uses its own EntityManager
        // This method is kept for interface compatibility but is essentially a no-op
        LOG.warn(
            "clear() called but has no effect - JpaBeliefRepository uses EntityManagerFactory pattern where each operation manages its own EntityManager"
        );
    }

    @Override
    public void detach(BeliefEntity belief) {
        // Note: detach() doesn't make sense with EntityManagerFactory pattern
        // since each operation uses its own EntityManager
        // This method is kept for interface compatibility but is essentially a no-op
        LOG.warnf(
            "detach() called for belief ID %s but has no effect - JpaBeliefRepository uses EntityManagerFactory pattern where each operation manages its own EntityManager",
            belief != null ? belief.getId() : "null"
        );
    }

    @Override
    public void refresh(BeliefEntity belief) {
        if (belief == null) {
            throw new IllegalArgumentException("Belief entity cannot be null");
        }

        LOG.debugf("Refreshing belief entity ID: %s", belief.getId());
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            BeliefEntity managedEntity = em.find(
                BeliefEntity.class,
                belief.getId()
            );
            if (managedEntity != null) {
                LOG.debugf(
                    "Found managed entity for refresh, updating belief ID: %s",
                    belief.getId()
                );
                em.refresh(managedEntity);
                // Copy refreshed state back to the provided entity
                // Note: This is a simplified approach and may not cover all fields
                belief.setStatement(managedEntity.getStatement());
                belief.setConfidence(managedEntity.getConfidence());
                belief.setActive(managedEntity.getActive());
                // ... copy other fields as needed
                LOG.debugf(
                    "Belief refreshed successfully - ID: %s",
                    belief.getId()
                );
            } else {
                LOG.warnf(
                    "No managed entity found for refresh - belief ID: %s may not exist in database",
                    belief.getId()
                );
            }
        } catch (Exception e) {
            LOG.errorf(
                e,
                "Error refreshing belief entity ID %s: %s",
                belief.getId(),
                e.getMessage()
            );
            throw new RuntimeException(
                "Failed to refresh belief entity: " + e.getMessage(),
                e
            );
        } finally {
            LOG.debug("Closing EntityManager for refresh operation");
            em.close();
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Checks if an entity exists by ID within a transaction context.
     * Used by batch operations that already have an EntityManager.
     *
     * @param id The ID to check
     * @param em The EntityManager to use
     * @return true if entity exists, false otherwise
     */
    private boolean existsByIdInTransaction(String id, EntityManager em) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b WHERE b.id = :id",
                Long.class
            );
            query.setParameter("id", id);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts keywords from a statement for similarity search.
     *
     * @param statement The statement to extract keywords from
     * @return Keywords for search
     */
    private String extractKeywordsFromStatement(String statement) {
        if (statement == null || statement.trim().isEmpty()) {
            return "";
        }

        // Simple keyword extraction - remove common words and keep important terms
        String[] words = statement.toLowerCase().split("\\s+");
        Set<String> stopWords = Set.of(
            "the",
            "a",
            "an",
            "and",
            "or",
            "but",
            "in",
            "on",
            "at",
            "to",
            "for",
            "of",
            "with",
            "by",
            "is",
            "are",
            "was",
            "were"
        );

        return Arrays.stream(words)
            .filter(word -> word.length() > 2)
            .filter(word -> !stopWords.contains(word))
            .limit(5) // Take top 5 keywords
            .collect(Collectors.joining(" "));
    }
}
