package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.exceptions.MemoryNotFoundException;
import ai.headkey.memory.exceptions.StorageException;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.memory.strategies.SimilaritySearchStrategy;
import ai.headkey.memory.strategies.HsqldbSimilaritySearchStrategy;
import ai.headkey.memory.strategies.PostgresSimilaritySearchStrategy;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * JDBC-based implementation of the Memory Encoding System (MES).
 * 
 * This implementation provides persistent storage for memory records using
 * SQL databases with support for vector embeddings and similarity search.
 * It supports both HSQLDB (for testing) and PostgreSQL (for production)
 * with pluggable similarity search strategies.
 * 
 * Key features:
 * - Vector embeddings storage and similarity search
 * - Pluggable similarity search strategies
 * - Transaction support for data consistency
 * - Optimized indexing for performance
 * - Support for both HSQLDB and PostgreSQL
 * 
 * @since 1.0
 */
public class JdbcMemoryEncodingSystem implements MemoryEncodingSystem {
    
    private final DataSource dataSource;
    private final SimilaritySearchStrategy similaritySearchStrategy;
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong totalSearches = new AtomicLong(0);
    private final AtomicLong totalUpdates = new AtomicLong(0);
    private final AtomicLong totalDeletes = new AtomicLong(0);
    private final Instant startTime;
    
    // Embedding generation function (to be injected)
    private VectorEmbeddingGenerator embeddingGenerator;
    
    /**
     * Functional interface for generating vector embeddings from text content.
     */
    @FunctionalInterface
    public interface VectorEmbeddingGenerator {
        double[] generateEmbedding(String content) throws Exception;
    }
    
    /**
     * Creates a new JDBC-based memory encoding system.
     * 
     * @param dataSource The data source for database connections
     * @param embeddingGenerator Function to generate vector embeddings from text
     */
    public JdbcMemoryEncodingSystem(DataSource dataSource, VectorEmbeddingGenerator embeddingGenerator) {
        this.dataSource = dataSource;
        this.embeddingGenerator = embeddingGenerator;
        this.startTime = Instant.now();
        
        // Auto-detect database type and select appropriate strategy
        this.similaritySearchStrategy = detectAndCreateSimilarityStrategy();
        
        // Initialize the system
        initialize();
    }
    
    /**
     * Creates a new JDBC-based memory encoding system with custom similarity strategy.
     * 
     * @param dataSource The data source for database connections
     * @param embeddingGenerator Function to generate vector embeddings from text
     * @param similaritySearchStrategy Custom similarity search strategy
     */
    public JdbcMemoryEncodingSystem(DataSource dataSource, VectorEmbeddingGenerator embeddingGenerator,
                                   SimilaritySearchStrategy similaritySearchStrategy) {
        this.dataSource = dataSource;
        this.embeddingGenerator = embeddingGenerator;
        this.similaritySearchStrategy = similaritySearchStrategy;
        this.startTime = Instant.now();
        
        // Initialize the system
        initialize();
    }
    
    @Override
    public MemoryRecord encodeAndStore(String content, CategoryLabel category, Metadata meta) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        if (meta == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        String agentId = (String) meta.getProperty("agentId");
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID in metadata cannot be null or empty");
        }
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Generate unique ID
                String id = UUID.randomUUID().toString();
                
                // Generate vector embedding
                double[] vectorEmbedding = null;
                if (embeddingGenerator != null) {
                    try {
                        vectorEmbedding = embeddingGenerator.generateEmbedding(content);
                    } catch (Exception e) {
                        // Log warning but continue without vector embedding
                        System.err.println("Warning: Failed to generate vector embedding: " + e.getMessage());
                    }
                }
                
                // Create memory record
                MemoryRecord record = new MemoryRecord(id, agentId, content, category, meta, Instant.now());
                record.setRelevanceScore(1.0); // Initial relevance score
                record.setVersion(1L);
                
