package ai.headkey.persistence.elastic.services;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.headkey.memory.abstracts.AbstractMemoryEncodingSystem;
import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.exceptions.MemoryNotFoundException;
import ai.headkey.memory.exceptions.StorageException;
import ai.headkey.memory.interfaces.VectorEmbeddingGenerator;
import ai.headkey.persistence.elastic.configuration.ElasticsearchConfiguration;
import ai.headkey.persistence.elastic.documents.MemoryDocument;
import ai.headkey.persistence.elastic.mappers.MemoryDocumentMapper;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;

/**
 * Elasticsearch implementation of the MemoryEncodingSystem interface.
 *
 * This implementation provides persistent storage using Elasticsearch for the
 * HeadKey memory system.
 * It supports:
 * - Full-text search on memory content
 * - Vector similarity search using dense_vector fields
 * - Efficient filtering by agent, category, and temporal ranges
 * - Bulk operations for high-performance scenarios
 * - Automatic index management and mapping creation
 * - Connection pooling and error handling
 *
 * Features:
 * - Agent-based index isolation for multi-tenancy
 * - Optimized mappings for search and aggregation performance
 * - Vector embedding storage and similarity search
 * - Comprehensive error handling and logging
 * - Health checking and statistics collection
 * - Automatic index creation with proper mappings
 */
