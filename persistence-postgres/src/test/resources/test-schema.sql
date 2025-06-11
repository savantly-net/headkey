-- Test database initialization script for HeadKey Belief Storage
-- This script creates the necessary tables and indexes for testing

-- Drop tables if they exist (for clean test runs)
DROP TABLE IF EXISTS belief_relationship_metadata CASCADE;
DROP TABLE IF EXISTS belief_relationships CASCADE;
DROP TABLE IF EXISTS belief_tags CASCADE;
DROP TABLE IF EXISTS belief_evidence_memories CASCADE;
DROP TABLE IF EXISTS conflict_belief_ids CASCADE;
DROP TABLE IF EXISTS belief_conflicts CASCADE;
DROP TABLE IF EXISTS beliefs CASCADE;

-- Create beliefs table
CREATE TABLE beliefs (
    id VARCHAR(100) PRIMARY KEY,
    agent_id VARCHAR(100) NOT NULL,
    statement TEXT NOT NULL,
    confidence DECIMAL(5,4) NOT NULL CHECK (confidence >= 0.0 AND confidence <= 1.0),
    category VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reinforcement_count INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT DEFAULT 0
);

-- Create belief_evidence_memories table (for evidence memory IDs)
CREATE TABLE belief_evidence_memories (
    belief_id VARCHAR(100) NOT NULL REFERENCES beliefs(id) ON DELETE CASCADE,
    memory_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (belief_id, memory_id)
);

