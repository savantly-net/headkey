package ai.headkey.persistence.strategies.jpa;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PostgreSQL-specific JPA similarity search strategy that leverages native PostgreSQL features.
 *
 * This implementation:
 * - Uses native PostgreSQL queries for optimal performance
 * - Supports pgvector extension for efficient vector similarity search
 * - Falls back to text-based search when vectors are not available
 * - Leverages PostgreSQL's full-text search capabilities
 *
 * @since 1.0
 */
public class PostgresJpaSimilaritySearchStrategy
    implements JpaSimilaritySearchStrategy {

    private final Logger log = LoggerFactory.getLogger(
        PostgresJpaSimilaritySearchStrategy.class
    );

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        );
        objectMapper.configure(
            com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
            false
        );
        objectMapper.setVisibility(
            com.fasterxml.jackson.annotation.PropertyAccessor.ALL,
            com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE
        );
        objectMapper.setVisibility(
            com.fasterxml.jackson.annotation.PropertyAccessor.FIELD,
            com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
        );
    }

    private static final String VECTOR_SIMILARITY_QUERY =
        """
        SELECT
            m.id,
            m.agent_id,
            m.content,
            m.category,
            m.relevance_score,
            m.created_at,
            m.last_accessed,
            m.metadata,
            m.embedding,
            (1 - (CAST(m.embedding AS vector) <=> CAST(:queryVector AS vector))) as similarity
        FROM memory_records m
        WHERE m.embedding IS NOT NULL
        AND (:agentId IS NULL OR m.agent_id = :agentId)
        AND (1 - (CAST(m.embedding AS vector) <=> CAST(:queryVector AS vector))) >= :threshold
        ORDER BY similarity DESC
        LIMIT :limit
        """;

    private static final String TEXT_SIMILARITY_QUERY =
        """
        SELECT
            m.id,
            m.agent_id,
            m.content,
            m.category,
            m.relevance_score,
            m.created_at,
            m.last_accessed,
            m.metadata,
            m.embedding,
            similarity(m.content, :queryContent) as text_similarity
        FROM memory_records m
        WHERE (:agentId IS NULL OR m.agent_id = :agentId)
        AND similarity(m.content, :queryContent) > 0.1
        ORDER BY text_similarity DESC
        LIMIT :limit
        """;

    private static final String FALLBACK_TEXT_QUERY =
        """
        SELECT
            m.id,
            m.agent_id,
            m.content,
            m.category,
            m.relevance_score,
            m.created_at,
            m.last_accessed,
            m.metadata,
            m.embedding
        FROM memory_records m
        WHERE LOWER(m.content) LIKE LOWER(:query)
        AND (:agentId IS NULL OR m.agent_id = :agentId)
        ORDER BY m.last_accessed DESC
        LIMIT :limit
        """;

    private static final String UPDATE_LAST_ACCESSED_QUERY =
        """
        UPDATE memory_records
        SET last_accessed = CURRENT_TIMESTAMP
        WHERE id = ANY(:ids)
        """;

    private boolean pgvectorAvailable = false;
    private boolean trigramAvailable = false;

    @Override
    public List<MemoryRecord> searchSimilar(
        EntityManager entityManager,
        String queryContent,
        double[] queryVector,
        String agentId,
        int limit,
        int maxSimilaritySearchResults,
        double similarityThreshold
    ) throws Exception {
        if (queryVector != null && pgvectorAvailable) {
            log.debug(
                "Performing vector similarity search with query vector: {}",
                queryVector
            );
            return performVectorSearch(
                entityManager,
                queryContent,
                queryVector,
                agentId,
                limit,
                similarityThreshold
            );
        } else if (trigramAvailable) {
            log.debug(
                "Performing trigram text similarity search for query: {}",
                queryContent
            );
            return performTrigramSearch(
                entityManager,
                queryContent,
                agentId,
                limit
            );
        } else {
            log.debug(
                "Performing fallback text search for query: {}",
                queryContent
            );
            return performFallbackTextSearch(
                entityManager,
                queryContent,
                agentId,
                limit
            );
        }
    }

    private List<MemoryRecord> performVectorSearch(
        EntityManager entityManager,
        String queryContent,
        double[] queryVector,
        String agentId,
        int limit,
        double similarityThreshold
    ) throws Exception {
        Query query = entityManager.createNativeQuery(VECTOR_SIMILARITY_QUERY);
        query.setParameter("queryVector", vectorToPostgresArray(queryVector));
        query.setParameter("agentId", agentId);
        query.setParameter("threshold", similarityThreshold);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<MemoryRecord> memoryRecords = new ArrayList<>();
        List<String> memoryIds = new ArrayList<>();

        for (Object[] row : results) {
            MemoryRecord record = mapResultToMemoryRecord(row);
            memoryRecords.add(record);
            memoryIds.add(record.getId());
        }

        // Update last accessed times
        if (!memoryIds.isEmpty()) {
            updateLastAccessedTimes(entityManager, memoryIds);
        }

        return memoryRecords;
    }

    private List<MemoryRecord> performTrigramSearch(
        EntityManager entityManager,
        String queryContent,
        String agentId,
        int limit
    ) throws Exception {
        Query query = entityManager.createNativeQuery(TEXT_SIMILARITY_QUERY);
        query.setParameter("queryContent", queryContent);
        query.setParameter("agentId", agentId);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<MemoryRecord> memoryRecords = new ArrayList<>();
        List<String> memoryIds = new ArrayList<>();

        for (Object[] row : results) {
            MemoryRecord record = mapResultToMemoryRecord(row);
            memoryRecords.add(record);
            memoryIds.add(record.getId());
        }

        // Update last accessed times
        if (!memoryIds.isEmpty()) {
            updateLastAccessedTimes(entityManager, memoryIds);
        }

        return memoryRecords;
    }

    private List<MemoryRecord> performFallbackTextSearch(
        EntityManager entityManager,
        String queryContent,
        String agentId,
        int limit
    ) throws Exception {
        Query query = entityManager.createNativeQuery(FALLBACK_TEXT_QUERY);
        query.setParameter("query", "%" + queryContent + "%");
        query.setParameter("agentId", agentId);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<MemoryRecord> memoryRecords = new ArrayList<>();
        List<String> memoryIds = new ArrayList<>();

        for (Object[] row : results) {
            MemoryRecord record = mapResultToMemoryRecord(row);
            memoryRecords.add(record);
            memoryIds.add(record.getId());
        }

        // Update last accessed times
        if (!memoryIds.isEmpty()) {
            updateLastAccessedTimes(entityManager, memoryIds);
        }

        return memoryRecords;
    }

    private void updateLastAccessedTimes(
        EntityManager entityManager,
        List<String> memoryIds
    ) throws Exception {
        EntityTransaction transaction = entityManager.getTransaction();
        boolean wasActive = transaction.isActive();

        if (!wasActive) {
            transaction.begin();
        }

        try {
            Query updateQuery = entityManager.createNativeQuery(
                UPDATE_LAST_ACCESSED_QUERY
            );
            updateQuery.setParameter("ids", memoryIds.toArray(new String[0]));
            updateQuery.executeUpdate();

            if (!wasActive) {
                transaction.commit();
            }
        } catch (Exception e) {
            if (!wasActive && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    private MemoryRecord mapResultToMemoryRecord(Object[] row) {
        // Assuming the query returns columns in this order:
        // id, agent_id, content, category, relevance_score, created_at, last_accessed, metadata, embedding

        MemoryRecord record = new MemoryRecord(
            row[0] != null ? row[0].toString() : null, // id
            row[1] != null ? row[1].toString() : null, // agentId
            row[2] != null ? row[2].toString() : null // content
        );

        // Set timestamps - handle both Instant and Timestamp types
        if (row[5] != null) {
            if (row[5] instanceof java.time.Instant) {
                record.setCreatedAt((java.time.Instant) row[5]);
            } else if (row[5] instanceof java.sql.Timestamp) {
                record.setCreatedAt(((java.sql.Timestamp) row[5]).toInstant());
            }
        } else {
            record.setCreatedAt(null);
        }
        if (row[6] != null) {
            if (row[6] instanceof java.time.Instant) {
                record.setLastAccessed((java.time.Instant) row[6]);
            } else if (row[6] instanceof java.sql.Timestamp) {
                record.setLastAccessed(
                    ((java.sql.Timestamp) row[6]).toInstant()
                );
            }
        } else {
            record.setLastAccessed(null);
        }

        // Set relevance score - handle different numeric types
        if (row[4] != null) {
            if (row[4] instanceof BigDecimal) {
                record.setRelevanceScore(((BigDecimal) row[4]).doubleValue());
            } else if (row[4] instanceof Number) {
                record.setRelevanceScore(((Number) row[4]).doubleValue());
            }
        }

        // Handle category JSON parsing
        if (row[3] != null) {
            try {
                String categoryJson = row[3].toString();
                if (!categoryJson.trim().isEmpty()) {
                    CategoryLabel category = objectMapper.readValue(
                        categoryJson,
                        CategoryLabel.class
                    );
                    record.setCategory(category);
                }
            } catch (Exception e) {
                log.warn("Failed to parse category JSON: {}", row[3], e);
            }
        }

        // Handle metadata JSON parsing
        if (row[7] != null) {
            try {
                String metadataJson = row[7].toString();
                if (!metadataJson.trim().isEmpty()) {
                    Metadata metadata = objectMapper.readValue(
                        metadataJson,
                        Metadata.class
                    );
                    record.setMetadata(metadata);
                }
            } catch (Exception e) {
                log.warn("Failed to parse metadata JSON: {}", row[7], e);
            }
        }

        return record;
    }

    private String vectorToPostgresArray(double[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean supportsVectorSearch() {
        return pgvectorAvailable;
    }

    @Override
    public String getStrategyName() {
        return "PostgreSQL JPA Similarity Search";
    }

    @Override
    public void initialize(EntityManager entityManager) throws Exception {
        log.info("Initializing PostgreSQL JPA Similarity Search Strategy");
        // Check for pgvector extension
        try {
            Query pgvectorCheck = entityManager.createNativeQuery(
                "SELECT 1 FROM pg_extension WHERE extname = 'vector'"
            );
            pgvectorCheck.getSingleResult();
            pgvectorAvailable = true;
            log.info("pgvector extension is available");
        } catch (Exception e) {
            pgvectorAvailable = false;
        }

        // Check for pg_trgm extension
        try {
            Query trigramCheck = entityManager.createNativeQuery(
                "SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm'"
            );
            trigramCheck.getSingleResult();
            trigramAvailable = true;
            log.info("pg_trgm extension is available");
        } catch (Exception e) {
            trigramAvailable = false;
        }
        log.info(
            "PostgreSQL JPA Similarity Search Strategy initialized: " +
            "pgvectorAvailable={}, trigramAvailable={}",
            pgvectorAvailable,
            trigramAvailable
        );
    }

    @Override
    public boolean validateSchema(EntityManager entityManager)
        throws Exception {
        try {
            // Check if memory_records table exists
            Query tableCheck = entityManager.createNativeQuery(
                "SELECT 1 FROM information_schema.tables WHERE table_name = 'memory_records'"
            );
            tableCheck.getSingleResult();

            // Check if embedding column exists if pgvector is available
            if (pgvectorAvailable) {
                Query embeddingCheck = entityManager.createNativeQuery(
                    "SELECT 1 FROM information_schema.columns " +
                    "WHERE table_name = 'memory_records' AND column_name = 'embedding'"
                );
                embeddingCheck.getSingleResult();
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
