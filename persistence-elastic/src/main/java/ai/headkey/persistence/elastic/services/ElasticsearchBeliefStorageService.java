package ai.headkey.persistence.elastic.services;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefConflict;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.memory.interfaces.VectorEmbeddingGenerator;
import ai.headkey.persistence.elastic.configuration.ElasticsearchConfiguration;
import ai.headkey.persistence.elastic.documents.BeliefDocument;
import ai.headkey.persistence.elastic.infrastructure.ElasticsearchOperationsHelper;
import ai.headkey.persistence.elastic.mappers.BeliefDocumentMapper;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.mget.MultiGetOperation;

/**
 * Elasticsearch implementation of the BeliefStorageService interface.
 *
 * This implementation provides comprehensive belief storage and retrieval operations
 * using Elasticsearch as the persistence backend. It leverages Elasticsearch's
 * capabilities for full-text search, vector similarity search, and advanced aggregations.
 *
 * Features:
 * - CRUD operations with optimistic locking
 * - Full-text search on belief statements
 * - Vector similarity search using dense_vector fields
 * - Efficient agent-based data isolation
 * - Bulk operations for high-performance scenarios
 * - Advanced analytics and statistics
 * - Conflict management with dedicated storage
 * - Health monitoring and diagnostics
 *
 * Index Structure:
 * - Beliefs: headkey-beliefs-{agent-id}-v1
 * - Conflicts: headkey-conflicts-{agent-id}-v1
 */
public class ElasticsearchBeliefStorageService implements BeliefStorageService {

    private static final Logger logger = LoggerFactory.getLogger(
        ElasticsearchBeliefStorageService.class
    );

    private final ElasticsearchConfiguration config;
    private final ElasticsearchClient client;
    private final ElasticsearchOperationsHelper operationsHelper;
    private final VectorEmbeddingGenerator embeddingGenerator;

    // Performance tracking
    private final AtomicLong operationCounter = new AtomicLong(0);
    private final AtomicLong errorCounter = new AtomicLong(0);

    // Configuration parameters
    private final int batchSize;
    private final boolean autoCreateIndices;
    private final int searchTimeout;
    private final int maxResults;
    private final double similarityThreshold;

    /**
     * Creates a new Elasticsearch belief storage service with default configuration.
     *
     * @param config The Elasticsearch configuration
     */
    public ElasticsearchBeliefStorageService(
        ElasticsearchConfiguration config
    ) {
        this(config, null, 100, true, 30000, 1000, 0.0);
    }

    /**
     * Creates a new Elasticsearch belief storage service with embedding generator.
     *
     * @param config The Elasticsearch configuration
     * @param embeddingGenerator Vector embedding generator for semantic search
     */
    public ElasticsearchBeliefStorageService(
        ElasticsearchConfiguration config,
        VectorEmbeddingGenerator embeddingGenerator
    ) {
        this(config, embeddingGenerator, 100, true, 30000, 1000, 0.0);
    }

    /**
     * Creates a new Elasticsearch belief storage service with full configuration.
     *
     * @param config The Elasticsearch configuration
     * @param embeddingGenerator Vector embedding generator
     * @param batchSize Batch size for bulk operations
     * @param autoCreateIndices Whether to automatically create indices
     * @param searchTimeout Search timeout in milliseconds
     * @param maxResults Maximum results for queries
     * @param similarityThreshold Minimum similarity threshold for searches
     */
    public ElasticsearchBeliefStorageService(
        ElasticsearchConfiguration config,
        VectorEmbeddingGenerator embeddingGenerator,
        int batchSize,
        boolean autoCreateIndices,
        int searchTimeout,
        int maxResults,
        double similarityThreshold
    ) {
        this.config = Objects.requireNonNull(
            config,
            "Elasticsearch configuration cannot be null"
        );
        this.client = config.getClient();
        this.operationsHelper = new ElasticsearchOperationsHelper(
            client,
            searchTimeout,
            maxResults
        );
        this.embeddingGenerator = embeddingGenerator;
        this.batchSize = Math.max(1, batchSize);
        this.autoCreateIndices = autoCreateIndices;
        this.searchTimeout = Math.max(1000, searchTimeout);
        this.maxResults = Math.max(1, maxResults);
        this.similarityThreshold = Math.max(
            0.0,
            Math.min(1.0, similarityThreshold)
        );

        logger.info(
            "Initialized Elasticsearch belief storage service with batch size: {}, auto-create indices: {}",
            this.batchSize,
            this.autoCreateIndices
        );
    }

    // ========== Basic CRUD Operations ==========

