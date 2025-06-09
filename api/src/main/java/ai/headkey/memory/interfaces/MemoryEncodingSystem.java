package ai.headkey.memory.interfaces;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.exceptions.MemoryNotFoundException;
import ai.headkey.memory.exceptions.StorageException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for the Memory Encoding System (MES).
 * 
 * The MES defines how data gets persisted and managed in the long-term memory store.
 * It handles the encoding, storage, indexing, and retrieval of memory records,
 * abstracting the underlying storage implementation details.
 * 
 * This interface supports various storage backends (SQL databases, NoSQL stores,
 * vector databases, etc.) and provides CRUD operations on memory records.
 * The encoding process may include generating embeddings for semantic search,
 * content compression, and index creation for efficient retrieval.
 * 
 * The interface follows the Open/Closed Principle by allowing different storage
 * implementations without changing the contract, and supports future extensions
 * like bulk operations and advanced querying.
 * 
 * @since 1.0
 */
public interface MemoryEncodingSystem {
    
    /**
     * Encodes and stores information into long-term memory.
     * 
     * This method performs the complete encoding and storage process:
     * 1. Encodes the content (e.g., generates vector embeddings)
     * 2. Creates indexes for efficient retrieval
     * 3. Persists the memory record to the storage backend
     * 4. Updates any necessary metadata or statistics
     * 
     * The encoding process may involve:
     * - Generating semantic vector embeddings
     * - Creating full-text search indexes
     * - Compressing content if needed
     * - Establishing relationships with existing memories
     * 
     * @param content The content to store
     * @param category The category label assigned by CCE
     * @param meta Metadata including agentId, timestamp, tags, etc.
     * @return MemoryRecord representing the stored memory with generated ID and storage details
     * @throws IllegalArgumentException if any required parameter is null
     * @throws StorageException if encoding or storage fails
     * 
     * @since 1.0
     */
    MemoryRecord encodeAndStore(String content, CategoryLabel category, Metadata meta, String agentId);
    
    /**
     * Retrieves a stored memory by its unique identifier.
     * 
     * Looks up and returns the complete memory record associated with
     * the given ID. This operation should be fast and efficient,
     * typically using primary key lookups.
     * 
     * @param memoryId The unique identifier of the memory
     * @return Optional containing the MemoryRecord if found, empty if not found
     * @throws IllegalArgumentException if memoryId is null or empty
     * @throws StorageException if retrieval operation fails
     * 
     * @since 1.0
     */
    Optional<MemoryRecord> getMemory(String memoryId);
    
    /**
     * Retrieves multiple memories by their identifiers.
     * 
     * Efficiently fetches multiple memory records in a single operation.
     * This method is optimized for batch retrieval scenarios.
     * 
     * @param memoryIds Collection of memory identifiers to retrieve
     * @return Map of memory ID to MemoryRecord for found memories
     * @throws IllegalArgumentException if memoryIds is null or empty
     * @throws StorageException if retrieval operation fails
     * 
     * @since 1.0
     */
    Map<String, MemoryRecord> getMemories(Set<String> memoryIds);
    
    /**
     * Updates an existing memory record.
     * 
     * Modifies an existing memory record with new content, metadata,
     * or category information. This may trigger re-encoding of the
     * content and updating of search indexes.
     * 
     * @param memoryRecord The updated memory record
     * @return The updated MemoryRecord with new version information
     * @throws IllegalArgumentException if memoryRecord is null or has invalid ID
     * @throws MemoryNotFoundException if the memory doesn't exist
     * @throws StorageException if update operation fails
     * 
     * @since 1.0
     */
    MemoryRecord updateMemory(MemoryRecord memoryRecord);
    
    /**
     * Deletes or archives a memory entry by ID.
     * 
     * Removes the memory from active storage. Depending on implementation,
     * this may be a soft delete (marking as inactive) or hard delete
     * (permanent removal). Used by forgetting processes and explicit
     * deletion requests.
     * 
     * @param memoryId The ID of the memory to remove
     * @return true if the memory was successfully removed, false if not found
     * @throws IllegalArgumentException if memoryId is null or empty
     * @throws StorageException if deletion operation fails
     * 
     * @since 1.0
     */
    boolean removeMemory(String memoryId);
    
    /**
     * Deletes multiple memories by their identifiers.
     * 
     * Efficiently removes multiple memory records in a single operation.
     * This method is optimized for batch deletion scenarios like forgetting.
     * 
     * @param memoryIds Collection of memory identifiers to remove
     * @return Set of memory IDs that were successfully removed
     * @throws IllegalArgumentException if memoryIds is null or empty
     * @throws StorageException if deletion operation fails
     * 
     * @since 1.0
     */
    Set<String> removeMemories(Set<String> memoryIds);
    
