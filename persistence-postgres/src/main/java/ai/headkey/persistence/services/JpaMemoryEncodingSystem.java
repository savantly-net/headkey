package ai.headkey.persistence.services;

import ai.headkey.memory.abstracts.AbstractMemoryEncodingSystem;
import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.persistence.entities.MemoryEntity;
import ai.headkey.memory.exceptions.MemoryNotFoundException;
import ai.headkey.memory.exceptions.StorageException;
import ai.headkey.persistence.strategies.jpa.JpaSimilaritySearchStrategy;
import ai.headkey.persistence.strategies.jpa.JpaSimilaritySearchStrategyFactory;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Jakarta Persistence (JPA) implementation of the MemoryEncodingSystem.
 * 
 * This implementation extends AbstractMemoryEncodingSystem to provide
 * persistent storage using JPA entities and the Jakarta Persistence API.
 * It supports:
 * 
 * - Persistent storage in relational databases
 * - Vector embedding storage and similarity search
 * - Transaction management for data consistency
 * - Optimized queries using JPA Criteria API and named queries
 * - Connection pooling and database optimization
 * - Full CRUD operations with proper error handling
 * 
 * The implementation uses MemoryEntity for database mapping and leverages
 * the abstract base class for common functionality like embedding generation,
 * statistics tracking, and input validation.
 * 
 * Vector similarity search is implemented using in-memory calculations
 * after database retrieval, which works well for moderate datasets.
 * For large-scale deployments, consider database-specific vector extensions
 * like PostgreSQL's pgvector.
 * 
 * @since 1.0
 */
public class JpaMemoryEncodingSystem extends AbstractMemoryEncodingSystem {
    
    private final EntityManagerFactory entityManagerFactory;
    private final String persistenceUnitName;
    private final int batchSize;
    private final boolean enableSecondLevelCache;
    private final JpaSimilaritySearchStrategy similaritySearchStrategy;
    
    // Configuration for similarity search
    private final int maxSimilaritySearchResults;
    private final double similarityThreshold;
    
    /**
     * Creates a new JPA-based memory encoding system with default configuration.
     * 
     * @param entityManagerFactory The JPA EntityManagerFactory
     */
    public JpaMemoryEncodingSystem(EntityManagerFactory entityManagerFactory) {
        this(entityManagerFactory, null, 100, true, 1000, 0.0, null);
    }
    
    /**
     * Creates a new JPA-based memory encoding system with embedding generator.
     * 
     * @param entityManagerFactory The JPA EntityManagerFactory
     * @param embeddingGenerator Function to generate vector embeddings
     */
    public JpaMemoryEncodingSystem(EntityManagerFactory entityManagerFactory, 
                                  VectorEmbeddingGenerator embeddingGenerator) {
        this(entityManagerFactory, embeddingGenerator, 100, true, 1000, 0.0, null);
    }
    
    /**
     * Creates a new JPA-based memory encoding system with full configuration.
     * 
     * @param entityManagerFactory The JPA EntityManagerFactory
     * @param embeddingGenerator Function to generate vector embeddings
     * @param batchSize Batch size for bulk operations
     * @param enableSecondLevelCache Whether to enable JPA second-level cache
     * @param maxSimilaritySearchResults Maximum results for similarity search
     * @param similarityThreshold Minimum similarity score for search results
     */
    public JpaMemoryEncodingSystem(EntityManagerFactory entityManagerFactory,
                                  VectorEmbeddingGenerator embeddingGenerator,
                                  int batchSize,
                                  boolean enableSecondLevelCache,
                                  int maxSimilaritySearchResults,
                                  double similarityThreshold) {
        this(entityManagerFactory, embeddingGenerator, batchSize, enableSecondLevelCache, 
             maxSimilaritySearchResults, similarityThreshold, null);
    }

