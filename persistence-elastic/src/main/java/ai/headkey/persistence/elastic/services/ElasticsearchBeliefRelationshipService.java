package ai.headkey.persistence.elastic.services;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefKnowledgeGraph;
import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.enums.RelationshipType;
import ai.headkey.memory.exceptions.StorageException;
import ai.headkey.memory.interfaces.BeliefRelationshipService;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.persistence.elastic.configuration.ElasticsearchConfiguration;
import ai.headkey.persistence.elastic.documents.BeliefDocument;
import ai.headkey.persistence.elastic.documents.BeliefRelationshipDocument;
import ai.headkey.persistence.elastic.infrastructure.ElasticsearchOperationsHelper;
import ai.headkey.persistence.elastic.mappers.BeliefRelationshipDocumentMapper;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.DoubleNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.json.JsonData;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refactored Elasticsearch implementation of the BeliefRelationshipService interface.
 *
 * This implementation leverages the ElasticsearchBeliefStorageService to reduce code
 * duplication and improve service composition. It focuses on relationship-specific
 * operations while delegating belief storage and retrieval to the storage service.
 *
 * Features:
 * - Belief validation using storage service
 * - Shared infrastructure through operations helper
 * - Reduced code duplication
 * - Composition-based architecture
 * - Temporal relationship support
 * - Comprehensive relationship analytics
 *
 * The service composes with:
 * - ElasticsearchBeliefStorageService: For belief operations and validation
 * - ElasticsearchOperationsHelper: For shared query operations
 */
