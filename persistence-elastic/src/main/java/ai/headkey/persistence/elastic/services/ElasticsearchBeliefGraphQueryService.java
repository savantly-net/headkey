package ai.headkey.persistence.elastic.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefKnowledgeGraph;
import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.enums.RelationshipType;
import ai.headkey.memory.exceptions.StorageException;
import ai.headkey.memory.interfaces.BeliefGraphQueryService;
import ai.headkey.persistence.elastic.configuration.ElasticsearchConfiguration;
import ai.headkey.persistence.elastic.documents.BeliefDocument;
import ai.headkey.persistence.elastic.infrastructure.ElasticsearchOperationsHelper;
import ai.headkey.persistence.elastic.mappers.BeliefDocumentMapper;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;

/**
 * Elasticsearch implementation of the BeliefGraphQueryService interface.
 *
 * This implementation provides efficient graph query operations using Elasticsearch
 * as the backend storage. It leverages service composition with BeliefStorageService
 * and BeliefRelationshipService to avoid code duplication.
 *
 * Features:
 * - Efficient graph statistics using database aggregations
 * - Streaming operations for large datasets
 * - Graph validation and integrity checks
 * - Optimized queries for graph traversal operations
 * - Composition-based architecture to reduce duplication
 */
public class ElasticsearchBeliefGraphQueryService
    implements BeliefGraphQueryService {

    private static final Logger logger = LoggerFactory.getLogger(
        ElasticsearchBeliefGraphQueryService.class
    );

    private final ElasticsearchBeliefStorageService beliefStorageService;
    private final ElasticsearchBeliefRelationshipService relationshipService;
    private final ElasticsearchConfiguration config;
    private final ElasticsearchClient client;
    private final ElasticsearchOperationsHelper operationsHelper;

    // Configuration parameters
    private final int streamBatchSize;

    /**
     * Creates a new Elasticsearch belief graph query service with service composition.
     *
     * @param beliefStorageService The belief storage service to delegate to
     * @param relationshipService The relationship service for graph operations
     */
    public ElasticsearchBeliefGraphQueryService(
        ElasticsearchBeliefStorageService beliefStorageService,
        ElasticsearchBeliefRelationshipService relationshipService
    ) {
        this(beliefStorageService, relationshipService, 1000);
    }

    /**
     * Creates a new Elasticsearch belief graph query service with configuration.
     *
     * @param beliefStorageService The belief storage service to delegate to
     * @param relationshipService The relationship service for graph operations
     * @param streamBatchSize Batch size for streaming operations
     */
    public ElasticsearchBeliefGraphQueryService(
        ElasticsearchBeliefStorageService beliefStorageService,
        ElasticsearchBeliefRelationshipService relationshipService,
        int streamBatchSize
    ) {
        this.beliefStorageService = Objects.requireNonNull(
            beliefStorageService,
            "Belief storage service cannot be null"
        );
        this.relationshipService = Objects.requireNonNull(
            relationshipService,
            "Relationship service cannot be null"
        );
        this.config = beliefStorageService.getConfig();
        this.client = beliefStorageService.getClient();
        this.operationsHelper = beliefStorageService.getOperationsHelper();
        this.streamBatchSize = Math.max(10, streamBatchSize);

        logger.info(
            "Initialized Elasticsearch belief graph query service with stream batch size: {}",
            this.streamBatchSize
        );
    }

    // ========================================
    // EFFICIENT GRAPH STATISTICS
    // ========================================

    @Override
    public Map<String, Long> getGraphStatistics(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        Map<String, Long> stats = new HashMap<>();

        try {
            // Get belief counts
            stats.put("totalBeliefs", getBeliefsCount(agentId, true));
            stats.put("activeBeliefs", getBeliefsCount(agentId, false));

            // Get relationship counts
            stats.put(
                "totalRelationships",
                getRelationshipsCount(agentId, true)
            );
            stats.put(
                "activeRelationships",
                getRelationshipsCount(agentId, false)
            );

            // Get deprecated beliefs count
            stats.put("deprecatedBeliefs", getDeprecatedBeliefsCount(agentId));

            // Calculate graph density
            long totalBeliefs = stats.get("totalBeliefs");
            long totalRelationships = stats.get("totalRelationships");
            if (totalBeliefs > 0) {
                double density = (double) totalRelationships / totalBeliefs;
                stats.put("graphDensity", Math.round(density * 100));
            } else {
                stats.put("graphDensity", 0L);
            }

            logger.debug(
                "Generated graph statistics for agent {}: {} items",
                agentId,
                stats.size()
            );
            return stats;
        } catch (Exception e) {
            logger.error(
                "Error generating graph statistics for agent: {}",
                agentId,
                e
            );
            throw new StorageException(
                "Failed to generate graph statistics",
                e
            );
        }
    }

    @Override
    public Map<RelationshipType, Long> getRelationshipTypeDistribution(
        String agentId
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return relationshipService.getRelationshipTypeDistribution(agentId);
        } catch (Exception e) {
            logger.error(
                "Error getting relationship type distribution for agent: {}",
                agentId,
                e
            );
            return Collections.emptyMap();
        }
    }

    @Override
    public long getBeliefsCount(String agentId, boolean includeInactive) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            String indexName = config.getBeliefIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return 0;
            }

            Query query = includeInactive
                ? operationsHelper.createAgentQuery(agentId)
                : operationsHelper.createAgentActiveQuery(agentId, false);

            CountRequest request = CountRequest.of(c ->
                c.index(indexName).query(query)
            );
            CountResponse response = client.count(request);

            return response.count();
        } catch (Exception e) {
            logger.error(
                "Error counting beliefs for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return 0;
        }
    }

    @Override
    public long getRelationshipsCount(String agentId, boolean includeInactive) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return relationshipService.getRelationshipCount(agentId);
        } catch (Exception e) {
            logger.error(
                "Error counting relationships for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return 0;
        }
    }

    @Override
    public long getDeprecatedBeliefsCount(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return findDeprecatedBeliefIds(agentId).size();
        } catch (Exception e) {
            logger.error(
                "Error counting deprecated beliefs for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return 0;
        }
    }

    // ========================================
    // TARGETED BELIEF QUERIES
    // ========================================

    @Override
    public Stream<Belief> streamBeliefs(
        String agentId,
        boolean includeInactive,
        int pageSize
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            List<Belief> beliefs = includeInactive
                ? beliefStorageService.getBeliefsForAgent(agentId, true)
                : beliefStorageService.getBeliefsForAgent(agentId, false);

            return beliefs.stream();
        } catch (Exception e) {
            logger.error(
                "Error streaming beliefs for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Stream.empty();
        }
    }

    @Override
    public List<Belief> getBeliefsInCategory(
        String agentId,
        String category,
        int limit
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");

        try {
            return beliefStorageService
                .getBeliefsInCategory(category, agentId, false)
                .stream()
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                "Error finding beliefs by category for agent {}, category {}: {}",
                agentId,
                category,
                e.getMessage()
            );
            throw new StorageException("Failed to find beliefs by category", e);
        }
    }

    @Override
    public List<Belief> getHighConfidenceBeliefs(
        String agentId,
        double confidenceThreshold,
        int limit
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            String indexName = config.getBeliefIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            Query confidenceQuery = operationsHelper.createConfidenceRangeQuery(
                confidenceThreshold,
                1.0
            );
            Query agentQuery = operationsHelper.createAgentActiveQuery(
                agentId,
                false
            );
            Query combinedQuery = operationsHelper.combineQueriesAnd(
                agentQuery,
                confidenceQuery
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(combinedQuery)
                .size(limit > 0 ? limit : 10000)
                .sort(s ->
                    s.field(f -> f.field("confidence").order(SortOrder.Desc))
                )
                .build();

            return operationsHelper.executeSearch(
                request,
                BeliefDocument.class,
                BeliefDocumentMapper::fromDocument
            );
        } catch (Exception e) {
            logger.error(
                "Error finding high confidence beliefs for agent {}: {}",
                agentId,
                e.getMessage()
            );
            throw new StorageException(
                "Failed to find high confidence beliefs",
                e
            );
        }
    }

    @Override
    public List<Belief> getRecentBeliefs(String agentId, int limit) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            String indexName = config.getBeliefIndexName(agentId);
            if (!operationsHelper.indexExists(indexName)) {
                return Collections.emptyList();
            }

            Query agentQuery = operationsHelper.createAgentActiveQuery(
                agentId,
                false
            );

            SearchRequest request = operationsHelper
                .createBaseSearchRequest(indexName)
                .query(agentQuery)
                .size(limit > 0 ? limit : 10000)
                .sort(s ->
                    s.field(f -> f.field("created_at").order(SortOrder.Desc))
                )
                .build();

            return operationsHelper.executeSearch(
                request,
                BeliefDocument.class,
                BeliefDocumentMapper::fromDocument
            );
        } catch (Exception e) {
            logger.error(
                "Error finding recent beliefs for agent {}: {}",
                agentId,
                e.getMessage()
            );
            throw new StorageException("Failed to find recent beliefs", e);
        }
    }

    @Override
    public boolean beliefExists(String beliefId, String agentId) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return beliefStorageService
                .findBeliefById(beliefId, agentId)
                .isPresent();
        } catch (Exception e) {
            logger.error("Error checking belief existence: {}", beliefId, e);
            return false;
        }
    }

    // ========================================
    // RELATIONSHIP QUERIES
    // ========================================

    // TODO: Consider implementing pagination for large datasets
    @Override
    public Stream<BeliefRelationship> streamRelationships(
        String agentId,
        boolean includeInactive,
        int pageSize
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return relationshipService.getAllRelationships(agentId).stream();
        } catch (Exception e) {
            logger.error(
                "Error streaming relationships for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Stream.empty();
        }
    }

    @Override
    public long getRelationshipCount(
        String beliefId,
        String agentId,
        String direction
    ) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        Objects.requireNonNull(direction, "Direction cannot be null");

        try {
            List<BeliefRelationship> relationships =
                relationshipService.findRelationshipsForBelief(
                    beliefId,
                    agentId
                );

            return switch (direction.toLowerCase()) {
                case "incoming" -> relationships
                    .stream()
                    .filter(r -> r.getTargetBeliefId().equals(beliefId))
                    .count();
                case "outgoing" -> relationships
                    .stream()
                    .filter(r -> r.getSourceBeliefId().equals(beliefId))
                    .count();
                case "both" -> relationships.size();
                default -> throw new IllegalArgumentException(
                    "Invalid direction: " + direction
                );
            };
        } catch (Exception e) {
            logger.error(
                "Error counting relationships for belief {}: {}",
                beliefId,
                e.getMessage()
            );
            return 0;
        }
    }

    @Override
    public List<String> getRelationshipIds(
        String beliefId,
        String agentId,
        String direction,
        int limit
    ) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        Objects.requireNonNull(direction, "Direction cannot be null");

        try {
            List<BeliefRelationship> relationships =
                relationshipService.findRelationshipsForBelief(
                    beliefId,
                    agentId
                );

            Stream<BeliefRelationship> filteredStream =
                switch (direction.toLowerCase()) {
                    case "incoming" -> relationships
                        .stream()
                        .filter(r -> r.getTargetBeliefId().equals(beliefId));
                    case "outgoing" -> relationships
                        .stream()
                        .filter(r -> r.getSourceBeliefId().equals(beliefId));
                    case "both" -> relationships.stream();
                    default -> throw new IllegalArgumentException(
                        "Invalid direction: " + direction
                    );
                };

            return filteredStream
                .map(BeliefRelationship::getId)
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                "Error getting relationship IDs for belief {}: {}",
                beliefId,
                e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getConnectedBeliefIds(
        String beliefId,
        String agentId,
        String direction,
        Set<RelationshipType> relationshipTypes,
        int limit
    ) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        Objects.requireNonNull(direction, "Direction cannot be null");

        try {
            List<BeliefRelationship> relationships =
                relationshipService.findRelationshipsForBelief(
                    beliefId,
                    agentId
                );

            Stream<BeliefRelationship> filteredStream = relationships.stream();

            // Filter by relationship types if specified
            if (relationshipTypes != null && !relationshipTypes.isEmpty()) {
                filteredStream = filteredStream.filter(r ->
                    relationshipTypes.contains(r.getRelationshipType())
                );
            }

            // Filter by direction and get connected belief IDs
            Stream<String> beliefIdStream =
                switch (direction.toLowerCase()) {
                    case "incoming" -> filteredStream
                        .filter(r -> r.getTargetBeliefId().equals(beliefId))
                        .map(BeliefRelationship::getSourceBeliefId);
                    case "outgoing" -> filteredStream
                        .filter(r -> r.getSourceBeliefId().equals(beliefId))
                        .map(BeliefRelationship::getTargetBeliefId);
                    case "both" -> filteredStream.map(r ->
                        r.getSourceBeliefId().equals(beliefId)
                            ? r.getTargetBeliefId()
                            : r.getSourceBeliefId()
                    );
                    default -> throw new IllegalArgumentException(
                        "Invalid direction: " + direction
                    );
                };

            return beliefIdStream
                .distinct()
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                "Error getting connected belief IDs for belief {}: {}",
                beliefId,
                e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    @Override
    public boolean areBeliefsDirectlyConnected(
        String sourceBeliefId,
        String targetBeliefId,
        String agentId,
        Set<RelationshipType> relationshipTypes
    ) {
        Objects.requireNonNull(
            sourceBeliefId,
            "Source belief ID cannot be null"
        );
        Objects.requireNonNull(
            targetBeliefId,
            "Target belief ID cannot be null"
        );
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            List<BeliefRelationship> relationships =
                relationshipService.findRelationshipsBetween(
                    sourceBeliefId,
                    targetBeliefId,
                    agentId
                );

            if (relationshipTypes == null || relationshipTypes.isEmpty()) {
                return !relationships.isEmpty();
            }

            return relationships
                .stream()
                .anyMatch(r ->
                    relationshipTypes.contains(r.getRelationshipType())
                );
        } catch (Exception e) {
            logger.error(
                "Error checking connection between beliefs {} and {}: {}",
                sourceBeliefId,
                targetBeliefId,
                e.getMessage()
            );
            return false;
        }
    }

    // ========================================
    // EFFICIENT DEPRECATION QUERIES
    // ========================================

    @Override
    public List<String> getDeprecatedBeliefIds(String agentId, int limit) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return relationshipService
                .findDeprecatedBeliefs(agentId)
                .stream()
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                "Error finding deprecated belief IDs for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getSupersedingBeliefIds(
        String beliefId,
        String agentId
    ) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return relationshipService
                .findSupersedingBeliefs(beliefId, agentId)
                .stream()
                .map(Belief::getId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                "Error finding superseding belief IDs for belief {}: {}",
                beliefId,
                e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isBeliefDeprecated(String beliefId, String agentId) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return relationshipService
                .findDeprecatedBeliefs(agentId)
                .contains(beliefId);
        } catch (Exception e) {
            logger.error(
                "Error checking if belief is deprecated {}: {}",
                beliefId,
                e.getMessage()
            );
            return false;
        }
    }

    @Override
    public List<String> getDeprecationChainIds(
        String beliefId,
        String agentId,
        int maxDepth
    ) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return relationshipService
                .findDeprecationChain(beliefId, agentId)
                .stream()
                .map(Belief::getId)
                .limit(maxDepth > 0 ? maxDepth : Integer.MAX_VALUE)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                "Error finding deprecation chain for belief {}: {}",
                beliefId,
                e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    // ========================================
    // EFFICIENT GRAPH TRAVERSAL
    // ========================================

    @Override
    public Set<String> getReachableBeliefIds(
        String startBeliefId,
        String agentId,
        int maxDepth,
        Set<RelationshipType> relationshipTypes
    ) {
        Objects.requireNonNull(startBeliefId, "Start belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return relationshipService.findRelatedBeliefs(
                startBeliefId,
                agentId,
                maxDepth
            );
        } catch (Exception e) {
            logger.error(
                "Error finding reachable belief IDs from {}: {}",
                startBeliefId,
                e.getMessage()
            );
            return Collections.emptySet();
        }
    }

    @Override
    public List<String> getShortestPathIds(
        String sourceBeliefId,
        String targetBeliefId,
        String agentId,
        int maxDepth
    ) {
        Objects.requireNonNull(
            sourceBeliefId,
            "Source belief ID cannot be null"
        );
        Objects.requireNonNull(
            targetBeliefId,
            "Target belief ID cannot be null"
        );
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return relationshipService
                .findShortestPath(sourceBeliefId, targetBeliefId, agentId)
                .stream()
                .map(BeliefRelationship::getId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                "Error finding shortest path between {} and {}: {}",
                sourceBeliefId,
                targetBeliefId,
                e.getMessage()
            );
            return Collections.emptyList();
        }
    }


    // ========================================
    // CONFLICT AND VALIDATION QUERIES
    // ========================================

    // TODO: Implement conflict resolution logic
    @Override
    public List<String> getConflictingRelationshipIds(
        String agentId,
        int limit
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        return List.of(); // Placeholder for future implementation
    }

    // TODO: Implement logic to find contradictory beliefs
    @Override
    public List<Map<String, String>> getContradictoryBeliefPairs(
        String agentId,
        int limit
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        return Collections.emptyList(); // Placeholder for future implementation
    }

    @Override
    public List<String> validateGraphStructure(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return relationshipService.performEfficientGraphValidation(agentId);
        } catch (Exception e) {
            logger.error(
                "Error validating graph structure for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Arrays.asList("Error during validation: " + e.getMessage());
        }
    }

    // ========================================
    // BATCH AND UTILITY OPERATIONS
    // ========================================

    @Override
    public Map<String, Belief> getBeliefsById(
        Set<String> beliefIds,
        String agentId
    ) {
        Objects.requireNonNull(beliefIds, "Belief IDs cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            Map<String, Belief> results = new HashMap<>();
            for (String beliefId : beliefIds) {
                beliefStorageService
                    .findBeliefById(beliefId, agentId)
                    .ifPresent(belief -> results.put(beliefId, belief));
            }
            return results;
        } catch (Exception e) {
            logger.error(
                "Error getting beliefs by IDs for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, BeliefRelationship> getRelationshipsById(
        Set<String> relationshipIds,
        String agentId
    ) {
        Objects.requireNonNull(
            relationshipIds,
            "Relationship IDs cannot be null"
        );
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            Map<String, BeliefRelationship> results = new HashMap<>();
            for (String relationshipId : relationshipIds) {
                relationshipService
                    .findRelationshipById(relationshipId, agentId)
                    .ifPresent(relationship ->
                        results.put(relationshipId, relationship)
                    );
            }
            return results;
        } catch (Exception e) {
            logger.error(
                "Error getting relationships by IDs for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Boolean> checkBeliefsExist(
        Set<String> beliefIds,
        String agentId
    ) {
        Objects.requireNonNull(beliefIds, "Belief IDs cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            Map<String, Boolean> results = new HashMap<>();
            for (String beliefId : beliefIds) {
                results.put(beliefId, beliefExists(beliefId, agentId));
            }
            return results;
        } catch (Exception e) {
            logger.error(
                "Error checking belief existence for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> getBeliefDegrees(
        Set<String> beliefIds,
        String agentId,
        String direction
    ) {
        Objects.requireNonNull(beliefIds, "Belief IDs cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        Objects.requireNonNull(direction, "Direction cannot be null");

        try {
            Map<String, Long> results = new HashMap<>();
            for (String beliefId : beliefIds) {
                results.put(
                    beliefId,
                    getRelationshipCount(beliefId, agentId, direction)
                );
            }
            return results;
        } catch (Exception e) {
            logger.error(
                "Error getting belief degrees for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Collections.emptyMap();
        }
    }

    // ========================================
    // EFFICIENT SEARCH AND FILTERING
    // ========================================

    @Override
    public List<Belief> searchBeliefsByContent(
        String agentId,
        String searchText,
        int limit
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");
        Objects.requireNonNull(searchText, "Search text cannot be null");

        try {
            return beliefStorageService
                .searchBeliefs(searchText, agentId, limit)
                .stream()
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                "Error searching beliefs by content for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getSimilarBeliefIds(
        String beliefId,
        String agentId,
        double similarityThreshold,
        int limit
    ) {
        Objects.requireNonNull(beliefId, "Belief ID cannot be null");
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            Belief belief = beliefStorageService
                .findBeliefById(beliefId, agentId)
                .orElseThrow(() ->
                    new StorageException(
                        "Belief not found: " + beliefId
                    )
                );
            var similarBeliefs = beliefStorageService
                .findSimilarBeliefs(belief.getStatement(), agentId, similarityThreshold, limit)
                .stream()
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
            var result = new ArrayList<Map<String, Object>>();
            
            similarBeliefs.forEach(s -> {
                Map<String, Object> beliefData = new HashMap<>();
                beliefData.put("beliefId", s.getBelief().getId());
                beliefData.put("similarityScore", s.getSimilarityScore());
                result.add(beliefData);
            });
            return result;
        } catch (Exception e) {
            logger.error(
                "Error finding similar belief IDs for belief {}: {}",
                beliefId,
                e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    // ========================================
    // HEALTH AND MAINTENANCE
    // ========================================

    @Override
    public Map<String, Object> getServiceHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            health.put("service", "ElasticsearchBeliefGraphQueryService");
            health.put("healthy", config.isHealthy());
            health.put("streamBatchSize", streamBatchSize);

            // Check storage service health
            if (beliefStorageService != null) {
                health.put("beliefStorageHealthy", true);
            }

            // Check relationship service health
            if (relationshipService != null) {
                health.put("relationshipServiceHealthy", true);
            }

            return health;
        } catch (Exception e) {
            logger.error("Error getting service health: {}", e.getMessage());
            health.put("healthy", false);
            health.put("error", e.getMessage());
            return health;
        }
    }

    @Override
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            metrics.put("service", "ElasticsearchBeliefGraphQueryService");
            metrics.put("streamBatchSize", streamBatchSize);
            metrics.put("timestamp", System.currentTimeMillis());

            return metrics;
        } catch (Exception e) {
            logger.error(
                "Error getting performance metrics: {}",
                e.getMessage()
            );
            metrics.put("error", e.getMessage());
            return metrics;
        }
    }

    @Override
    public long estimateGraphMemoryUsage(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            long beliefCount = getBeliefsCount(agentId, true);
            long relationshipCount = getRelationshipsCount(agentId, true);

            // Rough estimation: 1KB per belief, 512 bytes per relationship
            long estimatedBytes =
                (beliefCount * 1024) + (relationshipCount * 512);

            return estimatedBytes;
        } catch (Exception e) {
            logger.error(
                "Error estimating memory usage for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return 0;
        }
    }

    // ========================================
    // COMPREHENSIVE GRAPH STATISTICS
    // ========================================

    @Override
    public Map<String, Object> getComprehensiveGraphStatistics(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            Map<String, Object> stats = new HashMap<>();

            // Basic statistics
            Map<String, Long> basicStats = getGraphStatistics(agentId);
            stats.putAll(basicStats);

            // Relationship type distribution
            Map<RelationshipType, Long> typeDistribution =
                getRelationshipTypeDistribution(agentId);
            stats.put("relationshipTypeDistribution", typeDistribution);

            // Average relationship strength
            stats.put(
                "averageRelationshipStrength",
                getAverageRelationshipStrength(agentId, false)
            );

            // Memory usage estimate
            stats.put(
                "estimatedMemoryUsage",
                estimateGraphMemoryUsage(agentId)
            );

            return stats;
        } catch (Exception e) {
            logger.error(
                "Error getting comprehensive graph statistics for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Collections.emptyMap();
        }
    }

    @Override
    public double getAverageRelationshipStrength(
        String agentId,
        boolean includeInactive
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            List<BeliefRelationship> relationships =
                relationshipService.getAllRelationships(agentId);

            if (relationships.isEmpty()) {
                return 0.0;
            }

            double totalStrength = relationships
                .stream()
                .filter(r -> includeInactive || r.isActive())
                .mapToDouble(BeliefRelationship::getStrength)
                .sum();

            long count = relationships
                .stream()
                .filter(r -> includeInactive || r.isActive())
                .count();

            return count > 0 ? totalStrength / count : 0.0;
        } catch (Exception e) {
            logger.error(
                "Error calculating average relationship strength for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return 0.0;
        }
    }

    // ========================================
    // GRAPH VALIDATION
    // ========================================

    @Override
    public List<String> findOrphanedRelationships(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            List<String> orphaned = new ArrayList<>();
            List<BeliefRelationship> relationships =
                relationshipService.getAllRelationships(agentId);

            for (BeliefRelationship relationship : relationships) {
                boolean sourceExists = beliefExists(
                    relationship.getSourceBeliefId(),
                    agentId
                );
                boolean targetExists = beliefExists(
                    relationship.getTargetBeliefId(),
                    agentId
                );

                if (!sourceExists || !targetExists) {
                    orphaned.add(relationship.getId());
                }
            }

            return orphaned;
        } catch (Exception e) {
            logger.error(
                "Error finding orphaned relationships for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> findSelfReferencingRelationships(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            return relationshipService
                .getAllRelationships(agentId)
                .stream()
                .filter(r ->
                    r.getSourceBeliefId().equals(r.getTargetBeliefId())
                )
                .map(BeliefRelationship::getId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                "Error finding self-referencing relationships for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> findTemporallyInvalidRelationships(String agentId) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            Instant now = Instant.now();
            return relationshipService
                .getAllRelationships(agentId)
                .stream()
                .filter(r -> {
                    if (
                        r.getEffectiveFrom() != null &&
                        r.getEffectiveFrom().isAfter(now)
                    ) {
                        return true; // Not yet effective
                    }
                    if (
                        r.getEffectiveUntil() != null &&
                        r.getEffectiveUntil().isBefore(now)
                    ) {
                        return true; // Already expired
                    }
                    return false;
                })
                .map(BeliefRelationship::getId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(
                "Error finding temporally invalid relationships for agent {}: {}",
                agentId,
                e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    // ========================================
    // ADVANCED GRAPH ANALYSIS
    // ========================================

    @Override
    public List<String> findDeprecatedBeliefIds(String agentId) {
        return getDeprecatedBeliefIds(agentId, 0);
    }

    @Override
    public List<String> findSupersedingBeliefIds(
        String beliefId,
        String agentId
    ) {
        return getSupersedingBeliefIds(beliefId, agentId);
    }

    @Override
    public List<String> findDeprecationChain(String beliefId, String agentId) {
        return getDeprecationChainIds(beliefId, agentId, 0);
    }

    @Override
    public Set<String> findRelatedBeliefIds(
        String beliefId,
        String agentId,
        int maxDepth
    ) {
        return getReachableBeliefIds(beliefId, agentId, maxDepth, null);
    }


    // ========================================
    // GRAPH SNAPSHOT CREATION
    // ========================================

    @Override
    public BeliefKnowledgeGraph createSnapshotGraph(
        String agentId,
        boolean includeInactive
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            // Delegate to relationship service
            return relationshipService.createSnapshotGraph(
                agentId,
                includeInactive
            );
        } catch (Exception e) {
            logger.error(
                "Error creating snapshot graph for agent {}: {}",
                agentId,
                e.getMessage()
            );
            throw new StorageException("Failed to create snapshot graph", e);
        }
    }

    @Override
    public BeliefKnowledgeGraph createFilteredSnapshotGraph(
        String agentId,
        Set<String> beliefIds,
        Set<RelationshipType> relationshipTypes,
        int maxBeliefs
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            // Delegate to relationship service
            return relationshipService.createFilteredSnapshot(
                agentId,
                beliefIds,
                relationshipTypes,
                maxBeliefs
            );
        } catch (Exception e) {
            logger.error(
                "Error creating filtered snapshot graph for agent {}: {}",
                agentId,
                e.getMessage()
            );
            throw new StorageException(
                "Failed to create filtered snapshot graph",
                e
            );
        }
    }

    @Override
    public BeliefKnowledgeGraph createExportGraph(
        String agentId,
        String format
    ) {
        Objects.requireNonNull(agentId, "Agent ID cannot be null");

        try {
            // Delegate to relationship service
            return relationshipService.createExportGraph(agentId, format);
        } catch (Exception e) {
            logger.error(
                "Error creating export graph for agent {}: {}",
                agentId,
                e.getMessage()
            );
            throw new StorageException("Failed to create export graph", e);
        }
    }
}