    /**
     * Creates a new JPA-based memory encoding system with full configuration and custom similarity strategy.
     * 
     * @param entityManagerFactory The JPA EntityManagerFactory
     * @param embeddingGenerator Function to generate vector embeddings
     * @param batchSize Batch size for bulk operations
     * @param enableSecondLevelCache Whether to enable JPA second-level cache
     * @param maxSimilaritySearchResults Maximum results for similarity search
     * @param similarityThreshold Minimum similarity score for search results
     * @param similaritySearchStrategy Custom similarity search strategy (null for auto-detection)
     */
    public JpaMemoryEncodingSystem(EntityManagerFactory entityManagerFactory,
                                  VectorEmbeddingGenerator embeddingGenerator,
                                  int batchSize,
                                  boolean enableSecondLevelCache,
                                  int maxSimilaritySearchResults,
                                  double similarityThreshold,
                                  JpaSimilaritySearchStrategy similaritySearchStrategy) {
        super(embeddingGenerator);
        this.entityManagerFactory = entityManagerFactory;
        this.persistenceUnitName = null;
        this.batchSize = Math.max(1, batchSize);
        this.enableSecondLevelCache = enableSecondLevelCache;
        this.maxSimilaritySearchResults = Math.max(1, maxSimilaritySearchResults);
        this.similarityThreshold = Math.max(0.0, Math.min(1.0, similarityThreshold));
        
        validateEntityManagerFactory();
        
        // Initialize similarity search strategy
        if (similaritySearchStrategy != null) {
            this.similaritySearchStrategy = similaritySearchStrategy;
        } else {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                this.similaritySearchStrategy = JpaSimilaritySearchStrategyFactory.createStrategy(em);
                this.similaritySearchStrategy.initialize(em);
            } catch (Exception e) {
                throw new StorageException("Failed to initialize similarity search strategy", e);
            } finally {
                em.close();
            }
        }
    }
    
    @Override
    protected MemoryRecord doEncodeAndStore(String content, CategoryLabel category, 
                                          Metadata meta, double[] embedding) {
        EntityManager em = null;
        EntityTransaction transaction = null;
        MemoryEntity entity = null;
        
        try {
            // Validate inputs more thoroughly
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("Content cannot be null or empty");
            }
            if (category == null) {
                throw new IllegalArgumentException("Category cannot be null");
            }
            if (meta == null) {
                throw new IllegalArgumentException("Metadata cannot be null");
            }
            
            em = entityManagerFactory.createEntityManager();
            transaction = em.getTransaction();
            
            transaction.begin();
            
            // Create new memory entity
            entity = new MemoryEntity();
            String memoryId = generateMemoryId();
            String agentId = getAgentIdFromMetadata(meta);
            
            entity.setId(memoryId);
            entity.setAgentId(agentId);
            entity.setContent(content);
            entity.setCategory(category);
            entity.setMetadata(meta);
            entity.setEmbedding(embedding);
            entity.setCreatedAt(Instant.now());
            entity.setLastAccessed(Instant.now());
            entity.setRelevanceScore(getInitialRelevanceScore(meta));
            
            // Persist the entity
            em.persist(entity);
            em.flush(); // Ensure ID is generated
            
            transaction.commit();
            
            return entity.toMemoryRecord();
            
        } catch (Exception e) {
            System.err.println("DETAILED ERROR in doEncodeAndStore:");
            System.err.println("  Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.err.println("  Memory ID: " + (entity != null ? entity.getId() : "null"));
            System.err.println("  Agent ID: " + (entity != null ? entity.getAgentId() : "null"));
            System.err.println("  Content length: " + (content != null ? content.length() : "null"));
            if (e.getCause() != null) {
                System.err.println("  Root cause: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
            e.printStackTrace();
            
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception rollbackException) {
                    System.err.println("ERROR during rollback: " + rollbackException.getMessage());
                }
            }
            
            // Re-throw with more context
            throw new StorageException("Failed to store memory - " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } finally {
            if (em != null) {
                try {
                    em.close();
                } catch (Exception closeException) {
                    System.err.println("ERROR closing EntityManager: " + closeException.getMessage());
                }
            }
        }
    }
    
    @Override
    protected Optional<MemoryRecord> doGetMemory(String memoryId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            MemoryEntity entity = em.find(MemoryEntity.class, memoryId);
            
            if (entity != null) {
                // Update last accessed time
                updateLastAccessedInTransaction(em, entity);
                return Optional.of(entity.toMemoryRecord());
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve memory: " + memoryId, e);
        } finally {
            em.close();
        }
    }
    
    @Override
    protected Map<String, MemoryRecord> doGetMemories(Set<String> memoryIds) {
        if (memoryIds.isEmpty()) {
            return new HashMap<>();
        }
        
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            // Use batch retrieval for efficiency
            List<String> idList = new ArrayList<>(memoryIds);
            Map<String, MemoryRecord> results = new HashMap<>();
            
            // Process in batches to avoid query parameter limits
            for (int i = 0; i < idList.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, idList.size());
                List<String> batchIds = idList.subList(i, endIndex);
                
                TypedQuery<MemoryEntity> query = em.createQuery(
                    "SELECT m FROM MemoryEntity m WHERE m.id IN :ids", MemoryEntity.class);
                query.setParameter("ids", batchIds);
                
                List<MemoryEntity> entities = query.getResultList();
                
                // Update last accessed times and convert to DTOs
                EntityTransaction transaction = em.getTransaction();
                transaction.begin();
                
                for (MemoryEntity entity : entities) {
                    entity.updateLastAccessed();
                    results.put(entity.getId(), entity.toMemoryRecord());
                }
                
                transaction.commit();
            }
            
            return results;
            
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve memories", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    protected MemoryRecord doUpdateMemory(MemoryRecord memoryRecord, double[] embedding) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        
        try {
            transaction.begin();
            
            MemoryEntity entity = em.find(MemoryEntity.class, memoryRecord.getId());
            if (entity == null) {
                throw new MemoryNotFoundException("Memory not found: " + memoryRecord.getId());
            }
            
            // Update entity fields
            entity.setContent(memoryRecord.getContent());
            entity.setCategory(memoryRecord.getCategory());
            entity.setMetadata(memoryRecord.getMetadata());
            entity.setRelevanceScore(memoryRecord.getRelevanceScore());
            entity.setEmbedding(embedding);
            entity.updateLastAccessed();
            
            // Merge changes
            entity = em.merge(entity);
            
            transaction.commit();
            
            return entity.toMemoryRecord();
            
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            if (e instanceof MemoryNotFoundException) {
                throw new StorageException("Memory not found: " + memoryRecord.getId(), e);
            }
            throw new StorageException("Failed to update memory: " + memoryRecord.getId(), e);
        } finally {
            em.close();
        }
    }
    
    @Override
    protected boolean doRemoveMemory(String memoryId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        
        try {
            transaction.begin();
            
            MemoryEntity entity = em.find(MemoryEntity.class, memoryId);
            if (entity != null) {
                em.remove(entity);
                transaction.commit();
                return true;
            }
            
            transaction.rollback();
            return false;
            
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new StorageException("Failed to remove memory: " + memoryId, e);
        } finally {
            em.close();
        }
    }
    
    @Override
    protected Set<String> doRemoveMemories(Set<String> memoryIds) {
        if (memoryIds.isEmpty()) {
            return new HashSet<>();
        }
        
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        Set<String> removedIds = new HashSet<>();
        
        try {
            transaction.begin();
            
            // Process in batches
            List<String> idList = new ArrayList<>(memoryIds);
            
            for (int i = 0; i < idList.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, idList.size());
                List<String> batchIds = idList.subList(i, endIndex);
                
                // Use bulk delete query for efficiency
                Query deleteQuery = em.createQuery(
                    "DELETE FROM MemoryEntity m WHERE m.id IN :ids");
                deleteQuery.setParameter("ids", batchIds);
                
                int deletedCount = deleteQuery.executeUpdate();
                
                // Track which IDs were actually deleted
                if (deletedCount > 0) {
                    // For simplicity, assume all were deleted
                    // In production, you might want to verify which specific ones were deleted
                    removedIds.addAll(batchIds);
                }
            }
            
            transaction.commit();
            
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new StorageException("Failed to remove memories", e);
        } finally {
            em.close();
        }
        
        return removedIds;
    }
    
    @Override
    protected List<MemoryRecord> doSearchSimilar(String queryContent, double[] queryEmbedding, int limit) {
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            return similaritySearchStrategy.searchSimilar(
                em, queryContent, queryEmbedding, null, limit, 
                maxSimilaritySearchResults, similarityThreshold);
        } catch (Exception e) {
            throw new StorageException("Failed to search similar memories", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<MemoryRecord> getMemoriesForAgent(String agentId, int limit) {
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            TypedQuery<MemoryEntity> query;
            
            if (limit > 0) {
                query = em.createNamedQuery("MemoryEntity.findByAgentIdAndLimit", MemoryEntity.class);
                query.setMaxResults(limit);
            } else {
                query = em.createNamedQuery("MemoryEntity.findByAgentId", MemoryEntity.class);
            }
            
            query.setParameter("agentId", agentId);
            
            List<MemoryEntity> entities = query.getResultList();
            
            // Update access times and convert to DTOs
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            
            List<MemoryRecord> results = entities.stream()
                .peek(MemoryEntity::updateLastAccessed)
                .map(MemoryEntity::toMemoryRecord)
                .collect(Collectors.toList());
            
            transaction.commit();
            
            return results;
            
        } catch (Exception e) {
            throw new StorageException("Failed to get memories for agent: " + agentId, e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<MemoryRecord> getMemoriesInCategory(String category, String agentId, int limit) {
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<MemoryEntity> query = cb.createQuery(MemoryEntity.class);
            Root<MemoryEntity> root = query.from(MemoryEntity.class);
            
            // Build dynamic query based on parameters
            List<Predicate> predicates = new ArrayList<>();
            
            // Category filter - this is simplified; in reality, you'd need to query JSON
            // For production, consider adding a separate category field or using database-specific JSON queries
            predicates.add(cb.like(cb.lower(root.get("category").as(String.class)), 
                                 "%" + category.toLowerCase() + "%"));
            
            if (agentId != null && !agentId.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("agentId"), agentId));
            }
            
            query.where(predicates.toArray(new Predicate[0]));
            query.orderBy(cb.desc(root.get("createdAt")));
            
            TypedQuery<MemoryEntity> typedQuery = em.createQuery(query);
            if (limit > 0) {
                typedQuery.setMaxResults(limit);
            }
            
            List<MemoryEntity> entities = typedQuery.getResultList();
            
            // Update access times and convert to DTOs
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            
            List<MemoryRecord> results = entities.stream()
                .peek(MemoryEntity::updateLastAccessed)
                .map(MemoryEntity::toMemoryRecord)
                .collect(Collectors.toList());
            
            transaction.commit();
            
            return results;
            
        } catch (Exception e) {
            throw new StorageException("Failed to get memories in category: " + category, e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public List<MemoryRecord> getOldMemories(long olderThanSeconds, String agentId, int limit) {
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            Instant threshold = Instant.now().minusSeconds(olderThanSeconds);
            
            TypedQuery<MemoryEntity> query;
            
            if (agentId != null && !agentId.trim().isEmpty()) {
                query = em.createNamedQuery("MemoryEntity.findOldMemoriesByAgent", MemoryEntity.class);
                query.setParameter("agentId", agentId);
            } else {
                query = em.createNamedQuery("MemoryEntity.findOldMemories", MemoryEntity.class);
            }
            
            query.setParameter("threshold", threshold);
            
            if (limit > 0) {
                query.setMaxResults(limit);
            }
            
            List<MemoryEntity> entities = query.getResultList();
            
            return entities.stream()
                .map(MemoryEntity::toMemoryRecord)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            throw new StorageException("Failed to get old memories", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    protected void addImplementationSpecificStatistics(Map<String, Object> stats) {
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            // Get total count
            TypedQuery<Long> countQuery = em.createNamedQuery("MemoryEntity.countTotal", Long.class);
            Long totalCount = countQuery.getSingleResult();
            stats.put("totalMemories", totalCount);
            
            // Get database-specific statistics
            stats.put("batchSize", batchSize);
            stats.put("secondLevelCacheEnabled", enableSecondLevelCache);
            stats.put("maxSimilaritySearchResults", maxSimilaritySearchResults);
            stats.put("similarityThreshold", similarityThreshold);
            
            // EntityManager factory statistics if available
            if (entityManagerFactory.getMetamodel() != null) {
                stats.put("managedTypes", entityManagerFactory.getMetamodel().getManagedTypes().size());
            }
            
        } catch (Exception e) {
            stats.put("statisticsError", e.getMessage());
        } finally {
            em.close();
        }
    }
    
    @Override
    protected boolean doHealthCheck() {
        EntityManager em = null;
        
        try {
            em = entityManagerFactory.createEntityManager();
            
            // Test basic connectivity
            em.createQuery("SELECT COUNT(m) FROM MemoryEntity m").getSingleResult();
            
            return true;
            
        } catch (Exception e) {
            return false;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    @Override
    protected long getCurrentMemoryCount() {
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            TypedQuery<Long> query = em.createNamedQuery("MemoryEntity.countTotal", Long.class);
            return query.getSingleResult();
            
        } catch (Exception e) {
            return 0;
        } finally {
            em.close();
        }
    }
    
    @Override
    protected void addImplementationSpecificCapacityInfo(Map<String, Object> capacity) {
        capacity.put("unlimited", true);
        capacity.put("batchSize", batchSize);
        capacity.put("currentConnections", "managed_by_jpa");
    }
    
    @Override
    public Map<String, Object> getAgentStatistics(String agentId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Agent-specific memory count
            TypedQuery<Long> countQuery = em.createNamedQuery("MemoryEntity.countByAgent", Long.class);
            countQuery.setParameter("agentId", agentId);
            Long agentMemoryCount = countQuery.getSingleResult();
            
            stats.put("agentId", agentId);
            stats.put("totalMemories", agentMemoryCount);
            
            if (agentMemoryCount > 0) {
                // Get recent activity
                TypedQuery<MemoryEntity> recentQuery = em.createQuery(
                    "SELECT m FROM MemoryEntity m WHERE m.agentId = :agentId " +
                    "ORDER BY m.lastAccessed DESC", MemoryEntity.class);
                recentQuery.setParameter("agentId", agentId);
                recentQuery.setMaxResults(1);
                
                List<MemoryEntity> recent = recentQuery.getResultList();
                if (!recent.isEmpty()) {
                    stats.put("lastActivity", recent.get(0).getLastAccessed());
                }
                
                // Get oldest memory
                TypedQuery<MemoryEntity> oldestQuery = em.createQuery(
                    "SELECT m FROM MemoryEntity m WHERE m.agentId = :agentId " +
                    "ORDER BY m.createdAt ASC", MemoryEntity.class);
                oldestQuery.setParameter("agentId", agentId);
                oldestQuery.setMaxResults(1);
                
                List<MemoryEntity> oldest = oldestQuery.getResultList();
                if (!oldest.isEmpty()) {
                    stats.put("firstMemory", oldest.get(0).getCreatedAt());
                }
            }
            
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        } finally {
            em.close();
        }
        
        return stats;
    }
    
    @Override
    public Map<String, Object> optimize(boolean vacuum) {
        Map<String, Object> results = new HashMap<>();
        results.put("startTime", Instant.now());
        
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        
        try {
            transaction.begin();
            
            if (vacuum) {
                // Perform deep optimization
                results.put("deepOptimization", true);
                
                // Clear second-level cache if enabled
                if (enableSecondLevelCache && entityManagerFactory.getCache() != null) {
                    entityManagerFactory.getCache().evictAll();
                    results.put("cacheCleared", true);
                }
            }
            
            // Flush any pending changes
            em.flush();
            em.clear();
            
            transaction.commit();
            
            results.put("success", true);
            results.put("endTime", Instant.now());
            
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            results.put("success", false);
            results.put("error", e.getMessage());
        } finally {
            em.close();
        }
        
        return results;
    }
    
    // Helper methods
    
    private void validateEntityManagerFactory() {
        if (entityManagerFactory == null) {
            throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        }
        if (!entityManagerFactory.isOpen()) {
            throw new IllegalArgumentException("EntityManagerFactory must be open");
        }
    }
    
    private String getAgentIdFromMetadata(Metadata meta) {
        if (meta != null && meta.getProperty("agentId") != null) {
            return meta.getProperty("agentId").toString();
        }
        return "default";
    }
    
    private Double getInitialRelevanceScore(Metadata meta) {
        if (meta != null && meta.getImportance() != null) {
            return meta.getImportance();
        }
        return 0.5; // Default relevance score
    }
    
    private void updateLastAccessedInTransaction(EntityManager em, MemoryEntity entity) {
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        entity.updateLastAccessed();
        transaction.commit();
    }
    

    


    
    // Getters for configuration
    
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public boolean isSecondLevelCacheEnabled() {
        return enableSecondLevelCache;
    }
    
    public int getMaxSimilaritySearchResults() {
        return maxSimilaritySearchResults;
    }
    
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public JpaSimilaritySearchStrategy getSimilaritySearchStrategy() {
        return similaritySearchStrategy;
    }
}