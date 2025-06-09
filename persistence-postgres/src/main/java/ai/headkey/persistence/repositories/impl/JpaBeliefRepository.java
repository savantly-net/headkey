package ai.headkey.persistence.repositories.impl;

import ai.headkey.persistence.entities.BeliefEntity;
import ai.headkey.persistence.repositories.BeliefRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of BeliefRepository.
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
 * 
 * @since 1.0
 */
public class JpaBeliefRepository implements BeliefRepository {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * Constructor with EntityManagerFactory dependency.
     */
    public JpaBeliefRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    // ========== Basic CRUD Operations ==========

    @Override
    public BeliefEntity save(BeliefEntity belief) {
        if (belief == null) {
            throw new IllegalArgumentException("Belief entity cannot be null");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            
            BeliefEntity result;
            if (belief.getId() != null && existsById(belief.getId())) {
                result = em.merge(belief);
            } else {
                em.persist(belief);
                result = belief;
            }
            
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save belief entity: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> saveAll(List<BeliefEntity> beliefs) {
        if (beliefs == null) {
            throw new IllegalArgumentException("Beliefs list cannot be null");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        List<BeliefEntity> savedEntities = new ArrayList<>();
        
        try {
            em.getTransaction().begin();
            
            for (int i = 0; i < beliefs.size(); i++) {
                BeliefEntity belief = beliefs.get(i);
                
                BeliefEntity saved;
                if (belief.getId() != null && existsByIdInTransaction(belief.getId(), em)) {
                    saved = em.merge(belief);
                } else {
                    em.persist(belief);
                    saved = belief;
                }
                savedEntities.add(saved);
                
                // Flush periodically for large batches
                if (i % 50 == 0) {
                    em.flush();
                    em.clear();
                }
            }
            
            em.getTransaction().commit();
            return savedEntities;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save belief entities: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<BeliefEntity> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<BeliefEntity> query = em.createQuery(
                "SELECT b FROM BeliefEntity b " +
                "LEFT JOIN FETCH b.evidenceMemoryIds " +
                "LEFT JOIN FETCH b.tags " +
                "WHERE b.id = :id", 
                BeliefEntity.class
            );
            query.setParameter("id", id);
            BeliefEntity entity = query.getResultStream().findFirst().orElse(null);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find belief by ID: " + e.getMessage(), e);
        } finally {
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
            throw new RuntimeException("Failed to find beliefs by IDs: " + e.getMessage(), e);
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
            throw new RuntimeException("Failed to delete belief: " + e.getMessage(), e);
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
                BeliefEntity managed = em.find(BeliefEntity.class, belief.getId());
                if (managed != null) {
                    em.remove(managed);
                }
            }
            
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete belief entity: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public boolean existsById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b WHERE b.id = :id",
                Long.class
            );
            query.setParameter("id", id);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            return false;
        } finally {
            em.close();
        }
    }

    // ========== Query Operations ==========

    @Override
    public List<BeliefEntity> findByAgent(String agentId, boolean includeInactive) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
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
            throw new RuntimeException("Failed to find beliefs by agent: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> findByCategory(String category, String agentId, boolean includeInactive) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
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
            throw new RuntimeException("Failed to find beliefs by category: " + e.getMessage(), e);
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
            throw new RuntimeException("Failed to find all active beliefs: " + e.getMessage(), e);
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
            throw new RuntimeException("Failed to find all beliefs: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> findLowConfidenceBeliefs(double threshold, String agentId) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
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
            throw new RuntimeException("Failed to find low confidence beliefs: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> searchByText(String searchText, String agentId, int limit) {
        if (searchText == null || searchText.trim().isEmpty()) {
            throw new IllegalArgumentException("Search text cannot be null or empty");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
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
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to search beliefs by text: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefEntity> findSimilarBeliefs(String statement, String agentId, double similarityThreshold, int limit) {
        if (statement == null || statement.trim().isEmpty()) {
            throw new IllegalArgumentException("Statement cannot be null or empty");
        }
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            // For now, use simple text search as a fallback
            // In a real implementation, this would use vector similarity with embeddings
            String searchText = extractKeywordsFromStatement(statement);
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
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find similar beliefs: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // ========== Statistics and Analytics ==========

    @Override
    public long countByAgent(String agentId, boolean includeInactive) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Long> query = em.createNamedQuery("BeliefEntity.countByAgent", Long.class);
            query.setParameter("agentId", agentId);
            query.setParameter("includeInactive", includeInactive);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count beliefs by agent: " + e.getMessage(), e);
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
            throw new RuntimeException("Failed to count all beliefs: " + e.getMessage(), e);
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
            throw new RuntimeException("Failed to count active beliefs: " + e.getMessage(), e);
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
            throw new RuntimeException("Failed to count distinct agents: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<CategoryDistribution> getBeliefDistributionByCategory(String agentId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql = "SELECT b.category, COUNT(b) FROM BeliefEntity b " +
                         "WHERE b.active = true AND b.category IS NOT NULL " +
                         (agentId != null ? "AND b.agentId = :agentId " : "") +
                         "GROUP BY b.category ORDER BY COUNT(b) DESC";
            
            TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            
            return query.getResultList().stream()
                .map(result -> new CategoryDistribution((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get belief distribution by category: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<ConfidenceDistribution> getBeliefDistributionByConfidence(String agentId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql = "SELECT " +
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
            
            return query.getResultList().stream()
                .map(result -> new ConfidenceDistribution((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get belief distribution by confidence: " + e.getMessage(), e);
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
            throw new RuntimeException("Failed to update beliefs in batch: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public int deactivateBatch(List<String> beliefIds) {
        if (beliefIds == null) {
            throw new IllegalArgumentException("Belief IDs list cannot be null");
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
            throw new RuntimeException("Failed to deactivate beliefs in batch: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public int deleteBatch(List<String> beliefIds) {
        if (beliefIds == null) {
            throw new IllegalArgumentException("Belief IDs list cannot be null");
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
            throw new RuntimeException("Failed to delete beliefs in batch: " + e.getMessage(), e);
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
    }

    @Override
    public void clear() {
        // Note: clear() doesn't make sense with EntityManagerFactory pattern
        // since each operation uses its own EntityManager
        // This method is kept for interface compatibility but is essentially a no-op
    }

    @Override
    public void detach(BeliefEntity belief) {
        // Note: detach() doesn't make sense with EntityManagerFactory pattern
        // since each operation uses its own EntityManager
        // This method is kept for interface compatibility but is essentially a no-op
    }

    @Override
    public void refresh(BeliefEntity belief) {
        if (belief == null) {
            throw new IllegalArgumentException("Belief entity cannot be null");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            BeliefEntity managedEntity = em.find(BeliefEntity.class, belief.getId());
            if (managedEntity != null) {
                em.refresh(managedEntity);
                // Copy refreshed values back to the parameter
                belief.setStatement(managedEntity.getStatement());
                belief.setConfidence(managedEntity.getConfidence());
                belief.setActive(managedEntity.getActive());
                belief.setLastUpdated(managedEntity.getLastUpdated());
                // Copy other fields as needed
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh belief entity: " + e.getMessage(), e);
        } finally {
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
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were");
        
        return Arrays.stream(words)
            .filter(word -> word.length() > 2)
            .filter(word -> !stopWords.contains(word))
            .limit(5) // Take top 5 keywords
            .collect(Collectors.joining(" "));
    }
}