    /**
     * Searches for memories based on content similarity.
     * 
     * Performs semantic or textual search to find memories similar to
     * the given query content. This typically uses vector similarity
     * search or full-text search capabilities.
     * 
     * @param queryContent The content to search for
     * @param limit Maximum number of results to return
     * @return List of MemoryRecords ordered by relevance/similarity
     * @throws IllegalArgumentException if queryContent is null or limit < 1
     * @throws StorageException if search operation fails
     * 
     * @since 1.0
     */
    List<MemoryRecord> searchSimilar(String queryContent, int limit, String agentId);
    
    /**
     * Finds memories for a specific agent.
     * 
     * Retrieves all active memories belonging to the specified agent,
     * optionally filtered by additional criteria.
     * 
     * @param agentId The agent identifier
     * @param limit Maximum number of memories to return (0 for no limit)
     * @return List of MemoryRecords for the agent, ordered by creation time (newest first)
     * @throws IllegalArgumentException if agentId is null or empty
     * @throws StorageException if query operation fails
     * 
     * @since 1.0
     */
    List<MemoryRecord> getMemoriesForAgent(String agentId, int limit);
    
    /**
     * Finds memories in a specific category.
     * 
     * Retrieves all memories that have been assigned to the specified
     * category, optionally filtered by agent or other criteria.
     * 
     * @param category The category to search for
     * @param agentId Optional agent ID filter (null for all agents)
     * @param limit Maximum number of memories to return (0 for no limit)
     * @return List of MemoryRecords in the category
     * @throws IllegalArgumentException if category is null or empty
     * @throws StorageException if query operation fails
     * 
     * @since 1.0
     */
    List<MemoryRecord> getMemoriesInCategory(String category, String agentId, int limit);
    
    /**
     * Gets memories older than a specified age.
     * 
     * Finds memories that were created before a certain timestamp,
     * useful for implementing age-based forgetting strategies.
     * 
     * @param olderThanSeconds Age threshold in seconds
     * @param agentId Optional agent ID filter (null for all agents)
     * @param limit Maximum number of memories to return (0 for no limit)
     * @return List of old MemoryRecords ordered by age (oldest first)
     * @throws IllegalArgumentException if olderThanSeconds < 0
     * @throws StorageException if query operation fails
     * 
     * @since 1.0
     */
    List<MemoryRecord> getOldMemories(long olderThanSeconds, String agentId, int limit);
    
    /**
     * Gets memory usage statistics.
     * 
     * Returns comprehensive statistics about memory storage including:
     * - Total number of memories stored
     * - Storage size and usage
     * - Category distribution
     * - Agent distribution
     * - Average access frequencies
     * - Performance metrics
     * 
     * @return Map containing various storage statistics and metrics
     * 
     * @since 1.0
     */
    Map<String, Object> getStorageStatistics();
    
    /**
     * Gets memory statistics for a specific agent.
     * 
     * Returns agent-specific memory statistics including:
     * - Total memories for the agent
     * - Category breakdown
     * - Storage usage
     * - Access patterns
     * - Recent activity
     * 
     * @param agentId The agent ID to get statistics for
     * @return Map containing agent-specific statistics
     * @throws IllegalArgumentException if agentId is null or empty
     * 
     * @since 1.0
     */
    Map<String, Object> getAgentStatistics(String agentId);
    
    /**
     * Optimizes the storage system.
     * 
     * Performs maintenance operations to optimize storage performance:
     * - Rebuilding indexes
     * - Compacting storage
     * - Updating statistics
     * - Cleaning up unused resources
     * 
     * This operation may be long-running and should typically be
     * executed during maintenance windows.
     * 
     * @param vacuum Whether to perform deep cleanup/vacuum operations
     * @return Map containing optimization results and metrics
     * @throws StorageException if optimization fails
     * 
     * @since 1.0
     */
    Map<String, Object> optimize(boolean vacuum);
    
    /**
     * Checks if the storage system is healthy and ready.
     * 
     * Performs a health check of the storage system including:
     * - Database/storage connectivity
     * - Index integrity
     * - Available disk space
     * - Memory usage
     * - Performance indicators
     * 
     * @return true if the storage system is healthy and ready
     * 
     * @since 1.0
     */
    boolean isHealthy();
    
    /**
     * Gets the current storage capacity and utilization.
     * 
     * Returns information about storage limits and current usage:
     * - Maximum capacity (if applicable)
     * - Current utilization
     * - Available space
     * - Growth projections
     * 
     * @return Map containing capacity and utilization metrics
     * 
     * @since 1.0
     */
    Map<String, Object> getCapacityInfo();
}