                // Insert into database
                String insertSql = """
                    INSERT INTO memories (
                        id, agent_id, content, category_primary, category_secondary, 
                        category_tags, category_confidence, metadata_json, created_at, 
                        last_accessed, relevance_score, version, vector_embedding
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setString(1, record.getId());
                    stmt.setString(2, record.getAgentId());
                    stmt.setString(3, record.getContent());
                    stmt.setString(4, category != null ? category.getPrimary() : null);
                    stmt.setString(5, category != null ? category.getSecondary() : null);
                    stmt.setString(6, category != null && category.getTags() != null ? 
                                    String.join(",", category.getTags()) : null);
                    stmt.setObject(7, category != null ? category.getConfidence() : null);
                    stmt.setString(8, metadataToJson(meta));
                    stmt.setTimestamp(9, Timestamp.from(record.getCreatedAt()));
                    stmt.setTimestamp(10, Timestamp.from(record.getLastAccessed()));
                    stmt.setObject(11, record.getRelevanceScore());
                    stmt.setLong(12, record.getVersion());
                    
                    // Handle vector embedding based on database type
                    if (vectorEmbedding != null) {
                        if (isPostgreSQL(conn)) {
                            stmt.setString(13, vectorToPostgresArray(vectorEmbedding));
                        } else {
                            // For HSQLDB, store as comma-separated string
                            stmt.setString(13, vectorToString(vectorEmbedding));
                        }
                    } else {
                        stmt.setNull(13, Types.VARCHAR);
                    }
                    
                    stmt.executeUpdate();
                }
                
                conn.commit();
                totalOperations.incrementAndGet();
                
                return record;
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
            
        } catch (Exception e) {
            throw new StorageException("Failed to encode and store memory: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Optional<MemoryRecord> getMemory(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Memory ID cannot be null or empty");
        }
        
        try (Connection conn = dataSource.getConnection()) {
            String selectSql = """
                SELECT id, agent_id, content, category_primary, category_secondary, 
                       category_tags, category_confidence, metadata_json, created_at, 
                       last_accessed, relevance_score, version, vector_embedding
                FROM memories WHERE id = ?
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, memoryId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        MemoryRecord record = mapResultSetToMemoryRecord(rs);
                        
                        // Update last accessed time
                        updateLastAccessedTime(conn, memoryId);
                        