-- Create belief_tags table (for tags)
CREATE TABLE belief_tags (
    belief_id VARCHAR(100) NOT NULL REFERENCES beliefs(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (belief_id, tag)
);

-- Create belief_conflicts table
CREATE TABLE belief_conflicts (
    id VARCHAR(100) PRIMARY KEY,
    agent_id VARCHAR(100) NOT NULL,
    new_evidence_memory_id VARCHAR(100),
    description TEXT,
    conflict_type VARCHAR(50),
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution_strategy VARCHAR(100),
    resolution_notes TEXT,
    severity VARCHAR(20) DEFAULT 'MEDIUM',
    auto_resolvable BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create conflict_belief_ids table (for conflicting belief IDs)
CREATE TABLE conflict_belief_ids (
    conflict_id VARCHAR(100) NOT NULL REFERENCES belief_conflicts(id) ON DELETE CASCADE,
    belief_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (conflict_id, belief_id)
);

-- Create indexes for performance

-- Beliefs table indexes
CREATE INDEX idx_belief_agent_id ON beliefs(agent_id);
CREATE INDEX idx_belief_category ON beliefs(category);
CREATE INDEX idx_belief_confidence ON beliefs(confidence);
CREATE INDEX idx_belief_active ON beliefs(active);
CREATE INDEX idx_belief_created_at ON beliefs(created_at);
CREATE INDEX idx_belief_agent_category ON beliefs(agent_id, category);
CREATE INDEX idx_belief_agent_active ON beliefs(agent_id, active);

-- Evidence memories indexes
CREATE INDEX idx_evidence_belief_id ON belief_evidence_memories(belief_id);
CREATE INDEX idx_evidence_memory_id ON belief_evidence_memories(memory_id);

-- Tags indexes
CREATE INDEX idx_tags_belief_id ON belief_tags(belief_id);
CREATE INDEX idx_tags_tag ON belief_tags(tag);

-- Conflicts table indexes
CREATE INDEX idx_conflict_agent_id ON belief_conflicts(agent_id);
CREATE INDEX idx_conflict_resolved ON belief_conflicts(resolved);
CREATE INDEX idx_conflict_detected_at ON belief_conflicts(detected_at);
CREATE INDEX idx_conflict_agent_resolved ON belief_conflicts(agent_id, resolved);
CREATE INDEX idx_conflict_resolution_strategy ON belief_conflicts(resolution_strategy);
CREATE INDEX idx_conflict_severity ON belief_conflicts(severity);
CREATE INDEX idx_conflict_type ON belief_conflicts(conflict_type);

-- Conflict belief IDs indexes
CREATE INDEX idx_conflict_beliefs ON conflict_belief_ids(conflict_id);
CREATE INDEX idx_conflict_belief_lookup ON conflict_belief_ids(belief_id);

-- Create functions for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_last_updated_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for automatic timestamp updates
CREATE TRIGGER update_beliefs_last_updated 
    BEFORE UPDATE ON beliefs 
    FOR EACH ROW 
    EXECUTE FUNCTION update_last_updated_column();

CREATE TRIGGER update_conflicts_last_updated 
    BEFORE UPDATE ON belief_conflicts 
    FOR EACH ROW 
    EXECUTE FUNCTION update_last_updated_column();

-- Insert some test data for initial testing
INSERT INTO beliefs (id, agent_id, statement, confidence, category, created_at, last_updated) VALUES
('test-belief-1', 'test-user-1', 'Test belief statement 1', 0.8, 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('test-belief-2', 'test-user-1', 'Test belief statement 2', 0.6, 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('test-belief-3', 'test-user-2', 'Test belief statement 3', 0.9, 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO belief_evidence_memories (belief_id, memory_id) VALUES
('test-belief-1', 'test-memory-1'),
('test-belief-2', 'test-memory-2'),
('test-belief-3', 'test-memory-3');

INSERT INTO belief_tags (belief_id, tag) VALUES
('test-belief-1', 'test-tag'),
('test-belief-2', 'test-tag'),
('test-belief-3', 'another-tag');

-- Create belief_relationships table
CREATE TABLE belief_relationships (
    id VARCHAR(100) PRIMARY KEY,
    source_belief_id VARCHAR(100) NOT NULL,
    target_belief_id VARCHAR(100) NOT NULL,
    agent_id VARCHAR(100) NOT NULL,
    relationship_type VARCHAR(50) NOT NULL,
    strength DECIMAL(5,4) NOT NULL DEFAULT 1.0,
    effective_from TIMESTAMP WITH TIME ZONE,
    effective_until TIMESTAMP WITH TIME ZONE,
    deprecation_reason TEXT,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT DEFAULT 0,
    
    -- Constraints
    CONSTRAINT rel_strength_check CHECK (strength >= 0.0 AND strength <= 1.0),
    CONSTRAINT rel_different_beliefs CHECK (source_belief_id != target_belief_id),
    CONSTRAINT rel_temporal_validity CHECK (
        effective_from IS NULL OR 
        effective_until IS NULL OR 
        effective_from <= effective_until
    ),
    CONSTRAINT rel_valid_type CHECK (
        relationship_type IN (
            'SUPERSEDES', 'UPDATES', 'DEPRECATES', 'REPLACES',
            'SUPPORTS', 'CONTRADICTS', 'IMPLIES', 'REINFORCES', 'WEAKENS',
            'RELATES_TO', 'SPECIALIZES', 'GENERALIZES', 'EXTENDS', 'DERIVES_FROM',
            'CAUSES', 'CAUSED_BY', 'ENABLES', 'PREVENTS',
            'DEPENDS_ON', 'PRECEDES', 'FOLLOWS', 'CONTEXT_FOR',
            'EVIDENCED_BY', 'PROVIDES_EVIDENCE_FOR', 'CONFLICTS_WITH',
            'SIMILAR_TO', 'ANALOGOUS_TO', 'CONTRASTS_WITH',
            'CUSTOM'
        )
    ),
    
    -- Foreign key constraints
    CONSTRAINT fk_rel_source_belief FOREIGN KEY (source_belief_id) 
        REFERENCES beliefs(id) ON DELETE CASCADE,
    CONSTRAINT fk_rel_target_belief FOREIGN KEY (target_belief_id) 
        REFERENCES beliefs(id) ON DELETE CASCADE
);

-- Metadata table for flexible relationship properties
CREATE TABLE belief_relationship_metadata (
    relationship_id VARCHAR(100) NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT,
    PRIMARY KEY (relationship_id, metadata_key),
    CONSTRAINT fk_rel_metadata FOREIGN KEY (relationship_id) 
        REFERENCES belief_relationships(id) ON DELETE CASCADE
);

-- Relationship table indexes
CREATE INDEX idx_rel_source_belief ON belief_relationships(source_belief_id);
CREATE INDEX idx_rel_target_belief ON belief_relationships(target_belief_id);
CREATE INDEX idx_rel_agent_id ON belief_relationships(agent_id);
CREATE INDEX idx_rel_type ON belief_relationships(relationship_type);
CREATE INDEX idx_rel_active ON belief_relationships(active);
CREATE INDEX idx_rel_strength ON belief_relationships(strength DESC);
CREATE INDEX idx_rel_temporal ON belief_relationships(effective_from, effective_until);
CREATE INDEX idx_rel_agent_active ON belief_relationships(agent_id, active);
CREATE INDEX idx_rel_agent_type ON belief_relationships(agent_id, relationship_type);
CREATE INDEX idx_rel_source_type ON belief_relationships(source_belief_id, relationship_type);
CREATE INDEX idx_rel_target_type ON belief_relationships(target_belief_id, relationship_type);

-- Metadata table indexes
CREATE INDEX idx_rel_metadata_relationship ON belief_relationship_metadata(relationship_id);
CREATE INDEX idx_rel_metadata_key ON belief_relationship_metadata(metadata_key);

-- Relationship triggers for timestamp updates
CREATE TRIGGER update_relationships_last_updated 
    BEFORE UPDATE ON belief_relationships 
    FOR EACH ROW 
    EXECUTE FUNCTION update_last_updated_column();

-- Insert test relationship data
INSERT INTO belief_relationships (id, source_belief_id, target_belief_id, agent_id, relationship_type, strength, created_at, last_updated) VALUES
('test-rel-1', 'test-belief-1', 'test-belief-2', 'test-user-1', 'SUPPORTS', 0.8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('test-rel-2', 'test-belief-2', 'test-belief-3', 'test-user-1', 'RELATES_TO', 0.7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO belief_relationship_metadata (relationship_id, metadata_key, metadata_value) VALUES
('test-rel-1', 'source', 'test_conversation'),
('test-rel-1', 'confidence', '0.85'),
('test-rel-2', 'type', 'semantic');