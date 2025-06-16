package ai.headkey.persistence.elastic.infrastructure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shared utility class for common Elasticsearch operations across persistence services.
 *
 * This helper centralizes frequently used Elasticsearch operations to reduce code
 * duplication between BeliefStorageService, BeliefGraphQueryService, and
 * BeliefRelationshipService implementations.
 *
 * Features:
 * - Common query builders for agent filtering and active status
 * - Shared index management operations with caching
 * - Reusable search request configuration
 * - Centralized aggregation operations
 * - Error handling and logging utilities
 * - Performance optimized operations
 */
public class ElasticsearchOperationsHelper {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchOperationsHelper.class);

    private final ElasticsearchClient client;
    private final Map<String, Boolean> indexExistsCache;
    private final int searchTimeout;
    private final int maxResults;

    /**
     * Creates a new operations helper with the specified client and configuration.
     *
     * @param client The Elasticsearch client
     * @param searchTimeout Search timeout in milliseconds
     * @param maxResults Maximum results for queries
     */
    public ElasticsearchOperationsHelper(ElasticsearchClient client, int searchTimeout, int maxResults) {
        this.client = Objects.requireNonNull(client, "Elasticsearch client cannot be null");
        this.indexExistsCache = new ConcurrentHashMap<>();
        this.searchTimeout = Math.max(1000, searchTimeout);
        this.maxResults = Math.max(1, maxResults);
    }

    /**
     * Creates a new operations helper with default configuration.
     *
     * @param client The Elasticsearch client
     */
    public ElasticsearchOperationsHelper(ElasticsearchClient client) {
        this(client, 30000, 10000);
    }

    // ========== Query Builders ==========

    /**
     * Creates a query to filter documents by agent ID.
     *
     * @param agentId The agent identifier
     * @return Query for agent filtering
     */
    public Query createAgentQuery(String agentId) {
        return Query.of(q -> q.term(t -> t.field("agent_id").value(agentId)));
    }

    /**
     * Creates a query to filter active documents.
     *
     * @param activeOnly Whether to include only active documents
     * @return Query for active status filtering
     */
    public Query createActiveQuery(boolean activeOnly) {
        if (!activeOnly) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        return Query.of(q -> q.term(t -> t.field("active").value(true)));
    }

    /**
     * Creates a combined query for agent and active status filtering.
     *
     * @param agentId The agent identifier
     * @param includeInactive Whether to include inactive documents
     * @return Combined query
     */
    public Query createAgentActiveQuery(String agentId, boolean includeInactive) {
        if (includeInactive) {
            return createAgentQuery(agentId);
        }

        return Query.of(q -> q.bool(BoolQuery.of(b -> b
            .must(createAgentQuery(agentId))
            .must(createActiveQuery(true))
        )));
    }

    /**
     * Creates a confidence range query.
     *
     * @param minConfidence Minimum confidence value
     * @param maxConfidence Maximum confidence value
     * @return Range query for confidence filtering
     */
    public Query createConfidenceRangeQuery(double minConfidence, double maxConfidence) {
        return Query.of(q -> q.range(r -> r
            .field("confidence")
            .gte(JsonData.of(minConfidence))
            .lte(JsonData.of(maxConfidence))
        ));
    }

    /**
     * Creates a category filter query.
     *
     * @param category The category to filter by
     * @return Query for category filtering
     */
    public Query createCategoryQuery(String category) {
        return Query.of(q -> q.term(t -> t.field("category").value(category)));
    }

    /**
     * Combines multiple queries using boolean AND logic.
     *
     * @param queries Queries to combine
     * @return Combined boolean query
     */
    public Query combineQueriesAnd(Query... queries) {
        if (queries.length == 0) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        if (queries.length == 1) {
            return queries[0];
        }

        return Query.of(q -> q.bool(BoolQuery.of(b -> {
            for (Query query : queries) {
                b.must(query);
            }
            return b;
        })));
    }

    /**
     * Combines multiple queries using boolean OR logic.
     *
     * @param queries Queries to combine
     * @return Combined boolean query
     */
    public Query combineQueriesOr(Query... queries) {
        if (queries.length == 0) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        if (queries.length == 1) {
            return queries[0];
        }

        return Query.of(q -> q.bool(BoolQuery.of(b -> {
            for (Query query : queries) {
                b.should(query);
            }
            return b.minimumShouldMatch("1");
        })));
    }

    // ========== Search Request Builders ==========

    /**
     * Creates a base search request with common configuration.
     *
     * @param indexName The index to search
     * @return Configured search request builder
     */
    public SearchRequest.Builder createBaseSearchRequest(String indexName) {
        return new SearchRequest.Builder()
            .index(indexName)
            .size(maxResults)
            .timeout(searchTimeout + "ms")
            .trackTotalHits(t -> t.enabled(true)) // Always track total hits for accurate counts
            .sort(s -> s.field(f -> f.field("_score").order(SortOrder.Desc))); // Default sort by relevance
    }

    /**
     * Creates a search request with agent filtering.
     *
     * @param indexName The index to search
     * @param agentId The agent identifier
     * @param includeInactive Whether to include inactive documents
     * @return Configured search request builder
     */
    public SearchRequest.Builder createAgentSearchRequest(String indexName, String agentId, boolean includeInactive) {
        return createBaseSearchRequest(indexName)
            .query(createAgentActiveQuery(agentId, includeInactive));
    }

    // ========== Index Management ==========

    /**
     * Checks if an index exists, using cache for performance.
     *
     * @param indexName The index name to check
     * @return true if the index exists
     */
    public boolean indexExists(String indexName) {
        return indexExistsCache.computeIfAbsent(indexName, this::checkIndexExists);
    }

    /**
     * Ensures an index exists, creating it if necessary.
     *
     * @param indexName The index name
     * @param mapping The field mappings for the index
     * @return true if the index was created or already exists
     */
    public boolean ensureIndexExists(String indexName, Map<String, Property> mapping) {
        if (indexExists(indexName)) {
            return true;
        }

        try {
            CreateIndexRequest request = CreateIndexRequest.of(c -> c
                .index(indexName)
                .mappings(m -> m.properties(mapping))
                .settings(s -> s
                    .numberOfShards("1")
                    .numberOfReplicas("1")
                    .refreshInterval(Time.of(t -> t.time("1s")))
                )
            );

            client.indices().create(request);
            indexExistsCache.put(indexName, true);
            logger.info("Created index: {}", indexName);
            return true;

        } catch (ElasticsearchException e) {
            if (e.response().status() == 400 && e.getMessage().contains("already exists")) {
                // Index was created by another thread/process
                indexExistsCache.put(indexName, true);
                return true;
            }
            logger.error("Failed to create index: {}", indexName, e);
            return false;
        } catch (IOException e) {
            logger.error("I/O error creating index: {}", indexName, e);
            return false;
        }
    }

    /**
     * Invalidates the index existence cache for a specific index.
     *
     * @param indexName The index name to invalidate
     */
    public void invalidateIndexCache(String indexName) {
        indexExistsCache.remove(indexName);
    }

    /**
     * Clears the entire index existence cache.
     */
    public void clearIndexCache() {
        indexExistsCache.clear();
    }

    // ========== Count Operations ==========

    /**
     * Counts documents matching a query in an index.
     *
     * @param indexName The index to count in
     * @param query The query to match documents
     * @return The count of matching documents
     */
    public long countDocuments(String indexName, Query query) {
        if (!indexExists(indexName)) {
            return 0L;
        }

        try {
            CountRequest request = CountRequest.of(c -> c
                .index(indexName)
                .query(query)
            );

            CountResponse response = client.count(request);
            return response.count();

        } catch (IOException e) {
            logger.error("Error counting documents in index: {}", indexName, e);
            return 0L;
        }
    }

    /**
     * Counts documents for a specific agent.
     *
     * @param indexName The index to count in
     * @param agentId The agent identifier
     * @param includeInactive Whether to include inactive documents
     * @return The count of matching documents
     */
    public long countDocumentsForAgent(String indexName, String agentId, boolean includeInactive) {
        return countDocuments(indexName, createAgentActiveQuery(agentId, includeInactive));
    }

    // ========== Aggregation Operations ==========

    /**
     * Executes a terms aggregation on a field and returns the bucket counts.
     *
     * @param indexName The index to aggregate on
     * @param field The field to aggregate
     * @param query Filter query for the aggregation
     * @param aggName The aggregation name
     * @return Map of term values to document counts
     */
    public Map<FieldValue, Long> executeTermsAggregation(String indexName, String field, Query query, String aggName) {
        if (!indexExists(indexName)) {
            return Collections.emptyMap();
        }

        try {
            SearchRequest request = SearchRequest.of(s -> s
                .index(indexName)
                .size(0) // We only want aggregation results
                .query(query)
                .aggregations(aggName, Aggregation.of(a -> a
                    .terms(t -> t.field(field).size(1000))
                ))
            );

            SearchResponse<Void> response = client.search(request, Void.class);

            if (response.aggregations() != null && response.aggregations().containsKey(aggName)) {
                StringTermsAggregate termsAgg = response.aggregations().get(aggName).sterms();

                return termsAgg.buckets().array().stream()
                    .collect(Collectors.toMap(
                        StringTermsBucket::key,
                        StringTermsBucket::docCount,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                    ));
            }

            return Collections.emptyMap();

        } catch (IOException e) {
            logger.error("Error executing terms aggregation on index: {}", indexName, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Gets distribution of documents by category for an agent.
     *
     * @param indexName The index to aggregate on
     * @param agentId The agent identifier
     * @return Map of category names to document counts
     */
    public Map<FieldValue, Long> getCategoryDistribution(String indexName, String agentId) {
        Query query = createAgentActiveQuery(agentId, false); // Only active documents
        return executeTermsAggregation(indexName, "category", query, "category_distribution");
    }

    /**
     * Gets distribution of documents by confidence ranges.
     *
     * @param indexName The index to aggregate on
     * @param agentId The agent identifier
     * @return Map of confidence ranges to document counts
     */
    public Map<String, Long> getConfidenceDistribution(String indexName, String agentId) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        Query baseQuery = createAgentActiveQuery(agentId, false);

        // Define confidence ranges
        String[] ranges = {"0.0-0.2", "0.2-0.4", "0.4-0.6", "0.6-0.8", "0.8-1.0"};
        double[][] rangeBounds = {{0.0, 0.2}, {0.2, 0.4}, {0.4, 0.6}, {0.6, 0.8}, {0.8, 1.0}};

        for (int i = 0; i < ranges.length; i++) {
            Query rangeQuery = combineQueriesAnd(
                baseQuery,
                createConfidenceRangeQuery(rangeBounds[i][0], rangeBounds[i][1])
            );

            long count = countDocuments(indexName, rangeQuery);
            distribution.put(ranges[i], count);
        }

        return distribution;
    }

    // ========== Search Operations ==========

    /**
     * Executes a search request and maps the results using the provided mapper function.
     *
     * @param request The search request
     * @param documentClass The document class for deserialization
     * @param mapper Function to map document to target type
     * @param <T> The document type
     * @param <R> The result type
     * @return List of mapped results
     */
    public <T, R> List<R> executeSearch(SearchRequest request, Class<T> documentClass, Function<T, R> mapper) {
        try {
            SearchResponse<T> response = client.search(request, documentClass);

            return response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(mapper)
                .collect(Collectors.toList());

        } catch (IOException e) {
            logger.error("Error executing search request", e);
            return Collections.emptyList();
        }
    }

    /**
     * Executes a simple search and returns the raw documents.
     *
     * @param request The search request
     * @param documentClass The document class for deserialization
     * @param <T> The document type
     * @return List of documents
     */
    public <T> List<T> executeSimpleSearch(SearchRequest request, Class<T> documentClass) {
        return executeSearch(request, documentClass, Function.identity());
    }

    // ========== Private Helper Methods ==========

    /**
     * Actually checks if an index exists by calling Elasticsearch.
     *
     * @param indexName The index name to check
     * @return true if the index exists
     */
    private boolean checkIndexExists(String indexName) {
        try {
            ExistsRequest request = ExistsRequest.of(e -> e.index(indexName));
            return client.indices().exists(request).value();
        } catch (IOException e) {
            logger.error("Error checking if index exists: {}", indexName, e);
            return false;
        }
    }

    // ========== Getters for Configuration ==========

    /**
     * Gets the search timeout configuration.
     *
     * @return Search timeout in milliseconds
     */
    public int getSearchTimeout() {
        return searchTimeout;
    }

    /**
     * Gets the maximum results configuration.
     *
     * @return Maximum results for queries
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Gets the Elasticsearch client.
     *
     * @return The Elasticsearch client
     */
    public ElasticsearchClient getClient() {
        return client;
    }
}