                        totalOperations.incrementAndGet();
                        return Optional.of(record);
                    }
                }
            }
            
        } catch (Exception e) {
            throw new StorageException("Failed to get memory: " + e.getMessage(), e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Map<String, MemoryRecord> getMemories(Set<String> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            throw new IllegalArgumentException("Memory IDs cannot be null or empty");
        }
        
        Map<String, MemoryRecord> results = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            String placeholders = memoryIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String selectSql = String.format("""
                SELECT id, agent_id, content, category_primary, category_secondary, 
                       category_tags, category_confidence, metadata_json, created_at, 
                       last_accessed, relevance_score, version, vector_embedding
                FROM memories WHERE id IN (%s)
                """, placeholders);
            
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                int paramIndex = 1;
                for (String memoryId : memoryIds) {
                    stmt.setString(paramIndex++, memoryId);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        MemoryRecord record = mapResultSetToMemoryRecord(rs);
                        results.put(record.getId(), record);
                    }
                }
            }
            
            // Update last accessed times for found memories
            if (!results.isEmpty()) {
                updateLastAccessedTimes(conn, results.keySet());
            }
            
            totalOperations.incrementAndGet();
            
        } catch (Exception e) {
            throw new StorageException("Failed to get memories: " + e.getMessage(), e);
        }
        
        return results;
    }
    
    @Override
    public MemoryRecord updateMemory(MemoryRecord memoryRecord) {
        if (memoryRecord == null || memoryRecord.getId() == null) {
            throw new IllegalArgumentException("Memory record and ID cannot be null");
        }
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Check if memory exists
                if (!memoryExists(conn, memoryRecord.getId())) {
                    // Create a runtime exception to match the interface expectation
                    MemoryNotFoundException notFound = new MemoryNotFoundException("Memory not found: " + memoryRecord.getId());
                    throw new RuntimeException(notFound.getMessage(), notFound);
                }
                
                // Generate new vector embedding if content changed
                double[] vectorEmbedding = null;
                if (embeddingGenerator != null && memoryRecord.getContent() != null) {
                    try {
                        vectorEmbedding = embeddingGenerator.generateEmbedding(memoryRecord.getContent());
                    } catch (Exception e) {
                        System.err.println("Warning: Failed to generate vector embedding: " + e.getMessage());
                    }
                }
                
                // Update the record
                String updateSql = """
                    UPDATE memories SET 
                        content = ?, category_primary = ?, category_secondary = ?, 
                        category_tags = ?, category_confidence = ?, metadata_json = ?, 
                        relevance_score = ?, version = version + 1, vector_embedding = ?,
                        last_accessed = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """;
                
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setString(1, memoryRecord.getContent());
                    
                    CategoryLabel category = memoryRecord.getCategory();
                    stmt.setString(2, category != null ? category.getPrimary() : null);
                    stmt.setString(3, category != null ? category.getSecondary() : null);
                    stmt.setString(4, category != null && category.getTags() != null ? 
                                    String.join(",", category.getTags()) : null);
                    stmt.setObject(5, category != null ? category.getConfidence() : null);
                    stmt.setString(6, metadataToJson(memoryRecord.getMetadata()));
                    stmt.setObject(7, memoryRecord.getRelevanceScore());
                    
                    // Handle vector embedding
                    if (vectorEmbedding != null) {
                        if (isPostgreSQL(conn)) {
                            stmt.setString(8, vectorToPostgresArray(vectorEmbedding));
                        } else {
                            stmt.setString(8, vectorToString(vectorEmbedding));
                        }
                    } else {
                        stmt.setNull(8, Types.VARCHAR);
                    }
                    
                    stmt.setString(9, memoryRecord.getId());
                    
                    int rowsUpdated = stmt.executeUpdate();
                    if (rowsUpdated == 0) {
                        throw new MemoryNotFoundException("Memory not found: " + memoryRecord.getId());
                    }
                }
                
                conn.commit();
                totalOperations.incrementAndGet();
                totalUpdates.incrementAndGet();
                
                // Return updated record
                memoryRecord.setVersion(memoryRecord.getVersion() + 1);
                memoryRecord.setLastAccessed(Instant.now());
                
                return memoryRecord;
                
            } catch (MemoryNotFoundException e) {
                conn.rollback();
                throw new StorageException("Memory not found: " + e.getMessage(), e);
            } catch (Exception e) {
                conn.rollback();
                throw new StorageException("Failed to update memory: " + e.getMessage(), e);
            }
            
        } catch (SQLException e) {
            throw new StorageException("Database error during memory update: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean removeMemory(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Memory ID cannot be null or empty");
        }
        
        try (Connection conn = dataSource.getConnection()) {
            String deleteSql = "DELETE FROM memories WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setString(1, memoryId);
                
                int rowsDeleted = stmt.executeUpdate();
                
                if (rowsDeleted > 0) {
                    totalOperations.incrementAndGet();
                    totalDeletes.incrementAndGet();
                    return true;
                }
                
                return false;
            }
            
        } catch (Exception e) {
            throw new StorageException("Failed to remove memory: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Set<String> removeMemories(Set<String> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            throw new IllegalArgumentException("Memory IDs cannot be null or empty");
        }
        
        Set<String> removedIds = new HashSet<>();
        
        try (Connection conn = dataSource.getConnection()) {
            String placeholders = memoryIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String deleteSql = String.format("DELETE FROM memories WHERE id IN (%s)", placeholders);
            
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                int paramIndex = 1;
                for (String memoryId : memoryIds) {
                    stmt.setString(paramIndex++, memoryId);
                }
                
                int rowsDeleted = stmt.executeUpdate();
                
                if (rowsDeleted > 0) {
                    // For simplicity, assume all requested IDs were deleted
                    // In a real implementation, you might want to check which ones were actually deleted
                    removedIds.addAll(memoryIds);
                    totalOperations.incrementAndGet();
                    totalDeletes.addAndGet(rowsDeleted);
                }
            }
            
        } catch (Exception e) {
            throw new StorageException("Failed to remove memories: " + e.getMessage(), e);
        }
        
        return removedIds;
    }
    
    @Override
    public List<MemoryRecord> searchSimilar(String queryContent, int limit) {
        if (queryContent == null || queryContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Query content cannot be null or empty");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }
        
        try (Connection conn = dataSource.getConnection()) {
            // Generate query vector if embedding generator is available
            double[] queryVector = null;
            if (embeddingGenerator != null) {
                try {
                    queryVector = embeddingGenerator.generateEmbedding(queryContent);
                } catch (Exception e) {
                    System.err.println("Warning: Failed to generate query vector: " + e.getMessage());
                }
            }
            
            List<MemoryRecord> results = similaritySearchStrategy.searchSimilar(
                conn, queryContent, queryVector, null, limit);
            
            // Update last accessed times for found memories
            if (!results.isEmpty()) {
                Set<String> memoryIds = results.stream()
                    .map(MemoryRecord::getId)
                    .collect(Collectors.toSet());
                updateLastAccessedTimes(conn, memoryIds);
            }
            
            totalOperations.incrementAndGet();
            totalSearches.incrementAndGet();
            
            return results;
            
        } catch (Exception e) {
            throw new StorageException("Failed to search similar memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MemoryRecord> getMemoriesForAgent(String agentId, int limit) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        
        try (Connection conn = dataSource.getConnection()) {
            String selectSql = """
                SELECT id, agent_id, content, category_primary, category_secondary, 
                       category_tags, category_confidence, metadata_json, created_at, 
                       last_accessed, relevance_score, version, vector_embedding
                FROM memories 
                WHERE agent_id = ? 
                ORDER BY created_at DESC
                """ + (limit > 0 ? " LIMIT " + limit : "");
            
            List<MemoryRecord> results = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, agentId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapResultSetToMemoryRecord(rs));
                    }
                }
            }
            
            totalOperations.incrementAndGet();
            return results;
            
        } catch (Exception e) {
            throw new StorageException("Failed to get memories for agent: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MemoryRecord> getMemoriesInCategory(String category, String agentId, int limit) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }
        
        try (Connection conn = dataSource.getConnection()) {
            StringBuilder sqlBuilder = new StringBuilder("""
                SELECT id, agent_id, content, category_primary, category_secondary, 
                       category_tags, category_confidence, metadata_json, created_at, 
                       last_accessed, relevance_score, version, vector_embedding
                FROM memories 
                WHERE (category_primary = ? OR category_secondary = ?)
                """);
            
            if (agentId != null) {
                sqlBuilder.append(" AND agent_id = ?");
            }
            
            sqlBuilder.append(" ORDER BY created_at DESC");
            
            if (limit > 0) {
                sqlBuilder.append(" LIMIT ").append(limit);
            }
            
            List<MemoryRecord> results = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
                stmt.setString(1, category);
                stmt.setString(2, category);
                
                if (agentId != null) {
                    stmt.setString(3, agentId);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapResultSetToMemoryRecord(rs));
                    }
                }
            }
            
            totalOperations.incrementAndGet();
            return results;
            
        } catch (Exception e) {
            throw new StorageException("Failed to get memories in category: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MemoryRecord> getOldMemories(long olderThanSeconds, String agentId, int limit) {
        if (olderThanSeconds < 0) {
            throw new IllegalArgumentException("Age threshold must be non-negative");
        }
        
        try (Connection conn = dataSource.getConnection()) {
            Instant threshold = Instant.now().minusSeconds(olderThanSeconds);
            
            StringBuilder sqlBuilder = new StringBuilder("""
                SELECT id, agent_id, content, category_primary, category_secondary, 
                       category_tags, category_confidence, metadata_json, created_at, 
                       last_accessed, relevance_score, version, vector_embedding
                FROM memories 
                WHERE created_at < ?
                """);
            
            if (agentId != null) {
                sqlBuilder.append(" AND agent_id = ?");
            }
            
            sqlBuilder.append(" ORDER BY created_at ASC");
            
            if (limit > 0) {
                sqlBuilder.append(" LIMIT ").append(limit);
            }
            
            List<MemoryRecord> results = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
                stmt.setTimestamp(1, Timestamp.from(threshold));
                
                if (agentId != null) {
                    stmt.setString(2, agentId);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapResultSetToMemoryRecord(rs));
                    }
                }
            }
            
            totalOperations.incrementAndGet();
            return results;
            
        } catch (Exception e) {
            throw new StorageException("Failed to get old memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> getStorageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // Total memories count
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM memories")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("totalMemories", rs.getLong(1));
                    }
                }
            }
            
            // Agent distribution
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT agent_id, COUNT(*) FROM memories GROUP BY agent_id")) {
                
                Map<String, Long> agentDistribution = new HashMap<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        agentDistribution.put(rs.getString(1), rs.getLong(2));
                    }
                }
                stats.put("agentDistribution", agentDistribution);
            }
            
            // Category distribution
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT category_primary, COUNT(*) FROM memories WHERE category_primary IS NOT NULL GROUP BY category_primary")) {
                
                Map<String, Long> categoryDistribution = new HashMap<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        categoryDistribution.put(rs.getString(1), rs.getLong(2));
                    }
                }
                stats.put("categoryDistribution", categoryDistribution);
            }
            
            // Performance stats
            stats.put("totalOperations", totalOperations.get());
            stats.put("totalSearches", totalSearches.get());
            stats.put("totalUpdates", totalUpdates.get());
            stats.put("totalDeletes", totalDeletes.get());
            stats.put("uptime", Instant.now().getEpochSecond() - startTime.getEpochSecond());
            stats.put("strategyName", similaritySearchStrategy.getStrategyName());
            stats.put("supportsVectorSearch", similaritySearchStrategy.supportsVectorSearch());
            
        } catch (Exception e) {
            throw new StorageException("Failed to get storage statistics: " + e.getMessage(), e);
        }
        
        return stats;
    }
    
    @Override
    public Map<String, Object> getAgentStatistics(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // Total memories for agent
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM memories WHERE agent_id = ?")) {
                stmt.setString(1, agentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("totalMemories", rs.getLong(1));
                    }
                }
            }
            
            // Category breakdown for agent
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT category_primary, COUNT(*) FROM memories WHERE agent_id = ? AND category_primary IS NOT NULL GROUP BY category_primary")) {
                stmt.setString(1, agentId);
                
                Map<String, Long> categoryBreakdown = new HashMap<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        categoryBreakdown.put(rs.getString(1), rs.getLong(2));
                    }
                }
                stats.put("categoryBreakdown", categoryBreakdown);
            }
            
            // Average relevance score
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT AVG(relevance_score) FROM memories WHERE agent_id = ? AND relevance_score IS NOT NULL")) {
                stmt.setString(1, agentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("averageRelevanceScore", rs.getDouble(1));
                    }
                }
            }
            
            // Most recent memory
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT MAX(created_at) FROM memories WHERE agent_id = ?")) {
                stmt.setString(1, agentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Timestamp timestamp = rs.getTimestamp(1);
                        if (timestamp != null) {
                            stats.put("mostRecentMemory", timestamp.toInstant());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            throw new StorageException("Failed to get agent statistics: " + e.getMessage(), e);
        }
        
        return stats;
    }
    
    @Override
    public Map<String, Object> optimize(boolean vacuum) {
        Map<String, Object> results = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            long startTime = System.currentTimeMillis();
            
            if (isPostgreSQL(conn)) {
                // PostgreSQL optimization
                if (vacuum) {
                    try (PreparedStatement stmt = conn.prepareStatement("VACUUM ANALYZE memories")) {
                        stmt.executeUpdate();
                        results.put("vacuum", "completed");
                    }
                }
                
                // Update table statistics
                try (PreparedStatement stmt = conn.prepareStatement("ANALYZE memories")) {
                    stmt.executeUpdate();
                    results.put("analyze", "completed");
                }
                
                // Reindex vector indexes if they exist
                try (PreparedStatement stmt = conn.prepareStatement("REINDEX INDEX CONCURRENTLY idx_memories_vector_cosine")) {
                    stmt.executeUpdate();
                    results.put("vectorIndexReindex", "completed");
                } catch (Exception e) {
                    results.put("vectorIndexReindex", "skipped: " + e.getMessage());
                }
                
            } else {
                // HSQLDB optimization
                try (PreparedStatement stmt = conn.prepareStatement("CHECKPOINT")) {
                    stmt.executeUpdate();
                    results.put("checkpoint", "completed");
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            results.put("optimizationDurationMs", duration);
            results.put("timestamp", Instant.now());
            
        } catch (Exception e) {
            throw new StorageException("Failed to optimize storage: " + e.getMessage(), e);
        }
        
        return results;
    }
    
    @Override
    public boolean isHealthy() {
        try (Connection conn = dataSource.getConnection()) {
            // Basic connectivity check
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM (VALUES(0))")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                }
            }
            
            // Check if memories table exists
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM memories LIMIT 1")) {
                stmt.executeQuery();
            }
            
            // Validate similarity search strategy
            return similaritySearchStrategy.validateSchema(conn);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getCapacityInfo() {
        Map<String, Object> info = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // Get current memory count
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM memories")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        info.put("currentMemoryCount", rs.getLong(1));
                    }
                }
            }
            
            if (isPostgreSQL(conn)) {
                // PostgreSQL-specific capacity information
                try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT pg_database_size(current_database())")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            info.put("databaseSizeBytes", rs.getLong(1));
                        }
                    }
                }
                
                try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT pg_size_pretty(pg_database_size(current_database()))")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            info.put("databaseSizeHuman", rs.getString(1));
                        }
                    }
                }
            } else {
                // HSQLDB doesn't have built-in size functions
                info.put("databaseSizeBytes", "Not available for HSQLDB");
                info.put("databaseSizeHuman", "Not available for HSQLDB");
            }
            
            // Estimate growth based on recent activity
            long recentOperations = totalOperations.get();
            long uptimeSeconds = Instant.now().getEpochSecond() - startTime.getEpochSecond();
            double operationsPerSecond = uptimeSeconds > 0 ? (double) recentOperations / uptimeSeconds : 0;
            
            info.put("operationsPerSecond", operationsPerSecond);
            info.put("estimatedDailyGrowth", (int) (operationsPerSecond * 24 * 60 * 60));
            info.put("maxCapacity", "Unlimited (database dependent)");
            info.put("utilizationPercent", "Unknown");
            
        } catch (Exception e) {
            throw new StorageException("Failed to get capacity info: " + e.getMessage(), e);
        }
        
        return info;
    }
    
    // Private utility methods
    
    /**
     * Initializes the storage system.
     */
    private void initialize() {
        try (Connection conn = dataSource.getConnection()) {
            createTablesIfNotExist(conn);
            similaritySearchStrategy.initialize(conn);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JDBC Memory Encoding System: " + e.getMessage(), e);
        }
    }
    
    /**
     * Detects the database type and creates appropriate similarity search strategy.
     */
    private SimilaritySearchStrategy detectAndCreateSimilarityStrategy() {
        try (Connection conn = dataSource.getConnection()) {
            if (isPostgreSQL(conn)) {
                return new PostgresSimilaritySearchStrategy();
            } else {
                return new HsqldbSimilaritySearchStrategy();
            }
        } catch (Exception e) {
            // Default to HSQLDB strategy if detection fails
            return new HsqldbSimilaritySearchStrategy();
        }
    }
    
    /**
     * Checks if the database is PostgreSQL.
     */
    private boolean isPostgreSQL(Connection conn) throws SQLException {
        String databaseProductName = conn.getMetaData().getDatabaseProductName().toLowerCase();
        return databaseProductName.contains("postgresql");
    }
    
    /**
     * Creates the memories table if it doesn't exist.
     */
    private void createTablesIfNotExist(Connection conn) throws SQLException {
        if (isPostgreSQL(conn)) {
            createPostgreSQLTables(conn);
        } else {
            createHsqldbTables(conn);
        }
    }
    
    /**
     * Creates PostgreSQL-specific tables with vector support.
     */
    private void createPostgreSQLTables(Connection conn) throws SQLException {
        // Ensure pgvector extension exists
        try (PreparedStatement stmt = conn.prepareStatement("CREATE EXTENSION IF NOT EXISTS vector")) {
            stmt.executeUpdate();
        }
        
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS memories (
                id VARCHAR(255) PRIMARY KEY,
                agent_id VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                category_primary VARCHAR(255),
                category_secondary VARCHAR(255),
                category_tags TEXT,
                category_confidence DOUBLE PRECISION,
                metadata_json TEXT,
                created_at TIMESTAMP NOT NULL,
                last_accessed TIMESTAMP NOT NULL,
                relevance_score DOUBLE PRECISION,
                version BIGINT NOT NULL,
                vector_embedding VECTOR(1536)
            )
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(createTableSql)) {
            stmt.executeUpdate();
        }
        
        // Create basic indexes
        createBasicIndexes(conn);
    }
    
    /**
     * Creates HSQLDB-specific tables.
     */
    private void createHsqldbTables(Connection conn) throws SQLException {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS memories (
                id VARCHAR(255) PRIMARY KEY,
                agent_id VARCHAR(255) NOT NULL,
                content LONGVARCHAR NOT NULL,
                category_primary VARCHAR(255),
                category_secondary VARCHAR(255),
                category_tags LONGVARCHAR,
                category_confidence DOUBLE,
                metadata_json LONGVARCHAR,
                created_at TIMESTAMP NOT NULL,
                last_accessed TIMESTAMP NOT NULL,
                relevance_score DOUBLE,
                version BIGINT NOT NULL,
                vector_embedding LONGVARCHAR
            )
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(createTableSql)) {
            stmt.executeUpdate();
        }
        
        // Create basic indexes
        createBasicIndexes(conn);
    }
    
    /**
     * Creates basic indexes for the memories table.
     */
    private void createBasicIndexes(Connection conn) throws SQLException {
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_memories_agent_id ON memories (agent_id)",
            "CREATE INDEX IF NOT EXISTS idx_memories_created_at ON memories (created_at)",
            "CREATE INDEX IF NOT EXISTS idx_memories_category_primary ON memories (category_primary)",
            "CREATE INDEX IF NOT EXISTS idx_memories_relevance_score ON memories (relevance_score)"
        };
        
        for (String indexSql : indexes) {
            try (PreparedStatement stmt = conn.prepareStatement(indexSql)) {
                stmt.executeUpdate();
            }
        }
    }
    
    /**
     * Checks if a memory exists in the database.
     */
    private boolean memoryExists(Connection conn, String memoryId) throws SQLException {
        String sql = "SELECT 1 FROM memories WHERE id = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, memoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    /**
     * Updates the last accessed time for a single memory.
     */
    private void updateLastAccessedTime(Connection conn, String memoryId) throws SQLException {
        String sql = "UPDATE memories SET last_accessed = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, memoryId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Updates the last accessed times for multiple memories.
     */
    private void updateLastAccessedTimes(Connection conn, Set<String> memoryIds) throws SQLException {
        if (memoryIds.isEmpty()) {
            return;
        }
        
        String placeholders = memoryIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = String.format("UPDATE memories SET last_accessed = CURRENT_TIMESTAMP WHERE id IN (%s)", placeholders);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            for (String memoryId : memoryIds) {
                stmt.setString(paramIndex++, memoryId);
            }
            stmt.executeUpdate();
        }
    }
    
    /**
     * Maps a ResultSet row to a MemoryRecord object.
     */
    private MemoryRecord mapResultSetToMemoryRecord(ResultSet rs) throws SQLException {
        MemoryRecord record = new MemoryRecord();
        
        record.setId(rs.getString("id"));
        record.setAgentId(rs.getString("agent_id"));
        record.setContent(rs.getString("content"));
        
        // Map category information
        String primaryCategory = rs.getString("category_primary");
        String secondaryCategory = rs.getString("category_secondary");
        String tags = rs.getString("category_tags");
        Double confidence = rs.getObject("category_confidence") != null ? 
                           rs.getDouble("category_confidence") : null;
        
        if (primaryCategory != null) {
            CategoryLabel category = new CategoryLabel();
            category.setPrimary(primaryCategory);
            category.setSecondary(secondaryCategory);
            if (tags != null && !tags.trim().isEmpty()) {
                category.setTags(Set.of(tags.split(",")));
            }
            category.setConfidence(confidence);
            record.setCategory(category);
        }
        
        // Map metadata (basic implementation - in real scenario would parse JSON)
        String metadataJson = rs.getString("metadata_json");
        if (metadataJson != null && !metadataJson.trim().isEmpty()) {
            Metadata metadata = new Metadata();
            record.setMetadata(metadata);
        }
        
        // Map timestamps
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            record.setCreatedAt(createdAt.toInstant());
        }
        
        Timestamp lastAccessed = rs.getTimestamp("last_accessed");
        if (lastAccessed != null) {
            record.setLastAccessed(lastAccessed.toInstant());
        }
        
        // Map other fields
        Double relevanceScore = rs.getObject("relevance_score") != null ? 
                               rs.getDouble("relevance_score") : null;
        record.setRelevanceScore(relevanceScore);
        
        Long version = rs.getObject("version") != null ? 
                      rs.getLong("version") : null;
        record.setVersion(version);
        
        return record;
    }
    
    /**
     * Converts metadata to JSON string (simplified implementation).
     */
    private String metadataToJson(Metadata metadata) {
        if (metadata == null) {
            return null;
        }
        
        // Simplified JSON serialization - in real implementation use Jackson or similar
        StringBuilder json = new StringBuilder("{");
        
        String agentIdValue = (String) metadata.getProperty("agentId");
        if (agentIdValue != null) {
            json.append("\"agentId\":\"").append(agentIdValue).append("\",");
        }
        
        if (metadata.getSource() != null) {
            json.append("\"source\":\"").append(metadata.getSource()).append("\",");
        }
        
        if (metadata.getImportance() != null) {
            json.append("\"importance\":").append(metadata.getImportance()).append(",");
        }
        
        if (metadata.getAccessCount() != null) {
            json.append("\"accessCount\":").append(metadata.getAccessCount()).append(",");
        }
        
        // Remove trailing comma if exists
        if (json.length() > 1 && json.charAt(json.length() - 1) == ',') {
            json.setLength(json.length() - 1);
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Converts a double array to PostgreSQL vector format.
     */
    private String vectorToPostgresArray(double[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Converts a double array to comma-separated string for HSQLDB.
     */
    private String vectorToString(double[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        
        return Arrays.stream(vector)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));
    }
}