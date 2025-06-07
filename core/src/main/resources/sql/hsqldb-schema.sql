-- HSQLDB schema for HeadKey Memory Encoding System
-- Used for testing and development environments

-- Main memories table without native vector support
CREATE TABLE IF NOT EXISTS memories (
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
    vector_embedding LONGVARCHAR, -- Stored as comma-separated string
    
    -- Constraints
    CONSTRAINT memories_relevance_score_check CHECK (relevance_score >= 0.0 AND relevance_score <= 1.0),
    CONSTRAINT memories_version_check CHECK (version > 0),
    CONSTRAINT memories_category_confidence_check CHECK (category_confidence >= 0.0 AND category_confidence <= 1.0)
);

-- Basic indexes for common queries
CREATE INDEX IF NOT EXISTS idx_memories_agent_id ON memories (agent_id);
CREATE INDEX IF NOT EXISTS idx_memories_created_at ON memories (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_memories_last_accessed ON memories (last_accessed DESC);
CREATE INDEX IF NOT EXISTS idx_memories_category_primary ON memories (category_primary);
CREATE INDEX IF NOT EXISTS idx_memories_relevance_score ON memories (relevance_score DESC);

-- Text-based search indexes (HSQLDB doesn't have full-text search like PostgreSQL)
CREATE INDEX IF NOT EXISTS idx_memories_content_lower ON memories (LOWER(content));
CREATE INDEX IF NOT EXISTS idx_memories_category_lower ON memories (LOWER(category_primary));

-- Composite indexes for filtered searches
CREATE INDEX IF NOT EXISTS idx_memories_agent_relevance ON memories (agent_id, relevance_score DESC);
CREATE INDEX IF NOT EXISTS idx_memories_agent_category ON memories (agent_id, category_primary);
CREATE INDEX IF NOT EXISTS idx_memories_agent_created ON memories (agent_id, created_at DESC);

-- Index for memories with vector embeddings
CREATE INDEX IF NOT EXISTS idx_memories_agent_vector ON memories (agent_id) WHERE vector_embedding IS NOT NULL;

-- Partial indexes for common filtering scenarios
CREATE INDEX IF NOT EXISTS idx_memories_high_relevance 
ON memories (agent_id, created_at DESC) 
WHERE relevance_score >= 0.7;

-- Statistics views for HSQLDB
CREATE VIEW IF NOT EXISTS memory_statistics AS
SELECT 
    COUNT(*) as total_memories,
    COUNT(DISTINCT agent_id) as unique_agents,
    COUNT(DISTINCT category_primary) as unique_categories,
    AVG(relevance_score) as avg_relevance_score,
    COUNT(CASE WHEN vector_embedding IS NOT NULL THEN 1 END) as memories_with_vectors,
    MIN(created_at) as oldest_memory,
    MAX(created_at) as newest_memory,
    MAX(last_accessed) as most_recent_access
FROM memories;

CREATE VIEW IF NOT EXISTS agent_memory_stats AS
SELECT 
    agent_id,
    COUNT(*) as memory_count,
    AVG(relevance_score) as avg_relevance_score,
    COUNT(DISTINCT category_primary) as unique_categories,
    MIN(created_at) as oldest_memory,
    MAX(created_at) as newest_memory,
    MAX(last_accessed) as most_recent_access
FROM memories 
GROUP BY agent_id;

CREATE VIEW IF NOT EXISTS category_distribution AS
SELECT 
    category_primary,
    COUNT(*) as memory_count,
    COUNT(DISTINCT agent_id) as agent_count,
    AVG(relevance_score) as avg_relevance_score
FROM memories 
WHERE category_primary IS NOT NULL
GROUP BY category_primary
ORDER BY memory_count DESC;

-- HSQLDB-specific procedures for maintenance
-- Note: HSQLDB uses different syntax for stored procedures

-- Cleanup procedure for old memories
CREATE PROCEDURE cleanup_old_memories(
    IN days_threshold INTEGER, 
    IN min_relevance_score DOUBLE,
    OUT deleted_count INTEGER
)
MODIFIES SQL DATA
BEGIN ATOMIC
    DELETE FROM memories 
    WHERE 
        created_at < DATEADD('day', -days_threshold, CURRENT_TIMESTAMP)
        AND relevance_score < min_relevance_score;
    
    SET deleted_count = (SELECT COUNT(*) FROM memories WHERE created_at < DATEADD('day', -days_threshold, CURRENT_TIMESTAMP));
END;

-- Procedure to get memory statistics
CREATE PROCEDURE get_memory_stats()
READS SQL DATA
DYNAMIC RESULT SETS 1
BEGIN ATOMIC
    DECLARE result CURSOR FOR
        SELECT * FROM memory_statistics;
    OPEN result;
END;

-- Function to calculate text similarity (simple implementation)
CREATE FUNCTION text_similarity(content1 VARCHAR(32672), content2 VARCHAR(32672))
RETURNS DOUBLE
READS SQL DATA
DETERMINISTIC
BEGIN ATOMIC
    DECLARE similarity DOUBLE DEFAULT 0.0;
    -- Simple Jaccard similarity implementation
    -- In a real implementation, you might want more sophisticated text similarity
    IF content1 IS NULL OR content2 IS NULL THEN
        RETURN 0.0;
    END IF;
    
    -- Simple word overlap calculation
    IF LOWER(content1) LIKE '%' || LOWER(content2) || '%' OR LOWER(content2) LIKE '%' || LOWER(content1) || '%' THEN
        SET similarity = 0.8;
    ELSEIF LOCATE(LOWER(SUBSTRING(content2, 1, 20)), LOWER(content1)) > 0 THEN
        SET similarity = 0.6;
    ELSEIF LOCATE(LOWER(SUBSTRING(content1, 1, 20)), LOWER(content2)) > 0 THEN
        SET similarity = 0.6;
    ELSE
        SET similarity = 0.1;
    END IF;
    
    RETURN similarity;
END;

-- Comments for documentation
COMMENT ON TABLE memories IS 'Main table storing memory records with text-based similarity search for HSQLDB';
COMMENT ON COLUMN memories.vector_embedding IS 'Vector embedding stored as comma-separated string (no native vector support in HSQLDB)';
COMMENT ON COLUMN memories.metadata_json IS 'Metadata stored as JSON string';
COMMENT ON VIEW memory_statistics IS 'Overall statistics about the memory system';
COMMENT ON VIEW agent_memory_stats IS 'Per-agent memory statistics';
COMMENT ON VIEW category_distribution IS 'Distribution of memories across categories';