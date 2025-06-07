package ai.headkey.memory.strategies;

import ai.headkey.memory.dto.MemoryRecord;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for similarity search strategies.
 * 
 * Tests both HSQLDB and PostgreSQL similarity search strategies
 * using in-memory databases and mock data.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimilaritySearchStrategyTest {
    
    private Connection hsqldbConnection;
    private HsqldbSimilaritySearchStrategy hsqldbStrategy;
    private PostgresSimilaritySearchStrategy postgresStrategy;
    
    @BeforeEach
    void setUp() throws SQLException {
        // Set up HSQLDB in-memory database
        hsqldbConnection = DriverManager.getConnection("jdbc:hsqldb:mem:testdb", "SA", "");
        hsqldbStrategy = new HsqldbSimilaritySearchStrategy();
        postgresStrategy = new PostgresSimilaritySearchStrategy();
        
        createTestTable(hsqldbConnection, false);
        insertTestData(hsqldbConnection);
    }
    
    @AfterEach
    void tearDown() throws SQLException {
        if (hsqldbConnection != null && !hsqldbConnection.isClosed()) {
            hsqldbConnection.close();
        }
    }
    
    @Test
    @Order(1)
    void testHsqldbStrategy_BasicProperties() {
        assertFalse(hsqldbStrategy.supportsVectorSearch());
        assertEquals("HSQLDB Text-Based Similarity Search", hsqldbStrategy.getStrategyName());
    }
    
    @Test
    @Order(2)
    void testPostgresStrategy_BasicProperties() {
        assertTrue(postgresStrategy.supportsVectorSearch());
        assertEquals("PostgreSQL Vector Similarity Search", postgresStrategy.getStrategyName());
    }
    
    @Test
    @Order(3)
    void testHsqldbStrategy_ValidateSchema() throws Exception {
        assertTrue(hsqldbStrategy.validateSchema(hsqldbConnection));
    }
    
    @Test
    @Order(4)
    void testHsqldbStrategy_Initialize() throws Exception {
        assertDoesNotThrow(() -> hsqldbStrategy.initialize(hsqldbConnection));
        
        // Verify indexes were created
        DatabaseMetaData metaData = hsqldbConnection.getMetaData();
        try (ResultSet rs = metaData.getIndexInfo(null, null, "MEMORIES", false, false)) {
            boolean foundContentIndex = false;
            boolean foundCategoryIndex = false;
            boolean foundAgentRelevanceIndex = false;
            
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if ("IDX_MEMORIES_CONTENT_LOWER".equals(indexName)) {
                    foundContentIndex = true;
                } else if ("IDX_MEMORIES_CATEGORY_LOWER".equals(indexName)) {
                    foundCategoryIndex = true;
                } else if ("IDX_MEMORIES_AGENT_RELEVANCE".equals(indexName)) {
                    foundAgentRelevanceIndex = true;
                }
            }
            
            assertTrue(foundContentIndex);
            assertTrue(foundCategoryIndex);
            assertTrue(foundAgentRelevanceIndex);
        }
    }
    
    @Test
    @Order(5)
    void testHsqldbStrategy_SearchSimilar_ExactMatch() throws Exception {
        hsqldbStrategy.initialize(hsqldbConnection);
        
        List<MemoryRecord> results = hsqldbStrategy.searchSimilar(
            hsqldbConnection, 
            "machine learning", 
            null, 
            null, 
            5
        );
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        // Should find the memory with "machine learning" in content
        boolean foundExactMatch = results.stream()
            .anyMatch(record -> record.getContent().toLowerCase().contains("machine learning"));
        assertTrue(foundExactMatch);
    }
    
    @Test
    @Order(6)
    void testHsqldbStrategy_SearchSimilar_PartialMatch() throws Exception {
        hsqldbStrategy.initialize(hsqldbConnection);
        
        List<MemoryRecord> results = hsqldbStrategy.searchSimilar(
            hsqldbConnection, 
            "artificial", 
            null, 
            null, 
            5
        );
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        // Should find memories containing "artificial"
        boolean foundMatch = results.stream()
            .anyMatch(record -> record.getContent().toLowerCase().contains("artificial"));
        assertTrue(foundMatch);
    }
    
    @Test
    @Order(7)
    void testHsqldbStrategy_SearchSimilar_CategoryMatch() throws Exception {
        hsqldbStrategy.initialize(hsqldbConnection);
        
        List<MemoryRecord> results = hsqldbStrategy.searchSimilar(
            hsqldbConnection, 
            "technology", 
            null, 
            null, 
            5
        );
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        // Should find memories in technology category
        boolean foundCategoryMatch = results.stream()
            .anyMatch(record -> "technology".equals(record.getCategory().getPrimary()));
        assertTrue(foundCategoryMatch);
    }
    
    @Test
    @Order(8)
    void testHsqldbStrategy_SearchSimilar_AgentFilter() throws Exception {
        hsqldbStrategy.initialize(hsqldbConnection);
        
        List<MemoryRecord> results = hsqldbStrategy.searchSimilar(
            hsqldbConnection, 
            "test", 
            null, 
            "agent-1", 
            10
        );
        
        assertNotNull(results);
        
        // All results should belong to agent-1
        for (MemoryRecord record : results) {
            assertEquals("agent-1", record.getAgentId());
        }
    }
    
    @Test
    @Order(9)
    void testHsqldbStrategy_SearchSimilar_LimitRespected() throws Exception {
        hsqldbStrategy.initialize(hsqldbConnection);
        
        List<MemoryRecord> results = hsqldbStrategy.searchSimilar(
            hsqldbConnection, 
            "test", 
            null, 
            null, 
            2
        );
        
        assertNotNull(results);
        assertTrue(results.size() <= 2);
    }
    
    @Test
    @Order(10)
    void testHsqldbStrategy_SearchSimilar_NoResults() throws Exception {
        hsqldbStrategy.initialize(hsqldbConnection);
        
        List<MemoryRecord> results = hsqldbStrategy.searchSimilar(
            hsqldbConnection, 
            "nonexistent content xyz", 
            null, 
            null, 
            5
        );
        
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
    
    @Test
    @Order(11)
    void testHsqldbStrategy_SearchSimilar_InvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> 
            hsqldbStrategy.searchSimilar(hsqldbConnection, null, null, null, 5));
        
        assertThrows(IllegalArgumentException.class, () -> 
            hsqldbStrategy.searchSimilar(hsqldbConnection, "", null, null, 5));
    }
    
    @Test
    @Order(12)
    void testPostgresStrategy_SearchSimilar_TextFallback() throws Exception {
        // Since we're using HSQLDB, PostgreSQL strategy should fall back to text search
        // and work similarly to HSQLDB strategy
        
        List<MemoryRecord> results = postgresStrategy.searchSimilar(
            hsqldbConnection, 
            "machine learning", 
            null, 
            null, 
            5
        );
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        // Should find the memory with "machine learning" in content
        boolean foundMatch = results.stream()
            .anyMatch(record -> record.getContent().toLowerCase().contains("machine learning"));
        assertTrue(foundMatch);
    }
    
    @Test
    @Order(13)
    void testPostgresStrategy_SearchSimilar_WithVector() throws Exception {
        // Test with mock vector data
        double[] queryVector = createMockVector(1536);
        
        List<MemoryRecord> results = postgresStrategy.searchSimilar(
            hsqldbConnection, 
            "artificial intelligence", 
            queryVector, 
            null, 
            5
        );
        
        assertNotNull(results);
        // Should still work with text fallback since HSQLDB doesn't support vectors
    }
    
    @Test
    @Order(14)
    void testStrategyComparison_SameQuery() throws Exception {
        hsqldbStrategy.initialize(hsqldbConnection);
        
        String query = "artificial intelligence";
        
        List<MemoryRecord> hsqldbResults = hsqldbStrategy.searchSimilar(
            hsqldbConnection, query, null, null, 5);
        
        List<MemoryRecord> postgresResults = postgresStrategy.searchSimilar(
            hsqldbConnection, query, null, null, 5);
        
        assertNotNull(hsqldbResults);
        assertNotNull(postgresResults);
        
        // Both should find some results for this query
        assertFalse(hsqldbResults.isEmpty());
        assertFalse(postgresResults.isEmpty());
    }
    
    @Test
    @Order(15)
    void testMemoryRecordMapping() throws Exception {
        hsqldbStrategy.initialize(hsqldbConnection);
        
        List<MemoryRecord> results = hsqldbStrategy.searchSimilar(
            hsqldbConnection, 
            "machine learning", 
            null, 
            null, 
            1
        );
        
        assertFalse(results.isEmpty());
        MemoryRecord record = results.get(0);
        
        // Verify all fields are properly mapped
        assertNotNull(record.getId());
        assertNotNull(record.getAgentId());
        assertNotNull(record.getContent());
        assertNotNull(record.getCategory());
        assertNotNull(record.getCategory().getPrimary());
        assertNotNull(record.getCreatedAt());
        assertNotNull(record.getLastAccessed());
        assertNotNull(record.getVersion());
    }
    
    // Helper methods
    
    private void createTestTable(Connection conn, boolean isPostgres) throws SQLException {
        String createTableSql;
        
        if (isPostgres) {
            createTableSql = """
                CREATE TABLE memories (
                    id VARCHAR(255) PRIMARY KEY,
                    agent_id VARCHAR(255) NOT NULL,
                    content TEXT NOT NULL,
                    category_primary VARCHAR(255),
                    category_secondary VARCHAR(255),
                    category_tags TEXT,
                    category_confidence DOUBLE PRECISION,
                    metadata_json TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    last_accessed TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    relevance_score DOUBLE PRECISION,
                    version BIGINT NOT NULL DEFAULT 1,
                    vector_embedding VECTOR(1536)
                )
                """;
        } else {
            createTableSql = """
                CREATE TABLE memories (
                    id VARCHAR(255) PRIMARY KEY,
                    agent_id VARCHAR(255) NOT NULL,
                    content LONGVARCHAR NOT NULL,
                    category_primary VARCHAR(255),
                    category_secondary VARCHAR(255),
                    category_tags LONGVARCHAR,
                    category_confidence DOUBLE,
                    metadata_json LONGVARCHAR,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    last_accessed TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    relevance_score DOUBLE,
                    version BIGINT NOT NULL DEFAULT 1,
                    vector_embedding LONGVARCHAR
                )
                """;
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(createTableSql)) {
            stmt.executeUpdate();
        }
    }
    
    private void insertTestData(Connection conn) throws SQLException {
        String[] testData = {
            "('mem-1', 'agent-1', 'Machine learning is a subset of artificial intelligence', 'technology', 'ai', 'machine-learning,ai', 0.9, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.8, 1, null)",
            "('mem-2', 'agent-1', 'Deep learning uses neural networks with multiple layers', 'technology', 'ai', 'deep-learning,neural-networks', 0.85, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.7, 1, null)",
            "('mem-3', 'agent-2', 'Natural language processing helps computers understand text', 'technology', 'nlp', 'nlp,text-processing', 0.9, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.9, 1, null)",
            "('mem-4', 'agent-2', 'Computer vision enables machines to interpret visual information', 'technology', 'cv', 'computer-vision,image-processing', 0.8, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.6, 1, null)",
            "('mem-5', 'agent-3', 'Data science involves extracting insights from large datasets', 'science', 'data', 'data-science,analytics', 0.75, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0.5, 1, null)"
        };
        
        String insertSql = """
            INSERT INTO memories (
                id, agent_id, content, category_primary, category_secondary, 
                category_tags, category_confidence, metadata_json, created_at, 
                last_accessed, relevance_score, version, vector_embedding
            ) VALUES 
            """;
        
        for (int i = 0; i < testData.length; i++) {
            String sql = insertSql + testData[i];
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        }
    }
    
    private double[] createMockVector(int dimension) {
        double[] vector = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            vector[i] = Math.random();
        }
        return vector;
    }
}