public class ElasticsearchBeliefRelationshipService
    implements BeliefRelationshipService {

    private static final Logger logger = LoggerFactory.getLogger(
        ElasticsearchBeliefRelationshipService.class
    );

    private final BeliefStorageService beliefStorageService;
    private final ElasticsearchConfiguration config;
    private final ElasticsearchClient client;
    private final ElasticsearchOperationsHelper operationsHelper;

    // Performance tracking
    private final AtomicLong operationCounter = new AtomicLong(0);
    private final AtomicLong errorCounter = new AtomicLong(0);

    // Configuration parameters
    private final int batchSize;
    private final boolean autoCreateIndices;
    private final int searchTimeout;
    private final int maxResults;

    /**
     * Creates a new refactored Elasticsearch belief relationship service.
     *
     * @param beliefStorageService The belief storage service to compose with
     */
    public ElasticsearchBeliefRelationshipService(
        ElasticsearchBeliefStorageService beliefStorageService
    ) {
        this(beliefStorageService, 100, true, 30000, 10000);
    }

    /**
     * Creates a new refactored Elasticsearch belief relationship service with configuration.
     *
     * @param beliefStorageService The belief storage service to compose with
     * @param batchSize Batch size for bulk operations
     * @param autoCreateIndices Whether to automatically create indices
     * @param searchTimeout Search timeout in milliseconds
     * @param maxResults Maximum results for queries
     */
    public ElasticsearchBeliefRelationshipService(
        ElasticsearchBeliefStorageService beliefStorageService,
        int batchSize,
        boolean autoCreateIndices,
        int searchTimeout,
        int maxResults
    ) {
        this.beliefStorageService = Objects.requireNonNull(
            beliefStorageService,
            "Belief storage service cannot be null"
        );
        this.config = beliefStorageService.getConfig();
        this.client = beliefStorageService.getClient();
        this.operationsHelper = beliefStorageService.getOperationsHelper();
        this.batchSize = Math.max(1, batchSize);
        this.autoCreateIndices = autoCreateIndices;
        this.searchTimeout = Math.max(1000, searchTimeout);
        this.maxResults = Math.max(1, maxResults);

        logger.info(
            "Initialized refactored Elasticsearch belief relationship service with batch size: {}, auto-create indices: {}",
            this.batchSize,
            this.autoCreateIndices
        );
    }

    @Override
    public BeliefRelationship createRelationship(
        String sourceBeliefId,
        String targetBeliefId,
        RelationshipType relationshipType,
        double strength,
        String agentId
    ) {
        return createRelationshipWithMetadata(
            sourceBeliefId,
            targetBeliefId,
            relationshipType,
            strength,
            agentId,
            null
        );
    }

    @Override
    public BeliefRelationship createRelationshipWithMetadata(
        String sourceBeliefId,
        String targetBeliefId,
        RelationshipType relationshipType,
        double strength,
        String agentId,
        Map<String, Object> metadata
    ) {
        Objects.requireNonNull(
            sourceBeliefId,
            "Source belief ID cannot be null"
        );
        Objects.requireNonNull(
            targetBeliefId,
            "Target belief ID cannot be null"
        );
        Objects.requireNonNull(
            relationshipType,
            "Relationship type cannot be null"
        );
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            // Validate beliefs exist using storage service instead of duplicating logic
            List<Belief> beliefs = beliefStorageService.getBeliefsById(
                new HashSet<String>(Arrays.asList(sourceBeliefId, targetBeliefId))
            );

            if (!beliefs.contains(sourceBeliefId)) {
                throw new StorageException(
                    "Source belief not found: " + sourceBeliefId
                );
            }
            if (!beliefs.contains(targetBeliefId)) {
                throw new StorageException(
                    "Target belief not found: " + targetBeliefId
                );
            }

            // Generate unique ID
            String relationshipId = generateRelationshipId();

            // Create relationship
            BeliefRelationship relationship = new BeliefRelationship(
                sourceBeliefId,
                targetBeliefId,
                relationshipType,
                strength,
                agentId
            );
            relationship.setId(relationshipId);
            relationship.setCreatedAt(Instant.now());
            relationship.setActive(true);

            if (metadata != null) {
                relationship.setMetadata(metadata);
            }

            // Convert to document
            BeliefRelationshipDocument document =
                BeliefRelationshipDocumentMapper.toDocument(relationship);

            // Store in Elasticsearch
            String indexName = config.getRelationshipIndexName(agentId);
            ensureRelationshipIndexExists(indexName);

            IndexRequest<BeliefRelationshipDocument> request = IndexRequest.of(
                i ->
                    i
                        .index(indexName)
                        .id(relationshipId)
                        .document(document)
                        .refresh(Refresh.WaitFor)
            );

            IndexResponse response = client.index(request);

            if (
                response.result() == Result.Created ||
                response.result() == Result.Updated
            ) {
                logger.debug(
                    "Successfully created relationship {} between beliefs {} and {} for agent {}",
                    relationshipId,
                    sourceBeliefId,
                    targetBeliefId,
                    agentId
                );
                return relationship;
            } else {
                throw new StorageException(
                    "Failed to create relationship: unexpected result " +
                    response.result()
                );
            }
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to create relationship between beliefs " +
                sourceBeliefId +
                " and " +
                targetBeliefId,
                e
            );
        }
    }

    @Override
    public List<BeliefRelationship> findRelationshipsForBelief(
        String beliefId,
        String agentId
    ) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            // Validate belief exists using storage service
            if (!beliefStorageService.getBeliefById(beliefId).isPresent()) {
                return Collections.emptyList();
            }

            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            // Create query for relationships involving this belief
            Query sourceQuery = Query.of(q ->
                q.term(t -> t.field("source_belief_id").value(beliefId))
            );
            Query targetQuery = Query.of(q ->
                q.term(t -> t.field("target_belief_id").value(beliefId))
            );
            Query beliefQuery = operationsHelper.combineQueriesOr(
                sourceQuery,
                targetQuery
            );

            Query agentQuery = operationsHelper.createAgentQuery(agentId);
            Query activeQuery = operationsHelper.createActiveQuery(true);
            Query combinedQuery = operationsHelper.combineQueriesAnd(
                agentQuery,
                activeQuery,
                beliefQuery
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(combinedQuery)
                .sort(s ->
                    s.field(f -> f.field("created_at").order(SortOrder.Desc))
                )
                .build();

            return operationsHelper.executeSearch(
                request,
                BeliefRelationshipDocument.class,
                BeliefRelationshipDocumentMapper::fromDocument
            );
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to get relationships for belief: " + beliefId,
                e
            );
        }
    }

    @Override
    public List<BeliefRelationship> findRelationshipsByType(
        RelationshipType relationshipType,
        String agentId
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        Objects.requireNonNull(
            relationshipType,
            "Relationship type cannot be null"
        );
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            Query typeQuery = Query.of(q ->
                q.term(t ->
                    t
                        .field("relationship_type")
                        .value(relationshipType.getCode())
                )
            );
            Query agentQuery = operationsHelper.createAgentQuery(agentId);
            Query activeQuery = operationsHelper.createActiveQuery(true);
            Query combinedQuery = operationsHelper.combineQueriesAnd(
                agentQuery,
                activeQuery,
                typeQuery
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(combinedQuery)
                .sort(s ->
                    s.field(f -> f.field("strength").order(SortOrder.Desc))
                )
                .build();

            return operationsHelper.executeSearch(
                request,
                BeliefRelationshipDocument.class,
                BeliefRelationshipDocumentMapper::fromDocument
            );
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to get relationships by type: " + relationshipType,
                e
            );
        }
    }


    public void deleteRelationshipsForBelief(String beliefId, String agentId) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            // Use storage service for belief validation
            if (!beliefStorageService.getBeliefById(beliefId).isPresent()) {
                return; // Belief doesn't exist, nothing to delete
            }

            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return;
            }

            // Find all relationships involving this belief
            List<BeliefRelationship> relationships = findRelationshipsForBelief(
                beliefId,
                agentId
            );

            if (relationships.isEmpty()) {
                return;
            }

            // Bulk delete relationships
            List<BulkOperation> operations = relationships
                .stream()
                .map(rel ->
                    BulkOperation.of(b ->
                        b.delete(d -> d.index(indexName).id(rel.getId()))
                    )
                )
                .collect(Collectors.toList());

            BulkRequest request = BulkRequest.of(b ->
                b.operations(operations).refresh(Refresh.WaitFor)
            );

            BulkResponse response = client.bulk(request);

            if (response.errors()) {
                logger.warn(
                    "Some relationships failed to delete for belief {}",
                    beliefId
                );
            } else {
                logger.debug(
                    "Successfully deleted {} relationships for belief {}",
                    relationships.size(),
                    beliefId
                );
            }
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to delete relationships for belief: " + beliefId,
                e
            );
        }
    }

    @Override
    public Optional<BeliefRelationship> findRelationshipById(
        String relationshipId,
        String agentId
    ) {
        Objects.requireNonNull(
            relationshipId,
            "Relationship ID cannot be null"
        );
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Optional.empty();
            }

            GetRequest request = GetRequest.of(g ->
                g.index(indexName).id(relationshipId)
            );

            GetResponse<BeliefRelationshipDocument> response = client.get(
                request,
                BeliefRelationshipDocument.class
            );

            if (response.found()) {
                BeliefRelationshipDocument document = response.source();
                return Optional.of(
                    BeliefRelationshipDocumentMapper.fromDocument(document)
                );
            } else {
                return Optional.empty();
            }
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to get relationship by ID: " + relationshipId,
                e
            );
        }
    }

    @Override
    public boolean deleteRelationship(String relationshipId, String agentId) {
        Objects.requireNonNull(
            relationshipId,
            "Relationship ID cannot be null"
        );
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return false;
            }

            DeleteRequest request = DeleteRequest.of(d ->
                d.index(indexName).id(relationshipId).refresh(Refresh.WaitFor)
            );

            client.delete(request);
            logger.debug(
                "Successfully deleted relationship: {}",
                relationshipId
            );
            return true;
        } catch (ElasticsearchException e) {
            if (e.response().status() == 404) {
                return false;
            }
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to delete relationship: " + relationshipId,
                e
            );
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to delete relationship: " + relationshipId,
                e
            );
        }
    }

   
    public BeliefRelationship updateRelationshipStrength(
        String relationshipId,
        String agentId,
        double newStrength
    ) {
        Objects.requireNonNull(
            relationshipId,
            "Relationship ID cannot be null"
        );
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                throw new StorageException(
                    "Relationship not found: " + relationshipId
                );
            }

            // Update the document
            Map<String, Object> updateDoc = new HashMap<>();
            updateDoc.put(
                "strength",
                Math.max(0.0, Math.min(1.0, newStrength))
            );
            updateDoc.put("last_updated", Instant.now());

            UpdateRequest<Object, Object> request = UpdateRequest.of(u ->
                u
                    .index(indexName)
                    .id(relationshipId)
                    .doc(updateDoc)
                    .refresh(Refresh.WaitFor)
            );

            client.update(request, Object.class);

            // Return updated relationship
            Optional<BeliefRelationship> updated = findRelationshipById(
                relationshipId,
                agentId
            );
            if (updated.isPresent()) {
                logger.debug(
                    "Successfully updated relationship strength: {}",
                    relationshipId
                );
                return updated.get();
            } else {
                throw new StorageException(
                    "Failed to retrieve updated relationship: " + relationshipId
                );
            }
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to update relationship strength: " + relationshipId,
                e
            );
        }
    }

    // ========== Analytics and Statistics ==========

    public long getRelationshipCount(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        String indexName = config.getRelationshipIndexName(agentId);
        return operationsHelper.countDocumentsForAgent(
            indexName,
            agentId,
            false
        );
    }

    public Map<RelationshipType, Long> getRelationshipTypeDistribution(
        String agentId
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyMap();
            }

            Query query = operationsHelper.createAgentActiveQuery(
                agentId,
                false
            );
            Map<FieldValue, Long> distribution =
                operationsHelper.executeTermsAggregation(
                    indexName,
                    "relationship_type",
                    query,
                    "type_distribution"
                );

            // Convert string codes to RelationshipType enum
            Map<RelationshipType, Long> result = new LinkedHashMap<>();
            for (Map.Entry<FieldValue, Long> entry : distribution.entrySet()) {
                try {
                    RelationshipType type = RelationshipType.fromCode(
                        entry.getKey().stringValue()
                    );
                    if (type != null) {
                        result.put(type, entry.getValue());
                    }
                } catch (Exception e) {
                    logger.debug(
                        "Unknown relationship type code: {}",
                        entry.getKey()
                    );
                }
            }

            return result;
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            logger.error(
                "Failed to get relationship type distribution for agent: {}",
                agentId,
                e
            );
            return Collections.emptyMap();
        }
    }

    public List<BeliefRelationship> getAllRelationships(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            Query query = operationsHelper.createAgentActiveQuery(
                agentId,
                false
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(query)
                .sort(s ->
                    s.field(f -> f.field("created_at").order(SortOrder.Desc))
                )
                .build();

            return operationsHelper.executeSearch(
                request,
                BeliefRelationshipDocument.class,
                BeliefRelationshipDocumentMapper::fromDocument
            );
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to get all relationships for agent: " + agentId,
                e
            );
        }
    }

    public boolean validateRelationshipIntegrity(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            logger.info(
                "Validating relationship integrity for agent: {}",
                agentId
            );

            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return true; // No relationships to validate
            }

            // Get all active relationships
            List<BeliefRelationship> relationships = getAllRelationships(
                agentId
            );
            if (relationships.isEmpty()) {
                return true;
            }

            // Extract all belief IDs referenced in relationships
            Set<String> referencedBeliefIds = new HashSet<>();
            for (BeliefRelationship rel : relationships) {
                referencedBeliefIds.add(rel.getSourceBeliefId());
                referencedBeliefIds.add(rel.getTargetBeliefId());
            }

            // Validate that all referenced beliefs exist using storage service
            List<String> existingBeliefs =
                beliefStorageService.getBeliefsById(
                    referencedBeliefIds
                ).stream().map(b -> b.getId())
                    .collect(Collectors.toList());

            // Check for orphaned relationships
            List<String> orphanedRelationships = new ArrayList<>();
            for (BeliefRelationship rel : relationships) {
                if (
                    !existingBeliefs.contains(rel.getSourceBeliefId()) ||
                    !existingBeliefs.contains(rel.getTargetBeliefId())
                ) {
                    orphanedRelationships.add(rel.getId());
                }
            }

            if (!orphanedRelationships.isEmpty()) {
                logger.warn(
                    "Found {} orphaned relationships for agent: {}",
                    orphanedRelationships.size(),
                    agentId
                );
                return false;
            }

            logger.info(
                "Relationship integrity validation passed for agent: {}",
                agentId
            );
            return true;
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            logger.error(
                "Error validating relationship integrity for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return false;
        }
    }

    // ========== Health and Monitoring ==========

    public boolean isHealthy() {
        try {
            return config.isHealthy() && operationCounter.get() > 0
                ? (errorCounter.get() / (double) operationCounter.get()) < 0.1
                : true;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new HashMap<>();

        try {
            health.put("healthy", isHealthy());
            health.put("operationCount", operationCounter.get());
            health.put("errorCount", errorCounter.get());
            health.put(
                "errorRate",
                operationCounter.get() > 0
                    ? (errorCounter.get() / (double) operationCounter.get())
                    : 0.0
            );
            health.put("timestamp", Instant.now());
            health.put("usingComposition", true);
            health.put(
                "beliefStorageServiceHealthy",
                beliefStorageService.isHealthy()
            );
        } catch (Exception e) {
            health.put("healthy", false);
            health.put("error", e.getMessage());
        }

        return health;
    }

    // ========== Private Helper Methods ==========

    /**
     * Generates a unique relationship ID.
     */
    private String generateRelationshipId() {
        return "rel_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Ensures that the relationship index exists.
     */
    private void ensureRelationshipIndexExists(String indexName) {
        if (!autoCreateIndices) {
            return;
        }

        Map<String, Property> mapping = createRelationshipIndexMapping();
        operationsHelper.ensureIndexExists(indexName, mapping);
    }

    /**
     * Creates the mapping for relationship indices.
     */
    private Map<String, Property> createRelationshipIndexMapping() {
        Map<String, Property> mapping = new HashMap<>();

        // Basic fields
        mapping.put(
            "id",
            Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))
        );
        mapping.put(
            "agent_id",
            Property.of(p -> p.keyword(KeywordProperty.of(k -> k.index(false))))
        );

        // Relationship fields
        mapping.put(
            "source_belief_id",
            Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))
        );
        mapping.put(
            "target_belief_id",
            Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))
        );
        mapping.put(
            "relationship_type",
            Property.of(p -> p.keyword(KeywordProperty.of(k -> k)))
        );
        mapping.put(
            "strength",
            Property.of(p -> p.double_(DoubleNumberProperty.of(d -> d)))
        );
        mapping.put("active", Property.of(p -> p.boolean_(b -> b)));

        // Temporal fields
        mapping.put(
            "created_at",
            Property.of(p -> p.date(DateProperty.of(d -> d)))
        );
        mapping.put(
            "last_updated",
            Property.of(p -> p.date(DateProperty.of(d -> d)))
        );
        mapping.put(
            "effective_from",
            Property.of(p -> p.date(DateProperty.of(d -> d)))
        );
        mapping.put(
            "effective_until",
            Property.of(p -> p.date(DateProperty.of(d -> d)))
        );

        // Metadata
        mapping.put(
            "metadata",
            Property.of(p -> p.object(o -> o.enabled(false)))
        );

        return mapping;
    }

    @Override
    public BeliefRelationship createTemporalRelationship(String sourceBeliefId, String targetBeliefId,
            RelationshipType relationshipType, double strength, String agentId, Instant effectiveFrom,
            Instant effectiveUntil) {
        Objects.requireNonNull(sourceBeliefId, "Source belief ID cannot be null");
        Objects.requireNonNull(targetBeliefId, "Target belief ID cannot be null");
        Objects.requireNonNull(relationshipType, "Relationship type cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        Objects.requireNonNull(effectiveFrom, "Effective from cannot be null");
        if (effectiveUntil != null && effectiveUntil.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Effective until cannot be before effective from");
        }
        operationCounter.incrementAndGet();
        try {
            // Validate beliefs exist using storage service
            List<String> beliefs = beliefStorageService.getBeliefsById(
                new HashSet<>(Arrays.asList(sourceBeliefId, targetBeliefId))
            )
            .stream()
            .map(Belief::getId)
            .collect(Collectors.toList());

            if (!beliefs.contains(sourceBeliefId)) {
                throw new StorageException("Source belief not found: " + sourceBeliefId);
            }
            if (!beliefs.contains(targetBeliefId)) {
                throw new StorageException("Target belief not found: " + targetBeliefId);
            }

            // Generate unique ID
            String relationshipId = generateRelationshipId();

            // Create relationship
            BeliefRelationship relationship = new BeliefRelationship(
                sourceBeliefId,
                targetBeliefId,
                relationshipType,
                strength,
                agentId
            );
            relationship.setId(relationshipId);
            relationship.setCreatedAt(Instant.now());
            relationship.setActive(true);
            relationship.setEffectiveFrom(effectiveFrom);
            relationship.setEffectiveUntil(effectiveUntil);

            // Convert to document
            BeliefRelationshipDocument document =
                BeliefRelationshipDocumentMapper.toDocument(relationship);

            // Store in Elasticsearch
            String indexName = config.getRelationshipIndexName(agentId);
            ensureRelationshipIndexExists(indexName);

            IndexRequest<BeliefRelationshipDocument> request = IndexRequest.of(
                i -> i.index(indexName).id(relationshipId).document(document).refresh(Refresh.WaitFor)
            );

            IndexResponse response = client.index(request);

            if (response.result() == Result.Created || response.result() == Result.Updated) {
                logger.debug("Successfully created temporal relationship {} between beliefs {} and {} for agent {}",
                        relationshipId, sourceBeliefId, targetBeliefId, agentId);
                return relationship;
            } else {
                throw new StorageException("Failed to create temporal relationship: unexpected result " + response.result());
            }
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new StorageException("Failed to create temporal relationship between beliefs " + sourceBeliefId + " and " + targetBeliefId, e);
        }
    }

    @Override
    public BeliefRelationship deprecateBeliefWith(String oldBeliefId, String newBeliefId, String reason,
            String agentId) {
        Objects.requireNonNull(oldBeliefId, "Old belief ID cannot be null");
        Objects.requireNonNull(newBeliefId, "New belief ID cannot be null");
        Objects.requireNonNull(reason, "Reason cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();
        try {
            // Validate beliefs exist using storage service
            List<String> beliefs = beliefStorageService.getBeliefsById(
                new HashSet<>(Arrays.asList(oldBeliefId, newBeliefId))
            )
            .stream()
            .map(Belief::getId)
            .collect(Collectors.toList());

            if (!beliefs.contains(oldBeliefId)) {
                throw new StorageException("Old belief not found: " + oldBeliefId);
            }
            if (!beliefs.contains(newBeliefId)) {
                throw new StorageException("New belief not found: " + newBeliefId);
            }

            // Create deprecation relationship
            BeliefRelationship relationship = new BeliefRelationship(
                oldBeliefId,
                newBeliefId,
                RelationshipType.DEPRECATES,
                1.0,
                agentId
            );
            relationship.setDeprecationReason(reason);
            relationship.setCreatedAt(Instant.now());
            relationship.setActive(true);

            // Convert to document
            BeliefRelationshipDocument document =
                BeliefRelationshipDocumentMapper.toDocument(relationship);

            // Store in Elasticsearch
            String indexName = config.getRelationshipIndexName(agentId);
            ensureRelationshipIndexExists(indexName);

            IndexRequest<BeliefRelationshipDocument> request = IndexRequest.of(
                i -> i.index(indexName).id(relationship.getId()).document(document).refresh(Refresh.WaitFor)
            );

            IndexResponse response = client.index(request);

            if (response.result() == Result.Created || response.result() == Result.Updated) {
                logger.debug("Successfully created deprecation relationship {} between beliefs {} and {} for agent {}",
                        relationship.getId(), oldBeliefId, newBeliefId, agentId);
                return relationship;
            } else {
                throw new StorageException("Failed to create deprecation relationship: unexpected result " + response.result());
            }
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new StorageException("Failed to create deprecation relationship between beliefs " + oldBeliefId + " and " + newBeliefId, e);
        }
    }

    @Override
    public BeliefRelationship updateRelationship(String relationshipId, double strength, Map<String, Object> metadata) {
        Objects.requireNonNull(relationshipId, "Relationship ID cannot be null");
        Objects.requireNonNull(strength, "Strength cannot be null");
        operationCounter.incrementAndGet();

        try {
            // Validate relationship exists
            Optional<BeliefRelationship> existingRelationship = findRelationshipById(relationshipId, null);
            if (!existingRelationship.isPresent()) {
                throw new StorageException("Relationship not found: " + relationshipId);
            }

            // Update strength and metadata
            BeliefRelationship relationship = existingRelationship.get();
            relationship.setStrength(Math.max(0.0, Math.min(1.0, strength)));
            if (metadata != null) {
                relationship.setMetadata(metadata);
            }
            relationship.setLastUpdated(Instant.now());

            // Convert to document
            BeliefRelationshipDocument document =
                BeliefRelationshipDocumentMapper.toDocument(relationship);

            // Store in Elasticsearch
            String indexName = config.getRelationshipIndexName(null);
            ensureRelationshipIndexExists(indexName);

            UpdateRequest<BeliefRelationshipDocument, BeliefRelationshipDocument> request = UpdateRequest.of(
                u -> u.index(indexName).id(relationshipId).doc(document).refresh(Refresh.WaitFor)
            );

            client.update(request, BeliefRelationshipDocument.class);

            logger.debug("Successfully updated relationship: {}", relationshipId);
            return relationship;
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new StorageException("Failed to update relationship: " + relationshipId, e);
        }
    }

    @Override
    public boolean deactivateRelationship(String relationshipId, String agentId) {
        Objects.requireNonNull(relationshipId, "Relationship ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return false;
            }

            // Update the document to set active to false
            Map<String, Object> updateDoc = new HashMap<>();
            updateDoc.put("active", false);
            updateDoc.put("last_updated", Instant.now());

            UpdateRequest<Object, Object> request = UpdateRequest.of(u ->
                u.index(indexName).id(relationshipId).doc(updateDoc).refresh(Refresh.WaitFor)
            );

            client.update(request, Object.class);

            logger.debug("Successfully deactivated relationship: {}", relationshipId);
            return true;
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new StorageException("Failed to deactivate relationship: " + relationshipId, e);
        }
    }

    @Override
    public boolean reactivateRelationship(String relationshipId, String agentId) {
        Objects.requireNonNull(relationshipId, "Relationship ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return false;
            }

            // Update the document to set active to true
            Map<String, Object> updateDoc = new HashMap<>();
            updateDoc.put("active", true);
            updateDoc.put("last_updated", Instant.now());

            UpdateRequest<Object, Object> request = UpdateRequest.of(u ->
                u.index(indexName).id(relationshipId).doc(updateDoc).refresh(Refresh.WaitFor)
            );

            client.update(request, Object.class);

            logger.debug("Successfully reactivated relationship: {}", relationshipId);
            return true;
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new StorageException("Failed to reactivate relationship: " + relationshipId, e);
        }
    }

    @Override
    public List<BeliefRelationship> findOutgoingRelationships(String beliefId, String agentId) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            // Validate belief exists using storage service
            if (!beliefStorageService.getBeliefById(beliefId).isPresent()) {
                return Collections.emptyList();
            }

            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            // Create query for outgoing relationships
            Query query = Query.of(q ->
                q.term(t -> t.field("source_belief_id").value(beliefId))
            );

            Query agentQuery = operationsHelper.createAgentQuery(agentId);
            Query activeQuery = operationsHelper.createActiveQuery(true);
            Query combinedQuery = operationsHelper.combineQueriesAnd(
                agentQuery,
                activeQuery,
                query
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(combinedQuery)
                .sort(s ->
                    s.field(f -> f.field("created_at").order(SortOrder.Desc))
                )
                .build();

            return operationsHelper.executeSearch(
                request,
                BeliefRelationshipDocument.class,
                BeliefRelationshipDocumentMapper::fromDocument
            );
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to get outgoing relationships for belief: " + beliefId,
                e
            );
        }
    }

    @Override
    public List<BeliefRelationship> findIncomingRelationships(String beliefId, String agentId) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            // Validate belief exists using storage service
            if (!beliefStorageService.getBeliefById(beliefId).isPresent()) {
                return Collections.emptyList();
            }

            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            // Create query for incoming relationships
            Query query = Query.of(q ->
                q.term(t -> t.field("target_belief_id").value(beliefId))
            );

            Query agentQuery = operationsHelper.createAgentQuery(agentId);
            Query activeQuery = operationsHelper.createActiveQuery(true);
            Query combinedQuery = operationsHelper.combineQueriesAnd(
                agentQuery,
                activeQuery,
                query
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(combinedQuery)
                .sort(s ->
                    s.field(f -> f.field("created_at").order(SortOrder.Desc))
                )
                .build();

            return operationsHelper.executeSearch(
                request,
                BeliefRelationshipDocument.class,
                BeliefRelationshipDocumentMapper::fromDocument
            );
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to get incoming relationships for belief: " + beliefId,
                e
            );
        }
    }

    @Override
    public List<BeliefRelationship> findRelationshipsBetween(String sourceBeliefId, String targetBeliefId,
            String agentId) {
        Objects.requireNonNull(sourceBeliefId, "Source belief ID cannot be null");
        Objects.requireNonNull(targetBeliefId, "Target belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();
        try {
            // Validate beliefs exist using storage service
            List<String> beliefs = beliefStorageService.getBeliefsById(
                new HashSet<>(Arrays.asList(sourceBeliefId, targetBeliefId))
            )
            .stream()
            .map(Belief::getId)
            .collect(Collectors.toList());

            if (!beliefs.contains(sourceBeliefId)) {
                throw new StorageException("Source belief not found: " + sourceBeliefId);
            }
            if (!beliefs.contains(targetBeliefId)) {
                throw new StorageException("Target belief not found: " + targetBeliefId);
            }

            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            // Create query for relationships between the two beliefs
            Query query = Query.of(q ->
                q.bool(b -> b.must(
                    Query.of(
                        m -> m.term(t -> t.field("source_belief_id").value(sourceBeliefId))
                    ),
                    Query.of(
                        m -> m.term(t -> t.field("target_belief_id").value(targetBeliefId))
                    )
                ))
            );

            Query agentQuery = operationsHelper.createAgentQuery(agentId);
            Query activeQuery = operationsHelper.createActiveQuery(true);
            Query combinedQuery = operationsHelper.combineQueriesAnd(
                agentQuery,
                activeQuery,
                query
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(combinedQuery)
                .sort(s ->
                    s.field(f -> f.field("created_at").order(SortOrder.Desc))
                )
                .build();

            return operationsHelper.executeSearch(
                request,
                BeliefRelationshipDocument.class,
                BeliefRelationshipDocumentMapper::fromDocument
            );
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to get relationships between beliefs: " + sourceBeliefId + " and " + targetBeliefId,
                e
            );
        }
    }

    @Override
    public List<String> findDeprecatedBeliefs(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            // Create query for deprecated relationships
            Query query = Query.of(q ->
                q.term(t -> t.field("relationship_type").value(RelationshipType.DEPRECATES.getCode()))
            );

            Query agentQuery = operationsHelper.createAgentQuery(agentId);
            Query activeQuery = operationsHelper.createActiveQuery(true);
            Query combinedQuery = operationsHelper.combineQueriesAnd(
                agentQuery,
                activeQuery,
                query
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(combinedQuery)
                .sort(s ->
                    s.field(f -> f.field("created_at").order(SortOrder.Desc))
                )
                .build();

            List<BeliefRelationship> relationships = operationsHelper.executeSearch(
                request,
                BeliefRelationshipDocument.class,
                BeliefRelationshipDocumentMapper::fromDocument
            );

            // Extract unique deprecated belief IDs
            return relationships.stream()
                .map(BeliefRelationship::getTargetBeliefId)
                .distinct()
                .collect(Collectors.toList());
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to find deprecated beliefs for agent: " + agentId,
                e
            );
        }
    }

    @Override
    public List<Belief> findSupersedingBeliefs(String beliefId, String agentId) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            // Validate belief exists using storage service
            if (!beliefStorageService.getBeliefById(beliefId).isPresent()) {
                return Collections.emptyList();
            }

            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            // Create query for relationships where the belief is the target
            Query query = Query.of(q ->
                q.term(t -> t.field("target_belief_id").value(beliefId))
            );

            Query agentQuery = operationsHelper.createAgentQuery(agentId);
            Query activeQuery = operationsHelper.createActiveQuery(true);
            Query combinedQuery = operationsHelper.combineQueriesAnd(
                agentQuery,
                activeQuery,
                query
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(combinedQuery)
                .sort(s ->
                    s.field(f -> f.field("created_at").order(SortOrder.Desc))
                )
                .build();

            List<BeliefRelationship> relationships = operationsHelper.executeSearch(
                request,
                BeliefRelationshipDocument.class,
                BeliefRelationshipDocumentMapper::fromDocument
            );

            // Extract unique superseding belief IDs
            Set<String> supersedingBeliefs = relationships.stream()
                .map(BeliefRelationship::getSourceBeliefId)
                .collect(Collectors.toSet());

            // Fetch beliefs by IDs
            return beliefStorageService.getBeliefsById(supersedingBeliefs);
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to find superseding beliefs for belief: " + beliefId,
                e
            );
        }
    }

    // TODO: only return beliefs that are actually deprecated, not all in the chain
    @Override
    public List<Belief> findDeprecationChain(String beliefId, String agentId) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            // Validate belief exists using storage service
            if (!beliefStorageService.getBeliefById(beliefId).isPresent()) {
                return Collections.emptyList();
            }

            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            // Create query for relationships where the belief is the source
            Query query = Query.of(q ->
                q.term(t -> t.field("source_belief_id").value(beliefId))
            );

            Query agentQuery = operationsHelper.createAgentQuery(agentId);
            Query activeQuery = operationsHelper.createActiveQuery(true);
            Query combinedQuery = operationsHelper.combineQueriesAnd(
                agentQuery,
                activeQuery,
                query
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(combinedQuery)
                .sort(s ->
                    s.field(f -> f.field("created_at").order(SortOrder.Desc))
                )
                .build();

            List<BeliefRelationship> relationships = operationsHelper.executeSearch(
                request,
                BeliefRelationshipDocument.class,
                BeliefRelationshipDocumentMapper::fromDocument
            );

            // Extract unique deprecated belief IDs
            Set<String> deprecatedBeliefs = relationships.stream()
                .map(BeliefRelationship::getTargetBeliefId)
                .collect(Collectors.toSet());

            // Fetch beliefs by IDs
            return beliefStorageService.getBeliefsById(deprecatedBeliefs);
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to find deprecation chain for belief: " + beliefId,
                e
            );
        }
    }

    @Override
    public Set<String> findRelatedBeliefs(String beliefId, String agentId, int maxDepth) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            // Validate belief exists using storage service
            if (!beliefStorageService.getBeliefById(beliefId).isPresent()) {
                return Collections.emptySet();
            }

            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptySet();
            }

            // Create query for relationships involving the belief
            Query query = Query.of(q ->
                q.bool(b -> b.should(
                    Query.of(m -> m.term(t -> t.field("source_belief_id").value(beliefId))),
                    Query.of(m -> m.term(t -> t.field("target_belief_id").value(beliefId)))
                ))
            );

            Query agentQuery = operationsHelper.createAgentQuery(agentId);
            Query activeQuery = operationsHelper.createActiveQuery(true);
            Query combinedQuery = operationsHelper.combineQueriesAnd(
                agentQuery,
                activeQuery,
                query
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(combinedQuery)
                .sort(s ->
                    s.field(f -> f.field("created_at").order(SortOrder.Desc))
                )
                .build();

            List<BeliefRelationship> relationships = operationsHelper.executeSearch(
                request,
                BeliefRelationshipDocument.class,
                BeliefRelationshipDocumentMapper::fromDocument
            );

            // Extract unique related belief IDs
            Set<String> relatedBeliefs = new HashSet<>();
            for (BeliefRelationship rel : relationships) {
                if (rel.getSourceBeliefId().equals(beliefId)) {
                    relatedBeliefs.add(rel.getTargetBeliefId());
                } else {
                    relatedBeliefs.add(rel.getSourceBeliefId());
                }
            }

            return relatedBeliefs;
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            throw new StorageException(
                "Failed to find related beliefs for belief: " + beliefId,
                e
            );
        }
    }


    @Override
    public Map<String, Object> getEfficientGraphStatistics(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        operationCounter.incrementAndGet();

        try {
            String indexName = config.getRelationshipIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyMap();
            }

            // Get index stats
            GetIndexResponse indexStats = client.indices().get(
                GetIndexRequest.of(g -> g.index(indexName))
            );

            Map<String, Object> stats = new HashMap<>();
            stats.put("index", indexName);
            for (String key : indexStats.result().keySet()) {
                stats.put(key, indexStats.result().get(key));
            }

            return stats;
        } catch (IOException e) {
            errorCounter.incrementAndGet();
            throw new StorageException("Failed to get efficient graph statistics for agent: " + agentId, e);
        }
    }

    // TODO: do we need this?
    @Override
    public List<String> performEfficientGraphValidation(String agentId) {
        return Collections.emptyList();
    }

    // TODO: move this to BeliefKnowledgeGraphService
    @Override
    public BeliefKnowledgeGraph createSnapshotGraph(String agentId, boolean includeInactive) {
        return new BeliefKnowledgeGraph(agentId);
    }

    // TODO: move this to BeliefKnowledgeGraphService
    @Override
    public BeliefKnowledgeGraph createFilteredSnapshot(String agentId, Set<String> beliefIds,
            Set<RelationshipType> relationshipTypes, int maxBeliefs) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createFilteredSnapshot'");
    }

    // TODO: move this to BeliefKnowledgeGraphService
    @Override
    public BeliefKnowledgeGraph createExportGraph(String agentId, String format) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createExportGraph'");
    }

    // TODO: move this to BeliefKnowledgeGraphService
    @Override
    public List<BeliefRelationship> findShortestPath(String sourceBeliefId, String targetBeliefId, String agentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findShortestPath'");
    }


    @Override
    public List<BeliefRelationship> createRelationshipsBulk(List<BeliefRelationship> relationships, String agentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createRelationshipsBulk'");
    }

    @Override
    public String exportKnowledgeGraph(String agentId, String format) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'exportKnowledgeGraph'");
    }

    @Override
    public int importRelationships(String data, String format, String agentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'importRelationships'");
    }

    @Override
    public int cleanupKnowledgeGraph(String agentId, int olderThanDays) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'cleanupKnowledgeGraph'");
    }
}