public class ElasticsearchMemoryEncodingSystem
        extends AbstractMemoryEncodingSystem {

    private static final Logger logger = LoggerFactory.getLogger(
            ElasticsearchMemoryEncodingSystem.class);

    private final ElasticsearchConfiguration config;
    private final ElasticsearchClient client;
    private final Map<String, Boolean> indexExistsCache = new ConcurrentHashMap<>();

    // Configuration parameters
    private final int batchSize;
    private final boolean autoCreateIndices;
    private final int searchTimeout;
    private final int maxSimilarityResults;
    private final double similarityThreshold;

    static final int DEFAULT_VECTOR_DIMENSION = 1536; // Default dimension for embeddings

    /**
     * Creates a new Elasticsearch memory encoding system with default
     * configuration.
     *
     * @param config The Elasticsearch configuration
     */
    public ElasticsearchMemoryEncodingSystem(
            ElasticsearchConfiguration config) {
        this(config, null, 100, true, 30000, 1000, 0.0);
    }

    /**
     * Creates a new Elasticsearch memory encoding system with embedding generator.
     *
     * @param config             The Elasticsearch configuration
     * @param embeddingGenerator Function to generate vector embeddings
     */
    public ElasticsearchMemoryEncodingSystem(
            ElasticsearchConfiguration config,
            VectorEmbeddingGenerator embeddingGenerator) {
        this(config, embeddingGenerator, 100, true, 30000, 1000, 0.0);
    }

    /**
     * Creates a new Elasticsearch memory encoding system with full configuration.
     *
     * @param config               The Elasticsearch configuration
     * @param embeddingGenerator   Function to generate vector embeddings
     * @param batchSize            Batch size for bulk operations
     * @param autoCreateIndices    Whether to automatically create indices
     * @param searchTimeout        Search timeout in milliseconds
     * @param maxSimilarityResults Maximum results for similarity search
     * @param similarityThreshold  Minimum similarity score threshold
     */
    public ElasticsearchMemoryEncodingSystem(
            ElasticsearchConfiguration config,
            VectorEmbeddingGenerator embeddingGenerator,
            int batchSize,
            boolean autoCreateIndices,
            int searchTimeout,
            int maxSimilarityResults,
            double similarityThreshold) {
        super(embeddingGenerator);
        this.config = Objects.requireNonNull(
                config,
                "Elasticsearch configuration cannot be null");
        this.client = config.getClient();
        this.batchSize = Math.max(1, batchSize);
        this.autoCreateIndices = autoCreateIndices;
        this.searchTimeout = Math.max(1000, searchTimeout);
        this.maxSimilarityResults = Math.max(1, maxSimilarityResults);
        this.similarityThreshold = Math.max(
                0.0,
                Math.min(1.0, similarityThreshold));

        logger.info(
                "Initialized Elasticsearch memory encoding system with batch size: {}, auto-create indices: {}",
                this.batchSize,
                this.autoCreateIndices);
    }

    @Override
    protected MemoryRecord doEncodeAndStore(
            String content,
            CategoryLabel category,
            Metadata meta,
            String agentId,
            double[] embedding) {
        try {
            // Generate unique ID
            String memoryId = generateMemoryId();

            // Create MemoryRecord first
            MemoryRecord memoryRecord = new MemoryRecord(
                    memoryId,
                    agentId,
                    content);
            memoryRecord.setCategory(category);
            memoryRecord.setMetadata(meta);
            memoryRecord.setCreatedAt(Instant.now());
            memoryRecord.setLastAccessed(Instant.now());
            memoryRecord.setVersion(1L);

            // Convert to document
            MemoryDocument document = MemoryDocumentMapper.toDocumentWithEmbedding(
                    memoryRecord,
                    embedding);

            // Ensure index exists
            String indexName = config.getMemoryIndexName(agentId);
            ensureIndexExists(indexName, createMemoryIndexMapping());

            // Store document
            IndexRequest<MemoryDocument> request = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(memoryId)
                    .document(document)
                    .refresh(Refresh.WaitFor));

            IndexResponse response = client.index(request);

            if (response.result() == Result.Created ||
                    response.result() == Result.Updated) {
                logger.debug(
                        "Successfully stored memory {} in index {}",
                        memoryId,
                        indexName);
                return memoryRecord;
            } else {
                throw new StorageException(
                        "Failed to store memory: unexpected result " +
                                response.result());
            }
        } catch (Exception e) {
            logger.error(
                    "Failed to encode and store memory for agent {}: {}",
                    agentId,
                    e.getMessage(),
                    e);
            throw new StorageException("Failed to encode and store memory", e);
        }
    }

    @Override
    protected Optional<MemoryRecord> doGetMemory(String memoryId) {
        try {
            // Extract agent ID from memory ID or search across indices
            // For simplicity, we'll search across all memory indices
            List<String> indexNames = getAllMemoryIndices();

            for (String indexName : indexNames) {
                try {
                    GetRequest request = GetRequest.of(g -> g.index(indexName).id(memoryId));

                    GetResponse<MemoryDocument> response = client.get(
                            request,
                            MemoryDocument.class);

                    if (response.found()) {
                        MemoryDocument document = response.source();
                        if (document != null) {
                            document.updateLastAccessed();

                            // Update the document with new access time
                            updateDocumentAccessTime(
                                    indexName,
                                    memoryId,
                                    document);

                            MemoryRecord memoryRecord = MemoryDocumentMapper.fromDocument(document);
                            logger.debug(
                                    "Retrieved memory {} from index {}",
                                    memoryId,
                                    indexName);
                            return Optional.of(memoryRecord);
                        }
                    }
                } catch (ElasticsearchException e) {
                    // Index might not exist, continue to next
                    logger.debug(
                            "Index {} not found or accessible: {}",
                            indexName,
                            e.getMessage());
                }
            }

            logger.debug("Memory {} not found in any index", memoryId);
            return Optional.empty();
        } catch (Exception e) {
            logger.error(
                    "Failed to retrieve memory {}: {}",
                    memoryId,
                    e.getMessage(),
                    e);
            throw new StorageException(
                    "Failed to retrieve memory: " + memoryId,
                    e);
        }
    }

    @Override
    protected Map<String, MemoryRecord> doGetMemories(Set<String> memoryIds) {
        Map<String, MemoryRecord> results = new HashMap<>();

        try {
            List<String> indexNames = getAllMemoryIndices();

            for (String indexName : indexNames) {
                try {
                    // Use Multi-get API for efficient bulk retrieval
                    List<String> remainingIds = memoryIds
                            .stream()
                            .filter(id -> !results.containsKey(id))
                            .collect(Collectors.toList());

                    if (remainingIds.isEmpty()) {
                        break;
                    }

                    MgetRequest request = MgetRequest.of(m -> m.index(indexName).ids(remainingIds));

                    MgetResponse<MemoryDocument> response = client.mget(
                            request,
                            MemoryDocument.class);

                    for (var item : response.docs()) {
                        if (item.isResult() && item.result().found()) {
                            MemoryDocument document = item.result().source();
                            if (document != null) {
                                document.updateLastAccessed();
                                MemoryRecord memoryRecord = MemoryDocumentMapper.fromDocument(document);
                                results.put(memoryRecord.getId(), memoryRecord);
                            }
                        }
                    }
                } catch (ElasticsearchException e) {
                    logger.debug(
                            "Index {} not accessible: {}",
                            indexName,
                            e.getMessage());
                }
            }

            logger.debug(
                    "Retrieved {} out of {} requested memories",
                    results.size(),
                    memoryIds.size());
            return results;
        } catch (Exception e) {
            logger.error("Failed to retrieve memories: {}", e.getMessage(), e);
            throw new StorageException("Failed to retrieve memories", e);
        }
    }

    @Override
    protected MemoryRecord doUpdateMemory(
            MemoryRecord memoryRecord,
            double[] embedding) {
        try {
            String memoryId = memoryRecord.getId();
            String agentId = memoryRecord.getAgentId();
            String indexName = config.getMemoryIndexName(agentId);

            // Check if memory exists
            if (!documentExists(indexName, memoryId)) {
                throw new MemoryNotFoundException(
                        "Memory not found: " + memoryId);
            }

            // Update document
            MemoryDocument document = MemoryDocumentMapper.toDocumentWithEmbedding(
                    memoryRecord,
                    embedding);
            document.updateLastUpdated();

            IndexRequest<MemoryDocument> request = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(memoryId)
                    .document(document)
                    .refresh(Refresh.WaitFor));

            IndexResponse response = client.index(request);

            if (response.result() == Result.Updated ||
                    response.result() == Result.Created) {
                logger.debug(
                        "Successfully updated memory {} in index {}",
                        memoryId,
                        indexName);
                return memoryRecord;
            } else {
                throw new StorageException(
                        "Failed to update memory: unexpected result " +
                                response.result());
            }
        } catch (MemoryNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error(
                    "Failed to update memory {}: {}",
                    memoryRecord.getId(),
                    e.getMessage(),
                    e);
            throw new StorageException(
                    "Failed to update memory: " + memoryRecord.getId(),
                    e);
        }
    }

    @Override
    protected boolean doRemoveMemory(String memoryId) {
        try {
            List<String> indexNames = getAllMemoryIndices();

            for (String indexName : indexNames) {
                try {
                    DeleteRequest request = DeleteRequest
                            .of(d -> d.index(indexName).id(memoryId).refresh(Refresh.WaitFor));

                    DeleteResponse response = client.delete(request);

                    if (response.result() == Result.Deleted) {
                        logger.debug(
                                "Successfully deleted memory {} from index {}",
                                memoryId,
                                indexName);
                        return true;
                    }
                } catch (ElasticsearchException e) {
                    logger.debug(
                            "Failed to delete from index {}: {}",
                            indexName,
                            e.getMessage());
                }
            }

            logger.debug("Memory {} not found for deletion", memoryId);
            return false;
        } catch (Exception e) {
            logger.error(
                    "Failed to remove memory {}: {}",
                    memoryId,
                    e.getMessage(),
                    e);
            throw new StorageException(
                    "Failed to remove memory: " + memoryId,
                    e);
        }
    }

    @Override
    protected Set<String> doRemoveMemories(Set<String> memoryIds) {
        Set<String> removedIds = new HashSet<>();

        try {
            List<String> indexNames = getAllMemoryIndices();

            for (String indexName : indexNames) {
                try {
                    List<BulkOperation> operations = memoryIds
                            .stream()
                            .filter(id -> !removedIds.contains(id))
                            .map(id -> BulkOperation.of(b -> b.delete(
                                    DeleteOperation.of(d -> d.index(indexName).id(id)))))
                            .collect(Collectors.toList());

                    if (operations.isEmpty()) {
                        continue;
                    }

                    BulkRequest request = BulkRequest.of(b -> b.operations(operations).refresh(Refresh.WaitFor));

                    BulkResponse response = client.bulk(request);

                    for (var item : response.items()) {
                        if (item.status() == 200 || item.status() == 404) {
                            // 404 means the document was not found, which is acceptable
                            removedIds.add(item.id());
                        } else {
                            logger.debug(
                                    "Failed to delete memory {} from index {}: {}",
                                    item.id(),
                                    indexName,
                                    item.error());
                        }
                    }
                } catch (ElasticsearchException e) {
                    logger.debug(
                            "Failed to bulk delete from index {}: {}",
                            indexName,
                            e.getMessage());
                }
            }

            logger.debug(
                    "Successfully deleted {} out of {} requested memories",
                    removedIds.size(),
                    memoryIds.size());
            return removedIds;
        } catch (Exception e) {
            logger.error("Failed to remove memories: {}", e.getMessage(), e);
            throw new StorageException("Failed to remove memories", e);
        }
    }

    @Override
    protected List<MemoryRecord> doSearchSimilar(
            String queryContent,
            double[] queryEmbedding,
            int limit,
            String agentId) {
        try {
            String indexName = config.getMemoryIndexName(agentId);

            if (!indexExists(indexName)) {
                logger.debug(
                        "Index {} does not exist, returning empty results",
                        indexName);
                return new ArrayList<>();
            }

            Query query;

            if (queryEmbedding != null && queryEmbedding.length > 0) {
                // Convert embedding to float array
                Float[] queryEmbeddingFloats = Arrays.stream(queryEmbedding)
                        .boxed()
                        .map(d -> d.floatValue())
                        .toArray(Float[]::new);

                // Vector similarity search
                query = Query.of(q -> q.knn(knn -> knn
                        .field("content_embedding")
                        .queryVector(Arrays.stream(queryEmbeddingFloats).collect(Collectors.toList()))
                        .k(limit)  // Number of nearest neighbors to retrieve
                        .numCandidates(100)  // Controls recall vs performance tradeoff
                        .filter(Query.of(fq -> fq
                                .term(t -> t
                                .field("agent_id")
                                .value(agentId)
                                )
                        ))
                        ));
            } else {
                // Text-based search
                query = Query.of(q -> q.bool(
                        BoolQuery.of(b -> b
                                .must(
                                        Query.of(m -> m.term(t -> t.field("agent_id").value(agentId))))
                                .must(
                                        Query.of(m -> m.multiMatch(mm -> mm
                                                .query(queryContent)
                                                .fields(
                                                        "content^2",
                                                        "primary_category",
                                                        "secondary_category",
                                                        "tags")))))));
            }

            SearchRequest request = SearchRequest.of(s -> s
                    .index(indexName)
                    .query(query)
                    .size(Math.min(limit, maxSimilarityResults))
                    .timeout(searchTimeout + "ms"));

            SearchResponse<MemoryDocument> response = client.search(
                    request,
                    MemoryDocument.class);

            List<MemoryRecord> results = new ArrayList<>();
            for (Hit<MemoryDocument> hit : response.hits().hits()) {
                MemoryDocument document = hit.source();
                if (document != null) {
                    // Filter by similarity threshold if using vector search
                    if (queryEmbedding != null && hit.score() != null) {
                        double normalizedScore = hit.score() - 1.0; // Subtract the +1.0 from script
                        if (normalizedScore < similarityThreshold) {
                            continue;
                        }
                    }

                    document.updateLastAccessed();
                    MemoryRecord memoryRecord = MemoryDocumentMapper.fromDocument(document);
                    results.add(memoryRecord);
                }
            }

            logger.debug(
                    "Found {} similar memories for query in index {}",
                    results.size(),
                    indexName);
            return results;
        } catch (Exception e) {
            logger.error(
                    "Failed to search similar memories for agent {}: {}",
                    agentId,
                    e.getMessage(),
                    e);
            throw new StorageException("Failed to search similar memories", e);
        }
    }

    @Override
    public List<MemoryRecord> getMemoriesForAgent(String agentId, int limit) {
        try {
            String indexName = config.getMemoryIndexName(agentId);

            if (!indexExists(indexName)) {
                return new ArrayList<>();
            }

            Query query = Query.of(q -> q.term(t -> t.field("agent_id").value(agentId)));

            SearchRequest request = SearchRequest.of(s -> s
                    .index(indexName)
                    .query(query)
                    .size(limit > 0 ? limit : 10000)
                    .sort(so -> so.field(f -> f
                            .field("created_at")
                            .order(
                                    co.elastic.clients.elasticsearch._types.SortOrder.Desc))));

            SearchResponse<MemoryDocument> response = client.search(
                    request,
                    MemoryDocument.class);

            return response
                    .hits()
                    .hits()
                    .stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(MemoryDocumentMapper::fromDocument)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                    "Failed to get memories for agent {}: {}",
                    agentId,
                    e.getMessage(),
                    e);
            throw new StorageException(
                    "Failed to get memories for agent: " + agentId,
                    e);
        }
    }

    @Override
    public List<MemoryRecord> getMemoriesInCategory(
            String category,
            String agentId,
            int limit) {
        try {
            String indexName = config.getMemoryIndexName(agentId);

            if (!indexExists(indexName)) {
                return new ArrayList<>();
            }

            BoolQuery.Builder boolQuery = new BoolQuery.Builder()
                    .must(
                            Query.of(q -> q.term(t -> t.field("agent_id").value(agentId))));

            // Search in both primary and secondary categories
            boolQuery
                    .should(
                            Query.of(q -> q.term(t -> t.field("primary_category").value(category))))
                    .should(
                            Query.of(q -> q.term(t -> t.field("secondary_category").value(category))))
                    .minimumShouldMatch("1");

            SearchRequest request = SearchRequest.of(s -> s
                    .index(indexName)
                    .query(Query.of(q -> q.bool(boolQuery.build())))
                    .size(limit > 0 ? limit : 10000));

            SearchResponse<MemoryDocument> response = client.search(
                    request,
                    MemoryDocument.class);

            return response
                    .hits()
                    .hits()
                    .stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(MemoryDocumentMapper::fromDocument)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                    "Failed to get memories in category {} for agent {}: {}",
                    category,
                    agentId,
                    e.getMessage(),
                    e);
            throw new StorageException(
                    "Failed to get memories in category: " + category,
                    e);
        }
    }

    @Override
    public List<MemoryRecord> getOldMemories(
            long olderThanSeconds,
            String agentId,
            int limit) {
        try {
            String indexName = config.getMemoryIndexName(agentId);

            if (!indexExists(indexName)) {
                return new ArrayList<>();
            }

            Instant threshold = Instant.now().minusSeconds(olderThanSeconds);

            Query query = Query.of(q -> q.bool(
                    BoolQuery.of(b -> b
                            .must(
                                    Query.of(m -> m.term(t -> t.field("agent_id").value(agentId))))
                            .must(
                                    Query.of(m -> m.range(r -> r
                                            .date(d -> d.field("created_at").lt(
                                                            threshold.toString()))))))));

            SearchRequest request = SearchRequest.of(s -> s
                    .index(indexName)
                    .query(query)
                    .size(limit > 0 ? limit : 10000)
                    .sort(so -> so.field(f -> f
                            .field("created_at")
                            .order(
                                    co.elastic.clients.elasticsearch._types.SortOrder.Asc))));

            SearchResponse<MemoryDocument> response = client.search(
                    request,
                    MemoryDocument.class);

            return response
                    .hits()
                    .hits()
                    .stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(MemoryDocumentMapper::fromDocument)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                    "Failed to get old memories for agent {}: {}",
                    agentId,
                    e.getMessage(),
                    e);
            throw new StorageException("Failed to get old memories", e);
        }
    }

    @Override
    public Map<String, Object> getAgentStatistics(String agentId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            String indexName = config.getMemoryIndexName(agentId);

            if (!indexExists(indexName)) {
                stats.put("totalMemories", 0L);
                stats.put("indexExists", false);
                return stats;
            }

            // Get total count
            CountRequest countRequest = CountRequest.of(c -> c
                    .index(indexName)
                    .query(
                            Query.of(q -> q.term(t -> t.field("agent_id").value(agentId)))));

            CountResponse countResponse = client.count(countRequest);
            stats.put("totalMemories", countResponse.count());
            stats.put("indexExists", true);
            stats.put("indexName", indexName);

            // Add more detailed statistics if needed
            stats.put("agentId", agentId);
        } catch (Exception e) {
            logger.error(
                    "Failed to get agent statistics for {}: {}",
                    agentId,
                    e.getMessage(),
                    e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    @Override
    public Map<String, Object> optimize(boolean vacuum) {
        Map<String, Object> results = new HashMap<>();

        try {
            // Force merge indices for optimization
            List<String> indexNames = getAllMemoryIndices();

            for (String indexName : indexNames) {
                try {
                    // Force merge can be expensive, so we limit it
                    client
                            .indices()
                            .forcemerge(fm -> fm.index(indexName).maxNumSegments(1L));

                    results.put(indexName + "_optimized", true);
                } catch (Exception e) {
                    logger.warn(
                            "Failed to optimize index {}: {}",
                            indexName,
                            e.getMessage());
                    results.put(indexName + "_optimized", false);
                    results.put(indexName + "_error", e.getMessage());
                }
            }

            results.put("optimizationCompleted", true);
            results.put("vacuum", vacuum);
        } catch (Exception e) {
            logger.error("Failed to optimize storage: {}", e.getMessage(), e);
            results.put("optimizationCompleted", false);
            results.put("error", e.getMessage());
        }

        return results;
    }

    @Override
    protected boolean doHealthCheck() {
        return config.isHealthy();
    }

    @Override
    protected long getCurrentMemoryCount() {
        try {
            List<String> indexNames = getAllMemoryIndices();
            long totalCount = 0;

            for (String indexName : indexNames) {
                try {
                    CountRequest request = CountRequest.of(c -> c.index(indexName));
                    CountResponse response = client.count(request);
                    totalCount += response.count();
                } catch (Exception e) {
                    logger.debug(
                            "Failed to count documents in index {}: {}",
                            indexName,
                            e.getMessage());
                }
            }

            return totalCount;
        } catch (Exception e) {
            logger.error(
                    "Failed to get current memory count: {}",
                    e.getMessage(),
                    e);
            return 0;
        }
    }

    @Override
    protected void addImplementationSpecificStatistics(
            Map<String, Object> stats) {
        stats.put("implementation", "Elasticsearch");
        stats.put("batchSize", batchSize);
        stats.put("autoCreateIndices", autoCreateIndices);
        stats.put("searchTimeout", searchTimeout);
        stats.put("maxSimilarityResults", maxSimilarityResults);
        stats.put("similarityThreshold", similarityThreshold);
        stats.put("indexCacheSize", indexExistsCache.size());

        try {
            stats.put("clusterHealth", config.isHealthy());
            stats.put("totalIndices", getAllMemoryIndices().size());
        } catch (Exception e) {
            stats.put("clusterHealth", false);
            stats.put("healthCheckError", e.getMessage());
        }
    }

    // Helper methods

    private void ensureIndexExists(
            String indexName,
            Map<String, Property> mapping) throws IOException {
        if (indexExistsCache.getOrDefault(indexName, false)) {
            return;
        }

        if (!autoCreateIndices) {
            return;
        }

        ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(indexName));

        if (!client.indices().exists(existsRequest).value()) {
            CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .mappings(m -> m.properties(mapping))
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("0")
                            .refreshInterval(Time.of(fn -> fn.time("1s")))));

            client.indices().create(createRequest);
            logger.info("Created index: {}", indexName);
        }

        indexExistsCache.put(indexName, true);
    }

    private boolean indexExists(String indexName) {
        try {
            if (indexExistsCache.containsKey(indexName)) {
                return indexExistsCache.get(indexName);
            }

            ExistsRequest request = ExistsRequest.of(e -> e.index(indexName));
            boolean exists = client.indices().exists(request).value();
            indexExistsCache.put(indexName, exists);
            return exists;
        } catch (Exception e) {
            logger.debug(
                    "Failed to check if index {} exists: {}",
                    indexName,
                    e.getMessage());
            return false;
        }
    }

    private boolean documentExists(String indexName, String documentId) {
        try {
            return client.exists(fn -> fn
                    .index(indexName)
                    .id(documentId)).value();
        } catch (Exception e) {
            logger.debug(
                    "Failed to check if document {} exists in index {}: {}",
                    documentId,
                    indexName,
                    e.getMessage());
            return false;
        }
    }

    private void updateDocumentAccessTime(
            String indexName,
            String memoryId,
            MemoryDocument document) {
        try {
            // Update only the access-related fields
            Map<String, Object> updateDoc = new HashMap<>();
            updateDoc.put("last_accessed", document.getLastAccessed());
            updateDoc.put("access_count", document.getAccessCount());

            UpdateRequest<Map<String, Object>, Map<String, Object>> request = UpdateRequest
                    .of(u -> u.index(indexName).id(memoryId).doc(updateDoc));

            client.update(request, Map.class);
        } catch (Exception e) {
            logger.debug(
                    "Failed to update access time for memory {} in index {}: {}",
                    memoryId,
                    indexName,
                    e.getMessage());
        }
    }

    private List<String> getAllMemoryIndices() {
        // For simplicity, we'll return a pattern-based approach
        // In a real implementation, you might want to use the cluster API to get actual
        // indices
        List<String> indices = new ArrayList<>();

        // Add common agent patterns - this is a simplified approach
        // In practice, you might maintain a registry of agent IDs or use index patterns
        indices.add(config.getMemoryIndexName("default"));

        return indices;
    }

    private Map<String, Property> createMemoryIndexMapping() {
        Map<String, Property> properties = new HashMap<>();

        // Basic fields
        properties.put("agent_id", new Property.Builder().keyword(v -> v).build());
        properties.put("content", Property.of(p -> p
                .text(t -> t.analyzer("standard"))));

        properties.put("primary_category", Property.of(p -> p
                .keyword(k -> k)));

        properties.put("secondary_category", Property.of(p -> p
                .keyword(k -> k)));

        properties.put("tags", Property.of(p -> p
                .keyword(k -> k)));

        properties.put("content_embedding", Property.of(p -> p
                .denseVector(dv -> dv
                        .dims(DEFAULT_VECTOR_DIMENSION)
                        .index(true)
                        .similarity(DenseVectorSimilarity.Cosine))));

        // Score fields
        properties.put("category_confidence", Property.of(p -> p
                .float_(f -> f)));

        properties.put("relevance_score", Property.of(p -> p
                .float_(f -> f)));

        properties.put("importance_score", Property.of(p -> p
                .float_(f -> f)));

        // Metadata fields
        properties.put("source", Property.of(p -> p
                .keyword(k -> k)));

        properties.put("access_count", Property.of(p -> p
                .integer(i -> i)));

        // Timestamp fields
        properties.put("created_at", Property.of(p -> p
                .date(d -> d)));

        properties.put("last_accessed", Property.of(p -> p
                .date(d -> d)));

        properties.put("last_updated", Property.of(p -> p
                .date(d -> d)));

        // Additional fields
        properties.put("version", Property.of(p -> p
                .long_(l -> l)));

        properties.put("active", Property.of(p -> p
                .boolean_(b -> b)));

        // Nested metadata object
        properties.put("metadata", Property.of(p -> p
                .object(o -> o.enabled(true))));

        return properties;
    }
}
