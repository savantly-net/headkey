package ai.headkey.persistence.strategies.jpa;

import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.persistence.entities.MemoryEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Text-based JPA similarity search strategy for databases without vector support.
 * 
 * This implementation:
 * - Uses JPQL queries with LIKE operations for text matching
 * - Supports case-insensitive search
 * - Orders results by relevance score and recency
 * - Optimized for databases like H2, HSQLDB, MySQL without vector extensions
 * - Updates last accessed timestamps during search
 * 
 * @since 1.0
 */
public class TextBasedJpaSimilaritySearchStrategy implements JpaSimilaritySearchStrategy {
    
    // Use database-specific native SQL for case-insensitive search
    private static final String POSTGRES_TEXT_SEARCH_QUERY = """
        SELECT * FROM MemoryEntity m 
        WHERE m.content ILIKE ? 
        AND (? IS NULL OR m.agent_id = ?)
        ORDER BY 
            CASE WHEN m.relevance_score IS NOT NULL THEN m.relevance_score ELSE 0.0 END DESC,
            m.last_accessed DESC,
            m.created_at DESC
        """;
    
    // H2 fallback using JPQL without functions
    private static final String H2_TEXT_SEARCH_QUERY = """
        SELECT m FROM MemoryEntity m 
        WHERE LOWER(cast(m.content as string)) LIKE :query 
        AND (:agentId IS NULL OR m.agentId = :agentId)
        ORDER BY 
            CASE WHEN m.relevanceScore IS NOT NULL THEN m.relevanceScore ELSE 0.0 END DESC,
            m.lastAccessed DESC,
            m.createdAt DESC
        """;
    
    @Override
    public List<MemoryRecord> searchSimilar(EntityManager entityManager, String queryContent, 
                                          double[] queryVector, String agentId, int limit,
                                          int maxSimilaritySearchResults, double similarityThreshold) throws Exception {
        
        // Extract keywords from query content for better matching
        String[] keywords = extractKeywords(queryContent);
        
        if (keywords.length <= 1) {
            return performSimpleTextSearch(entityManager, queryContent, agentId, limit);
        } else {
            return performKeywordSearch(entityManager, keywords, agentId, limit);
        }
    }
    
    private List<MemoryRecord> performSimpleTextSearch(EntityManager entityManager, String queryContent, 
                                                     String agentId, int limit) throws Exception {
        
        // Check database type for case-insensitive search strategy
        String databaseProductName = entityManager.getEntityManagerFactory()
            .getProperties().get("hibernate.dialect").toString().toLowerCase();
        
        List<MemoryEntity> entities;
        
        if (databaseProductName.contains("postgresql")) {
            // Use PostgreSQL ILIKE for case-insensitive search
            var nativeQuery = entityManager.createNativeQuery(POSTGRES_TEXT_SEARCH_QUERY, MemoryEntity.class);
            nativeQuery.setParameter(1, "%" + queryContent + "%");
            nativeQuery.setParameter(2, agentId);
            nativeQuery.setParameter(3, agentId);
            nativeQuery.setMaxResults(limit);
            entities = nativeQuery.getResultList();
        } else {
            // Fallback to JPQL for H2 and other databases
            TypedQuery<MemoryEntity> query = entityManager.createQuery(H2_TEXT_SEARCH_QUERY, MemoryEntity.class);
            query.setParameter("query", "%" + queryContent.toLowerCase() + "%");
            query.setParameter("agentId", agentId);
            query.setMaxResults(limit);
            entities = query.getResultList();
        }
        
        return updateAccessTimesAndConvert(entityManager, entities);
    }
    
    private List<MemoryRecord> performKeywordSearch(EntityManager entityManager, String[] keywords, 
                                                  String agentId, int limit) throws Exception {
        
        // For keyword search, use simple text search with concatenated keywords
        String keywordQuery = String.join(" ", keywords);
        return performSimpleTextSearch(entityManager, keywordQuery, agentId, limit);
    }
    
    private List<MemoryRecord> updateAccessTimesAndConvert(EntityManager entityManager, List<MemoryEntity> entities) throws Exception {
        if (entities.isEmpty()) {
            return List.of();
        }
        
        EntityTransaction transaction = entityManager.getTransaction();
        boolean wasActive = transaction.isActive();
        
        if (!wasActive) {
            transaction.begin();
        }
        
        try {
            List<MemoryRecord> results = entities.stream()
                .peek(MemoryEntity::updateLastAccessed)
                .map(MemoryEntity::toMemoryRecord)
                .collect(Collectors.toList());
            
            if (!wasActive) {
                transaction.commit();
            }
            
            return results;
        } catch (Exception e) {
            if (!wasActive && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }
    
    private String[] extractKeywords(String queryContent) {
        if (queryContent == null || queryContent.trim().isEmpty()) {
            return new String[0];
        }
        
        // Simple keyword extraction: split by whitespace and filter out short words
        return Arrays.stream(queryContent.toLowerCase()
            .trim()
            .split("\\s+"))
            .filter(word -> word.length() > 2) // Ignore very short words
            .filter(word -> !isStopWord(word)) // Remove common stop words
            .distinct()
            .limit(5) // Limit to 5 keywords for performance
            .toArray(String[]::new);
    }
    
    private boolean isStopWord(String word) {
        // Simple stop word list - could be expanded
        String[] stopWords = {
            "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
            "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "will", "would", "could", "should", "may", "might",
            "this", "that", "these", "those", "a", "an", "as", "if", "it", "its",
            "from", "into", "onto", "upon", "over", "under", "above", "below"
        };
        
        for (String stopWord : stopWords) {
            if (stopWord.equals(word)) {
                return true;
            }
        }
        return false;
    }
    
    private int countKeywordMatches(String content, String[] keywords) {
        int count = 0;
        String lowerContent = content.toLowerCase();
        for (String keyword : keywords) {
            if (lowerContent.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }
    
    @Override
    public boolean supportsVectorSearch() {
        return false;
    }
    
    @Override
    public String getStrategyName() {
        return "Text-based JPA Similarity Search";
    }
    
    @Override
    public void initialize(EntityManager entityManager) throws Exception {
        // No special initialization required for text-based search
        // This strategy works with any JPA-compatible database
    }
    
    @Override
    public boolean validateSchema(EntityManager entityManager) throws Exception {
        try {
            // Simple validation by attempting to query the MemoryEntity table
            TypedQuery<Long> countQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM MemoryEntity m", Long.class);
            countQuery.setMaxResults(1);
            countQuery.getSingleResult();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}