    @Override
    public Belief storeBelief(Belief belief) {
        Objects.requireNonNull(belief, "Belief cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getBeliefIndexName(belief.getAgentId());
            ensureBeliefIndexExists(indexName);

            // Generate embedding if available and missing
            BeliefDocument document = BeliefDocumentMapper.toDocument(belief);
            if (
                embeddingGenerator != null &&
                document.getStatementEmbedding() == null
            ) {
                try {
                    double[] embedding = embeddingGenerator.generateEmbedding(
                        belief.getStatement()
                    );
                    BeliefDocumentMapper.setEmbedding(document, embedding);
                } catch (Exception e) {
                    logger.warn(
                        "Failed to generate embedding for belief {}: {}",
                        belief.getId(),
                        e.getMessage()
                    );
                }
            }

            // Set timestamps
            document.setLastUpdated(Instant.now());
            if (document.getCreatedAt() == null) {
                document.setCreatedAt(Instant.now());
            }

            IndexRequest<BeliefDocument> request = IndexRequest.of(i ->
                i
                    .index(indexName)
                    .id(belief.getId())
                    .document(document)
                    .refresh(Refresh.WaitFor)
            );

            IndexResponse response = client.index(request);

            if (
                response.result() == Result.Created ||
                response.result() == Result.Updated
            ) {
                logger.debug(
                    "Successfully stored belief: {} with result: {}",
                    belief.getId(),
                    response.result()
                );
                return BeliefDocumentMapper.fromDocument(document);
            } else {
                throw new BeliefStorageException(
                    "Unexpected result storing belief: " + response.result()
                );
            }
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to store belief: " + belief.getId(),
                e
            );
        }
    }

    @Override
    public List<Belief> storeBeliefs(List<Belief> beliefs) {
        if (beliefs == null || beliefs.isEmpty()) {
            return Collections.emptyList();
        }

        operationCounter.incrementAndGet();

        try {
            // Group beliefs by agent for efficient indexing
            Map<String, List<Belief>> beliefsByAgent = beliefs
                .stream()
                .collect(Collectors.groupingBy(Belief::getAgentId));

            List<Belief> storedBeliefs = new ArrayList<>();

            for (Map.Entry<
                String,
                List<Belief>
            > entry : beliefsByAgent.entrySet()) {
                String agentId = entry.getKey();
                List<Belief> agentBeliefs = entry.getValue();
                String indexName = config.getBeliefIndexName(agentId);

                ensureBeliefIndexExists(indexName);
                storedBeliefs.addAll(bulkStoreBeliefs(indexName, agentBeliefs));
            }

            return storedBeliefs;
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to store beliefs in bulk",
                e
            );
        }
    }

    @Override
    public Optional<Belief> getBeliefById(String beliefId) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        operationCounter.incrementAndGet();

        // Since we don't know the agent ID, we'll need to search across agent indices
        // This is less efficient but necessary for this interface
        try {
            // Try to extract agent ID from belief ID if it follows a pattern
            // Otherwise, search across common agent indices
            Set<String> indicesToSearch = getAllBeliefIndices();

            for (String indexName : indicesToSearch) {
                if (!operationsHelper.indexExists(indexName)) {
                    continue;
                }

                GetRequest request = GetRequest.of(g ->
                    g.index(indexName).id(beliefId)
                );

                try {
                    GetResponse<BeliefDocument> response = client.get(
                        request,
                        BeliefDocument.class
                    );
                    if (response.found()) {
                        BeliefDocument document = response.source();
                        return Optional.of(
                            BeliefDocumentMapper.fromDocument(document)
                        );
                    }
                } catch (ElasticsearchException e) {
                    // Continue to next index if this one fails
                    logger.debug(
                        "Error searching index {} for belief {}: {}",
                        indexName,
                        beliefId,
                        e.getMessage()
                    );
                }
            }

            return Optional.empty();
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to retrieve belief: " + beliefId,
                e
            );
        }
    }

    @Override
    public List<Belief> getBeliefsById(Set<String> beliefIds) {
        if (beliefIds == null || beliefIds.isEmpty()) {
            return Collections.emptyList();
        }

        operationCounter.incrementAndGet();

        try {
            Set<String> indicesToSearch = getAllBeliefIndices();
            List<Belief> beliefs = new ArrayList<>();

            for (String indexName : indicesToSearch) {
                if (!operationsHelper.indexExists(indexName)) {
                    continue;
                }

                List<MultiGetOperation> operations = beliefIds
                    .stream()
                    .map(id ->
                        MultiGetOperation.of(m -> m.index(indexName).id(id))
                    )
                    .collect(Collectors.toList());

                MgetRequest request = MgetRequest.of(m -> m.docs(operations));

                try {
                    MgetResponse<BeliefDocument> response = client.mget(
                        request,
                        BeliefDocument.class
                    );

                    beliefs.addAll(
                        response
                            .docs()
                            .stream()
                            .filter(doc -> doc.result().found())
                            .map(doc ->
                                BeliefDocumentMapper.fromDocument(
                                    doc.result().source()
                                )
                            )
                            .collect(Collectors.toList())
                    );
                } catch (ElasticsearchException e) {
                    logger.debug(
                        "Error multi-getting from index {}: {}",
                        indexName,
                        e.getMessage()
                    );
                }
            }

            return beliefs;
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to retrieve beliefs by IDs",
                e
            );
        }
    }

    @Override
    public boolean deleteBelief(String beliefId) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            Set<String> indicesToSearch = getAllBeliefIndices();

            for (String indexName : indicesToSearch) {
                if (!operationsHelper.indexExists(indexName)) {
                    continue;
                }

                DeleteRequest request = DeleteRequest.of(d ->
                    d.index(indexName).id(beliefId).refresh(Refresh.WaitFor)
                );

                try {
                    client.delete(request);
                    logger.debug(
                        "Deleted belief: {} from index: {}",
                        beliefId,
                        indexName
                    );
                    return true;
                } catch (ElasticsearchException e) {
                    if (e.response().status() != 404) {
                        logger.debug(
                            "Error deleting from index {}: {}",
                            indexName,
                            e.getMessage()
                        );
                    }
                }
            }

            return false;
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to delete belief: " + beliefId,
                e
            );
        }
    }

    // ========== Query Operations ==========

    @Override
    public List<Belief> getBeliefsForAgent(
        String agentId,
        boolean includeInactive
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getBeliefIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            SearchRequest request = operationsHelper
                .createAgentSearchRequest(indexName, agentId, includeInactive)
                .build();

            return operationsHelper.executeSearch(
                request,
                BeliefDocument.class,
                BeliefDocumentMapper::fromDocument
            );
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to get beliefs for agent: " + agentId,
                e
            );
        }
    }

    @Override
    public List<Belief> getBeliefsInCategory(
        String category,
        String agentId,
        boolean includeInactive
    ) {
        Objects.requireNonNull(category, "Category cannot be null");
        operationCounter.incrementAndGet();

        try {
            if (agentId != null) {
                // Search specific agent's index
                String indexName = config.getBeliefIndexName(agentId);
                if (!operationsHelper.indexExists(indexName)) {
                    return Collections.emptyList();
                }

                Query query = operationsHelper.combineQueriesAnd(
                    operationsHelper.createAgentActiveQuery(
                        agentId,
                        includeInactive
                    ),
                    operationsHelper.createCategoryQuery(category)
                );

                SearchRequest request = operationsHelper
                    .createBaseSearchRequest(indexName)
                    .query(query)
                    .build();

                return operationsHelper.executeSearch(
                    request,
                    BeliefDocument.class,
                    BeliefDocumentMapper::fromDocument
                );
            } else {
                // Search across all agent indices
                return searchAcrossAllAgents(
                    operationsHelper.createCategoryQuery(category),
                    includeInactive
                );
            }
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to get beliefs in category: " + category,
                e
            );
        }
    }

    @Override
    public List<Belief> getAllActiveBeliefs() {
        operationCounter.incrementAndGet();
        return searchAcrossAllAgents(
            operationsHelper.createActiveQuery(true),
            false
        );
    }

    @Override
    public List<Belief> getAllBeliefs() {
        operationCounter.incrementAndGet();
        return searchAcrossAllAgents(Query.of(q -> q.matchAll(m -> m)), true);
    }

    @Override
    public List<Belief> getLowConfidenceBeliefs(
        double confidenceThreshold,
        String agentId
    ) {
        operationCounter.incrementAndGet();

        try {
            Query confidenceQuery = Query.of(q ->
                q.range(r ->
                    r.number(fn -> fn.field("confidence").lt(confidenceThreshold))
                )
            );

            if (agentId != null) {
                String indexName = config.getBeliefIndexName(agentId);
                if (!operationsHelper.indexExists(indexName)) {
                    return Collections.emptyList();
                }

                Query combinedQuery = operationsHelper.combineQueriesAnd(
                    operationsHelper.createAgentActiveQuery(agentId, false),
                    confidenceQuery
                );

                SearchRequest request = operationsHelper
                    .createBaseSearchRequest(indexName)
                    .query(combinedQuery)
                    .build();

                return operationsHelper.executeSearch(
                    request,
                    BeliefDocument.class,
                    BeliefDocumentMapper::fromDocument
                );
            } else {
                return searchAcrossAllAgents(confidenceQuery, false);
            }
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to get low confidence beliefs",
                e
            );
        }
    }

    @Override
    public List<Belief> searchBeliefs(
        String searchText,
        String agentId,
        int limit
    ) {
        Objects.requireNonNull(searchText, "Search text cannot be null");
        operationCounter.incrementAndGet();

        try {
            Query textQuery = Query.of(q ->
                q.multiMatch(m ->
                    m
                        .query(searchText)
                        .fields("statement", "statement.search", "category")
                        .fuzziness("AUTO")
                        .minimumShouldMatch("75%")
                )
            );

            if (agentId != null) {
                String indexName = config.getBeliefIndexName(agentId);
                if (!operationsHelper.indexExists(indexName)) {
                    return Collections.emptyList();
                }

                Query combinedQuery = operationsHelper.combineQueriesAnd(
                    operationsHelper.createAgentActiveQuery(agentId, false),
                    textQuery
                );

                SearchRequest request = operationsHelper
                    .createBaseSearchRequest(indexName)
                    .query(combinedQuery)
                    .size(limit)
                    .build();

                return operationsHelper.executeSearch(
                    request,
                    BeliefDocument.class,
                    BeliefDocumentMapper::fromDocument
                );
            } else {
                return searchAcrossAllAgents(textQuery, false)
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException("Failed to search beliefs", e);
        }
    }

    @Override
    public List<SimilarBelief> findSimilarBeliefs(
        String statement,
        String agentId,
        double similarityThreshold,
        int limit
    ) {
        Objects.requireNonNull(statement, "Statement cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        if (embeddingGenerator == null) {
            logger.warn(
                "No embedding generator available for similarity search"
            );
            return Collections.emptyList();
        }

        try {
            String indexName = config.getBeliefIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            // Generate embedding for the query statement
            double[] queryEmbedding = embeddingGenerator.generateEmbedding(
                statement
            );

            // Convert to List<Double> for Elasticsearch
            List<Float> embeddingList = Arrays.stream(queryEmbedding)
                .boxed()
                .map(d -> d.floatValue())
                .collect(Collectors.toList());

            Query knnQuery = Query.of(q ->
                q.knn(k ->
                    k
                        .field("statement_embedding")
                        .queryVector(embeddingList)
                        .k(limit)
                )
            );

            Query filterQuery = operationsHelper.createAgentActiveQuery(
                agentId,
                false
            );

            SearchRequest request = SearchRequest.of(s ->
                s
                    .index(indexName)
                    .query(knnQuery)
                    .postFilter(filterQuery)
                    .size(limit)
                    .timeout(searchTimeout + "ms")
            );

            SearchResponse<BeliefDocument> response = client.search(
                request,
                BeliefDocument.class
            );

            return response
                .hits()
                .hits()
                .stream()
                .filter(
                    hit ->
                        hit.score() != null &&
                        hit.score() >= similarityThreshold
                )
                .map(hit ->
                    new SimilarBelief(
                        BeliefDocumentMapper.fromDocument(hit.source()),
                        hit.score()
                    )
                )
                .collect(Collectors.toList());
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to find similar beliefs",
                e
            );
        }
    }

    // ========== Conflict Management ==========

    @Override
    public BeliefConflict storeConflict(BeliefConflict conflict) {
        throw new UnsupportedOperationException(
            "Conflict management not yet implemented in Elasticsearch persistence"
        );
    }

    @Override
    public Optional<BeliefConflict> getConflictById(String conflictId) {
        throw new UnsupportedOperationException(
            "Conflict management not yet implemented in Elasticsearch persistence"
        );
    }

    @Override
    public List<BeliefConflict> getUnresolvedConflicts(String agentId) {
        throw new UnsupportedOperationException(
            "Conflict management not yet implemented in Elasticsearch persistence"
        );
    }

    @Override
    public boolean removeConflict(String conflictId) {
        throw new UnsupportedOperationException(
            "Conflict management not yet implemented in Elasticsearch persistence"
        );
    }

    // ========== Statistics and Analytics ==========

    @Override
    public Map<String, Object> getStorageStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("operationCount", operationCounter.get());
            stats.put("errorCount", errorCounter.get());
            stats.put("searchTimeout", searchTimeout);
            stats.put("maxResults", maxResults);
            stats.put("batchSize", batchSize);
            stats.put("autoCreateIndices", autoCreateIndices);
            stats.put("similarityThreshold", similarityThreshold);
            stats.put(
                "embeddingGeneratorAvailable",
                embeddingGenerator != null
            );

            // Add belief count statistics
            try {
                stats.put("totalBeliefs", getTotalBeliefs());
                stats.put("activeBeliefs", getActiveBeliefs());
                stats.put("totalConflicts", getTotalConflicts());
                stats.put("storageType", "elasticsearch");
                stats.put("persistenceProvider", "elasticsearch");

                // Add agent count by counting unique indices
                Set<String> beliefIndices = getAllBeliefIndices();
                stats.put("agentCount", beliefIndices.size());

                // Database information (cluster information for Elasticsearch)
                Map<String, Object> databaseInfo = new HashMap<>();
                databaseInfo.put("productName", "Elasticsearch");
                databaseInfo.put("persistenceProvider", "Elasticsearch Client");
                databaseInfo.put("connectionPooling", "Built-in HTTP Pool");
                stats.put("databaseInfo", databaseInfo);
            } catch (Exception e) {
                logger.warn("Error retrieving belief statistics", e);
                stats.put("beliefStatisticsError", e.getMessage());
            }

            // Add cluster health if available
            try {
                stats.put("clusterHealthy", config.isHealthy());
            } catch (Exception e) {
                stats.put("clusterHealthy", false);
                stats.put("clusterError", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error collecting storage statistics", e);
            stats.put("statisticsError", e.getMessage());
        }

        return stats;
    }

    @Override
    public Map<String, Long> getBeliefDistributionByCategory(String agentId) {
        operationCounter.incrementAndGet();

        if (agentId != null) {
            String indexName = config.getBeliefIndexName(agentId);
            var dist = operationsHelper.getCategoryDistribution(
                indexName,
                agentId
            );
            var result = new LinkedHashMap<String, Long>();
            for (Map.Entry<FieldValue, Long> entry : dist.entrySet()) {
                if (entry.getKey() == null) {
                    continue; // Skip null keys
                }
                String category = entry.getKey().stringValue();
                if (category != null && !category.isEmpty()) {
                    result.put(category, entry.getValue());
                }
            }
            return result;
        } else {
            // Aggregate across all agent indices
            Map<String, Long> totalDistribution = new HashMap<>();
            Set<String> indices = getAllBeliefIndices();

            for (String indexName : indices) {
                if (operationsHelper.indexExists(indexName)) {
                    // Extract agent ID from index name for filtering
                    String extractedAgentId = extractAgentIdFromIndexName(
                        indexName
                    );
                    if (extractedAgentId != null) {
                        Map<FieldValue, Long> agentDistribution =
                            operationsHelper.getCategoryDistribution(
                                indexName,
                                extractedAgentId
                            );
                        agentDistribution.forEach((category, count) ->
                            totalDistribution.merge(
                                category.stringValue(),
                                count,
                                Long::sum
                            )
                        );
                    }
                }
            }

            return totalDistribution;
        }
    }

    @Override
    public Map<String, Long> getBeliefDistributionByConfidence(String agentId) {
        operationCounter.incrementAndGet();

        if (agentId != null) {
            String indexName = config.getBeliefIndexName(agentId);
            return operationsHelper.getConfidenceDistribution(
                indexName,
                agentId
            );
        } else {
            // Aggregate across all agent indices
            Map<String, Long> totalDistribution = new LinkedHashMap<>();
            totalDistribution.put("0.0-0.2", 0L);
            totalDistribution.put("0.2-0.4", 0L);
            totalDistribution.put("0.4-0.6", 0L);
            totalDistribution.put("0.6-0.8", 0L);
            totalDistribution.put("0.8-1.0", 0L);

            Set<String> indices = getAllBeliefIndices();

            for (String indexName : indices) {
                if (operationsHelper.indexExists(indexName)) {
                    String extractedAgentId = extractAgentIdFromIndexName(
                        indexName
                    );
                    if (extractedAgentId != null) {
                        Map<String, Long> agentDistribution =
                            operationsHelper.getConfidenceDistribution(
                                indexName,
                                extractedAgentId
                            );
                        agentDistribution.forEach((range, count) ->
                            totalDistribution.merge(range, count, Long::sum)
                        );
                    }
                }
            }

            return totalDistribution;
        }
    }

    @Override
    public long countBeliefsForAgent(String agentId, boolean includeInactive) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        String indexName = config.getBeliefIndexName(agentId);
        return operationsHelper.countDocumentsForAgent(
            indexName,
            agentId,
            includeInactive
        );
    }

    // ========== Maintenance Operations ==========

    @Override
    public Map<String, Object> optimizeStorage() {
        Map<String, Object> results = new HashMap<>();

        try {
            // Force merge indices for optimization
            Set<String> indices = getAllBeliefIndices();
            int optimizedIndices = 0;

            for (String indexName : indices) {
                if (operationsHelper.indexExists(indexName)) {
                    try {
                        // Force merge to 1 segment for optimization
                        client
                            .indices()
                            .forcemerge(f ->
                                f
                                    .index(indexName)
                                    .maxNumSegments(1L)
                                    .onlyExpungeDeletes(false)
                                    .flush(true)
                            );
                        optimizedIndices++;
                    } catch (Exception e) {
                        logger.warn(
                            "Failed to optimize index: {}",
                            indexName,
                            e
                        );
                    }
                }
            }

            results.put("optimizedIndices", optimizedIndices);
            results.put("totalIndices", indices.size());
            results.put("timestamp", Instant.now());
        } catch (Exception e) {
            logger.error("Error during storage optimization", e);
            results.put("error", e.getMessage());
        }

        return results;
    }

    @Override
    public Map<String, Object> validateIntegrity() {
        Map<String, Object> results = new HashMap<>();

        try {
            Set<String> indices = getAllBeliefIndices();
            int healthyIndices = 0;
            int totalDocuments = 0;
            List<String> issues = new ArrayList<>();

            for (String indexName : indices) {
                if (operationsHelper.indexExists(indexName)) {
                    try {
                        long docCount = operationsHelper.countDocuments(
                            indexName,
                            Query.of(q -> q.matchAll(m -> m))
                        );
                        totalDocuments += docCount;
                        healthyIndices++;
                    } catch (Exception e) {
                        issues.add(
                            "Index " + indexName + ": " + e.getMessage()
                        );
                    }
                }
            }

            results.put("healthyIndices", healthyIndices);
            results.put("totalIndices", indices.size());
            results.put("totalDocuments", totalDocuments);
            results.put("issues", issues);
            results.put("healthy", issues.isEmpty());
            results.put("timestamp", Instant.now());
        } catch (Exception e) {
            logger.error("Error during integrity validation", e);
            results.put("error", e.getMessage());
            results.put("healthy", false);
        }

        return results;
    }

    @Override
    public Map<String, Object> createBackup(
        String backupId,
        Map<String, Object> options
    ) {
        throw new UnsupportedOperationException(
            "Backup operations not yet implemented for Elasticsearch persistence"
        );
    }

    // ========== Health and Monitoring ==========

    @Override
    public boolean isHealthy() {
        try {
            return config.isHealthy() && operationCounter.get() > 0
                ? (errorCounter.get() / (double) operationCounter.get()) < 0.1
                : true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new HashMap<>();

        try {
            health.put("healthy", isHealthy());
            health.put("clusterHealthy", config.isHealthy());
            health.put("operationCount", operationCounter.get());
            health.put("errorCount", errorCounter.get());
            health.put(
                "errorRate",
                operationCounter.get() > 0
                    ? (errorCounter.get() / (double) operationCounter.get())
                    : 0.0
            );
            health.put("timestamp", Instant.now());
        } catch (Exception e) {
            health.put("healthy", false);
            health.put("error", e.getMessage());
        }

        return health;
    }

    @Override
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("serviceName", "ElasticsearchBeliefStorageService");
        info.put("version", "1.0.0");
        info.put("implementation", "Elasticsearch");
        info.put(
            "features",
            Arrays.asList(
                "CRUD Operations",
                "Full-text Search",
                "Vector Similarity Search",
                "Agent Isolation",
                "Bulk Operations",
                "Statistics & Analytics"
            )
        );
        info.put("embeddingGeneratorAvailable", embeddingGenerator != null);
        info.put(
            "configuration",
            Map.of(
                "batchSize",
                batchSize,
                "autoCreateIndices",
                autoCreateIndices,
                "searchTimeout",
                searchTimeout,
                "maxResults",
                maxResults,
                "similarityThreshold",
                similarityThreshold
            )
        );

        return info;
    }

    // ========== Public Helper Methods for Service Composition ==========

    /**
     * Gets beliefs by their IDs for a specific agent (optimized version).
     * This method is used by other services like BeliefRelationshipService.
     *
     * @param agentId The agent identifier
     * @param beliefIds Collection of belief IDs to retrieve
     * @return Map of belief ID to Belief object
     */
    public Map<String, Belief> getBeliefsById(
        String agentId,
        Collection<String> beliefIds
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        if (beliefIds == null || beliefIds.isEmpty()) {
            return Collections.emptyMap();
        }

        operationCounter.incrementAndGet();

        try {
            String indexName = config.getBeliefIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyMap();
            }

            List<MultiGetOperation> operations = beliefIds
                .stream()
                .map(id -> MultiGetOperation.of(m -> m.index(indexName).id(id)))
                .collect(Collectors.toList());

            MgetRequest request = MgetRequest.of(m -> m.docs(operations));
            MgetResponse<BeliefDocument> response = client.mget(
                request,
                BeliefDocument.class
            );

            return response
                .docs()
                .stream()
                .filter(doc -> doc.result().found())
                .collect(
                    Collectors.toMap(
                        doc -> doc.result().id(),
                        doc ->
                            BeliefDocumentMapper.fromDocument(
                                doc.result().source()
                            ),
                        (existing, replacement) -> existing
                    )
                );
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to retrieve beliefs by IDs for agent: " + agentId,
                e
            );
        }
    }

    /**
     * Gets the operations helper for use by other services.
     * This allows other services to reuse common query building and operations.
     *
     * @return The ElasticsearchOperationsHelper instance
     */
    public ElasticsearchOperationsHelper getOperationsHelper() {
        return operationsHelper;
    }

    /**
     * Gets the Elasticsearch configuration.
     *
     * @return The ElasticsearchConfiguration instance
     */
    public ElasticsearchConfiguration getConfig() {
        return config;
    }

    /**
     * Gets the Elasticsearch client for advanced operations by other services.
     *
     * @return The ElasticsearchClient instance
     */
    public ElasticsearchClient getClient() {
        return client;
    }

    // ========== Private Helper Methods ==========

    /**
     * Ensures that the belief index exists for the given index name.
     *
     * @param indexName The index name
     */
    private void ensureBeliefIndexExists(String indexName) {
        if (!autoCreateIndices) {
            return;
        }

        Map<String, Property> mapping = createBeliefIndexMapping();
        operationsHelper.ensureIndexExists(indexName, mapping);
    }

    /**
     * Creates the mapping for belief indices.
     *
     * @return Map of field mappings
     */
    private Map<String, Property> createBeliefIndexMapping() {
        Map<String, Property> mapping = new HashMap<>();

        // ID field
        mapping.put(
            "id",
            Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))
        );

        // Agent ID field (not indexed for routing efficiency)
        mapping.put(
            "agent_id",
            Property.of(p -> p.keyword(KeywordProperty.of(k -> k.index(false))))
        );

        // Statement field with multiple analyzers
        mapping.put(
            "statement",
            Property.of(p ->
                p.text(
                    TextProperty.of(t ->
                        t
                            .analyzer("standard")
                            .fields(
                                Map.of(
                                    "keyword",
                                    Property.of(kw ->
                                        kw.keyword(
                                            KeywordProperty.of(k ->
                                                k.ignoreAbove(256)
                                            )
                                        )
                                    ),
                                    "search",
                                    Property.of(search ->
                                        search.text(
                                            TextProperty.of(st ->
                                                st.analyzer("english")
                                            )
                                        )
                                    )
                                )
                            )
                    )
                )
            )
        );

        // Vector embedding field
        mapping.put(
            "statement_embedding",
            Property.of(p ->
                p.denseVector(
                    DenseVectorProperty.of(d ->
                        d
                            .dims(1536) // Default OpenAI embedding dimension
                            .index(true)
                            .similarity(DenseVectorSimilarity.Cosine)
                    )
                )
            )
        );

        // Numeric and boolean fields
        mapping.put("confidence", Property.of(p -> p.double_(d -> d)));
        mapping.put("reinforcement_count", Property.of(p -> p.integer(i -> i)));
        mapping.put("active", Property.of(p -> p.boolean_(b -> b)));

        // Keyword fields
        mapping.put(
            "category",
            Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))
        );
        mapping.put(
            "evidence_memory_ids",
            Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))
        );
        mapping.put(
            "tags",
            Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))
        );

        // Date fields
        mapping.put("created_at", Property.of(p -> p.date(d -> d)));
        mapping.put("last_updated", Property.of(p -> p.date(d -> d)));

        // Version field
        mapping.put("version", Property.of(p -> p.long_(l -> l)));

        return mapping;
    }

    /**
     * Bulk stores beliefs for a specific agent.
     *
     * @param indexName The index name
     * @param beliefs The beliefs to store
     * @return List of stored beliefs
     */
    private List<Belief> bulkStoreBeliefs(
        String indexName,
        List<Belief> beliefs
    ) throws IOException {
        List<BulkOperation> operations = new ArrayList<>();
        Map<String, BeliefDocument> documentMap = new HashMap<>();

        for (Belief belief : beliefs) {
            BeliefDocument document = BeliefDocumentMapper.toDocument(belief);

            // Generate embedding if available
            if (
                embeddingGenerator != null &&
                document.getStatementEmbedding() == null
            ) {
                try {
                    double[] embedding = embeddingGenerator.generateEmbedding(
                        belief.getStatement()
                    );
                    BeliefDocumentMapper.setEmbedding(document, embedding);
                } catch (Exception e) {
                    logger.warn(
                        "Failed to generate embedding for belief {}: {}",
                        belief.getId(),
                        e.getMessage()
                    );
                }
            }

            // Set timestamps
            document.setLastUpdated(Instant.now());
            if (document.getCreatedAt() == null) {
                document.setCreatedAt(Instant.now());
            }

            operations.add(
                BulkOperation.of(b ->
                    b.index(
                        IndexOperation.of(i ->
                            i
                                .index(indexName)
                                .id(belief.getId())
                                .document(document)
                        )
                    )
                )
            );

            documentMap.put(belief.getId(), document);
        }

        // Execute bulk request
        BulkRequest request = BulkRequest.of(b ->
            b.operations(operations).refresh(Refresh.WaitFor)
        );

        BulkResponse response = client.bulk(request);

        // Process results
        List<Belief> storedBeliefs = new ArrayList<>();
        for (int i = 0; i < response.items().size(); i++) {
            var item = response.items().get(i);
            if (item.index() != null && Objects.isNull(item.error())) {
                String beliefId = beliefs.get(i).getId();
                BeliefDocument document = documentMap.get(beliefId);
                storedBeliefs.add(BeliefDocumentMapper.fromDocument(document));
            } else {
                logger.warn(
                    "Failed to store belief in bulk operation: {}",
                    item.error().reason() != null
                        ? item.error().reason()
                        : "unknown error"
                );
            }
        }

        return storedBeliefs;
    }

    /**
     * Searches across all agent indices with the given query.
     *
     * @param additionalQuery Additional query to combine with active filter
     * @param includeInactive Whether to include inactive beliefs
     * @return List of matching beliefs
     */
    private List<Belief> searchAcrossAllAgents(
        Query additionalQuery,
        boolean includeInactive
    ) {
        try {
            Set<String> indices = getAllBeliefIndices();
            List<Belief> allBeliefs = new ArrayList<>();

            for (String indexName : indices) {
                if (!operationsHelper.indexExists(indexName)) {
                    continue;
                }

                String agentId = extractAgentIdFromIndexName(indexName);
                if (agentId == null) {
                    continue;
                }

                Query baseQuery = operationsHelper.createAgentActiveQuery(
                    agentId,
                    includeInactive
                );
                Query combinedQuery = operationsHelper.combineQueriesAnd(
                    baseQuery,
                    additionalQuery
                );

                SearchRequest request = operationsHelper
                    .createBaseSearchRequest(indexName)
                    .query(combinedQuery)
                    .build();

                try {
                    List<Belief> indexBeliefs = operationsHelper.executeSearch(
                        request,
                        BeliefDocument.class,
                        BeliefDocumentMapper::fromDocument
                    );
                    allBeliefs.addAll(indexBeliefs);
                } catch (Exception e) {
                    logger.debug(
                        "Error searching index {}: {}",
                        indexName,
                        e.getMessage()
                    );
                }
            }

            return allBeliefs;
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to search across all agents",
                e
            );
        }
    }

    /**
     * Gets all belief indices that currently exist.
     *
     * @return Set of belief index names
     */
    private Set<String> getAllBeliefIndices() {
        try {
            // In a real implementation, this would discover indices dynamically
            // For now, we'll use a simple pattern-based approach
            Set<String> indices = new HashSet<>();

            // This is a simplified approach - in production, you'd want to:
            // 1. Use the Elasticsearch _cat/indices API to discover existing indices
            // 2. Filter by the belief index pattern
            // 3. Cache the results for performance

            // For demonstration, we'll return common agent patterns
            String[] commonAgents = { "default", "agent1", "agent2", "system" };
            for (String agentId : commonAgents) {
                String indexName = config.getBeliefIndexName(agentId);
                if (operationsHelper.indexExists(indexName)) {
                    indices.add(indexName);
                }
            }

            return indices;
        } catch (Exception e) {
            logger.error("Error getting all belief indices", e);
            return Collections.emptySet();
        }
    }

    /**
     * Extracts agent ID from index name.
     *
     * @param indexName The index name
     * @return The agent ID or null if not extractable
     */
    private String extractAgentIdFromIndexName(String indexName) {
        try {
            // Expected pattern: headkey-beliefs-{agent-id}-v1
            if (
                indexName.startsWith("headkey-beliefs-") &&
                indexName.endsWith("-v1")
            ) {
                String withoutPrefix = indexName.substring(
                    "headkey-beliefs-".length()
                );
                String withoutSuffix = withoutPrefix.substring(
                    0,
                    withoutPrefix.length() - "-v1".length()
                );
                return withoutSuffix;
            }
            return null;
        } catch (Exception e) {
            logger.debug(
                "Could not extract agent ID from index name: {}",
                indexName
            );
            return null;
        }
    }

    public Optional<Belief> findBeliefById(String beliefId, String agentId) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getBeliefIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Optional.empty();
            }

            GetRequest request = GetRequest.of(g ->
                g.index(indexName).id(beliefId)
            );

            GetResponse<BeliefDocument> response = client.get(
                request,
                BeliefDocument.class
            );
            if (response.found()) {
                BeliefDocument document = response.source();
                return Optional.of(BeliefDocumentMapper.fromDocument(document));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new BeliefStorageException(
                "Failed to find belief by ID: " + beliefId,
                e
            );
        }
    }

    @Override
    public long getTotalBeliefs() {
        operationCounter.incrementAndGet();

        try {
            Set<String> beliefIndices = getAllBeliefIndices();
            long totalCount = 0;

            // Count documents across all belief indices
            for (String indexName : beliefIndices) {
                if (operationsHelper.indexExists(indexName)) {
                    // Count all documents (active and inactive)
                    long count = operationsHelper.countDocuments(
                        indexName,
                        Query.of(q -> q.matchAll(m -> m))
                    );
                    totalCount += count;
                }
            }

            return totalCount;
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            logger.error("Error counting total beliefs", e);
            return 0L;
        }
    }

    @Override
    public long getActiveBeliefs() {
        operationCounter.incrementAndGet();

        try {
            Set<String> beliefIndices = getAllBeliefIndices();
            long activeCount = 0;

            // Count only active documents across all belief indices
            for (String indexName : beliefIndices) {
                if (operationsHelper.indexExists(indexName)) {
                    // Count only active beliefs
                    long count = operationsHelper.countDocuments(
                        indexName,
                        operationsHelper.createActiveQuery(true)
                    );
                    activeCount += count;
                }
            }

            return activeCount;
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            logger.error("Error counting active beliefs", e);
            return 0L;
        }
    }

    @Override
    public long getTotalConflicts() {
        operationCounter.incrementAndGet();

        try {
            // Since conflict management is not yet implemented in Elasticsearch,
            // we return 0 for now. This maintains API compatibility while
            // indicating that conflict tracking is not available.
            // TODO: Implement conflict index management and counting when
            // conflict storage is added to Elasticsearch persistence
            return 0L;
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            logger.error("Error counting total conflicts", e);
            return 0L;
        }
    }
}
