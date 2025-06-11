package ai.headkey.rest;

import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.dto.BeliefKnowledgeGraph;
import ai.headkey.memory.enums.RelationshipType;
import ai.headkey.memory.interfaces.BeliefRelationshipService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.ApiResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * REST API resource for managing belief relationships in the knowledge graph.
 * 
 * This resource provides comprehensive endpoints for creating, querying, and managing
 * relationships between beliefs, enabling rich knowledge graph operations including
 * temporal deprecation, semantic connections, and graph analytics.
 * 
 * @since 1.0
 */
@Path("/api/v1/agents/{agentId}/belief-relationships")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Belief Relationships", description = "Manage relationships between beliefs in the knowledge graph")
public class BeliefRelationshipResource {
    
    @Inject
    BeliefRelationshipService beliefRelationshipService;
    
    /**
     * Creates a new relationship between two beliefs.
     */
    @POST
    @Operation(
        summary = "Create a new belief relationship",
        description = "Creates a directed relationship between two beliefs with specified type and strength"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Relationship created successfully",
        content = @Content(schema = @Schema(implementation = BeliefRelationship.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "404", description = "One or both beliefs not found")
    public Response createRelationship(
            @Parameter(description = "Agent ID") @PathParam("agentId") @NotBlank String agentId,
            @Valid CreateRelationshipRequest request) {
        
        try {
            BeliefRelationship relationship;
            
            if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
                relationship = beliefRelationshipService.createRelationshipWithMetadata(
                    request.getSourceBeliefId(),
                    request.getTargetBeliefId(),
                    request.getRelationshipType(),
                    request.getStrength(),
                    agentId,
                    request.getMetadata()
                );
            } else {
                relationship = beliefRelationshipService.createRelationship(
                    request.getSourceBeliefId(),
                    request.getTargetBeliefId(),
                    request.getRelationshipType(),
                    request.getStrength(),
                    agentId
                );
            }
            
            return Response.status(Response.Status.CREATED).entity(relationship).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    /**
     * Creates a temporal relationship with effective period.
     */
    @POST
    @Path("/temporal")
    @Operation(
        summary = "Create a temporal belief relationship",
        description = "Creates a relationship with specific effective from/until timestamps"
    )
    @ApiResponse(responseCode = "201", description = "Temporal relationship created successfully")
    public Response createTemporalRelationship(
            @PathParam("agentId") @NotBlank String agentId,
            @Valid CreateTemporalRelationshipRequest request) {
        
        try {
            BeliefRelationship relationship = beliefRelationshipService.createTemporalRelationship(
                request.getSourceBeliefId(),
                request.getTargetBeliefId(),
                request.getRelationshipType(),
                request.getStrength(),
                agentId,
                request.getEffectiveFrom(),
                request.getEffectiveUntil()
            );
            
            return Response.status(Response.Status.CREATED).entity(relationship).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    /**
     * Creates a deprecation relationship where a new belief supersedes an old one.
     */
    @POST
    @Path("/deprecate")
    @Operation(
        summary = "Deprecate a belief with a new one",
        description = "Creates a SUPERSEDES relationship to deprecate an old belief with a new one"
    )
    @ApiResponse(responseCode = "201", description = "Deprecation relationship created successfully")
    public Response deprecateBelief(
            @PathParam("agentId") @NotBlank String agentId,
            @Valid DeprecateBeliefRequest request) {
        
        try {
            BeliefRelationship relationship = beliefRelationshipService.deprecateBeliefWith(
                request.getOldBeliefId(),
                request.getNewBeliefId(),
                request.getReason(),
                agentId
            );
            
            return Response.status(Response.Status.CREATED).entity(relationship).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    /**
     * Gets a specific relationship by ID.
     */
    @GET
    @Path("/{relationshipId}")
    @Operation(summary = "Get relationship by ID")
    @ApiResponse(responseCode = "200", description = "Relationship found")
    @ApiResponse(responseCode = "404", description = "Relationship not found")
    public Response getRelationship(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("relationshipId") @NotBlank String relationshipId) {
        
        Optional<BeliefRelationship> relationship = beliefRelationshipService.findRelationshipById(relationshipId, agentId);
        
        if (relationship.isPresent()) {
            return Response.ok(relationship.get()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Relationship not found")).build();
        }
    }
    
    /**
     * Updates an existing relationship.
     */
    @PUT
    @Path("/{relationshipId}")
    @Operation(summary = "Update relationship")
    @ApiResponse(responseCode = "200", description = "Relationship updated successfully")
    @ApiResponse(responseCode = "404", description = "Relationship not found")
    public Response updateRelationship(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("relationshipId") @NotBlank String relationshipId,
            @Valid UpdateRelationshipRequest request) {
        
        try {
            BeliefRelationship relationship = beliefRelationshipService.updateRelationship(
                relationshipId,
                request.getStrength(),
                request.getMetadata()
            );
            
            return Response.ok(relationship).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    /**
     * Deactivates a relationship.
     */
    @PUT
    @Path("/{relationshipId}/deactivate")
    @Operation(summary = "Deactivate relationship")
    @ApiResponse(responseCode = "200", description = "Relationship deactivated successfully")
    @ApiResponse(responseCode = "404", description = "Relationship not found")
    public Response deactivateRelationship(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("relationshipId") @NotBlank String relationshipId) {
        
        boolean success = beliefRelationshipService.deactivateRelationship(relationshipId, agentId);
        
        if (success) {
            return Response.ok(Map.of("message", "Relationship deactivated successfully")).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Relationship not found")).build();
        }
    }
    
    /**
     * Reactivates a relationship.
     */
    @PUT
    @Path("/{relationshipId}/reactivate")
    @Operation(summary = "Reactivate relationship")
    @ApiResponse(responseCode = "200", description = "Relationship reactivated successfully")
    public Response reactivateRelationship(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("relationshipId") @NotBlank String relationshipId) {
        
        boolean success = beliefRelationshipService.reactivateRelationship(relationshipId, agentId);
        
        if (success) {
            return Response.ok(Map.of("message", "Relationship reactivated successfully")).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Relationship not found")).build();
        }
    }
    
    /**
     * Deletes a relationship permanently.
     */
    @DELETE
    @Path("/{relationshipId}")
    @Operation(summary = "Delete relationship")
    @ApiResponse(responseCode = "204", description = "Relationship deleted successfully")
    @ApiResponse(responseCode = "404", description = "Relationship not found")
    public Response deleteRelationship(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("relationshipId") @NotBlank String relationshipId) {
        
        boolean success = beliefRelationshipService.deleteRelationship(relationshipId, agentId);
        
        if (success) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Relationship not found")).build();
        }
    }
    
    /**
     * Gets all relationships for a specific belief.
     */
    @GET
    @Path("/belief/{beliefId}")
    @Operation(summary = "Get all relationships for a belief")
    @ApiResponse(responseCode = "200", description = "Relationships retrieved successfully")
    public Response getRelationshipsForBelief(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("beliefId") @NotBlank String beliefId) {
        
        List<BeliefRelationship> relationships = beliefRelationshipService.findRelationshipsForBelief(beliefId, agentId);
        return Response.ok(relationships).build();
    }
    
    /**
     * Gets outgoing relationships from a belief.
     */
    @GET
    @Path("/belief/{beliefId}/outgoing")
    @Operation(summary = "Get outgoing relationships from a belief")
    public Response getOutgoingRelationships(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("beliefId") @NotBlank String beliefId) {
        
        List<BeliefRelationship> relationships = beliefRelationshipService.findOutgoingRelationships(beliefId, agentId);
        return Response.ok(relationships).build();
    }
    
    /**
     * Gets incoming relationships to a belief.
     */
    @GET
    @Path("/belief/{beliefId}/incoming")
    @Operation(summary = "Get incoming relationships to a belief")
    public Response getIncomingRelationships(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("beliefId") @NotBlank String beliefId) {
        
        List<BeliefRelationship> relationships = beliefRelationshipService.findIncomingRelationships(beliefId, agentId);
        return Response.ok(relationships).build();
    }
    
    /**
     * Gets relationships by type.
     */
    @GET
    @Path("/type/{relationshipType}")
    @Operation(summary = "Get relationships by type")
    public Response getRelationshipsByType(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("relationshipType") @NotNull RelationshipType relationshipType) {
        
        List<BeliefRelationship> relationships = beliefRelationshipService.findRelationshipsByType(relationshipType, agentId);
        return Response.ok(relationships).build();
    }
    
    /**
     * Gets relationships between two specific beliefs.
     */
    @GET
    @Path("/between/{sourceBeliefId}/{targetBeliefId}")
    @Operation(summary = "Get relationships between two beliefs")
    public Response getRelationshipsBetween(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("sourceBeliefId") @NotBlank String sourceBeliefId,
            @PathParam("targetBeliefId") @NotBlank String targetBeliefId) {
        
        List<BeliefRelationship> relationships = beliefRelationshipService.findRelationshipsBetween(
            sourceBeliefId, targetBeliefId, agentId);
        return Response.ok(relationships).build();
    }
    
    /**
     * Gets deprecated beliefs.
     */
    @GET
    @Path("/deprecated")
    @Operation(summary = "Get deprecated beliefs")
    @ApiResponse(responseCode = "200", description = "Deprecated beliefs retrieved successfully")
    public Response getDeprecatedBeliefs(@PathParam("agentId") @NotBlank String agentId) {
        List<String> deprecatedBeliefs = beliefRelationshipService.findDeprecatedBeliefs(agentId);
        return Response.ok(deprecatedBeliefs).build();
    }
    
    /**
     * Gets beliefs that supersede a given belief.
     */
    @GET
    @Path("/belief/{beliefId}/superseding")
    @Operation(summary = "Get beliefs that supersede a given belief")
    public Response getSupersedingBeliefs(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("beliefId") @NotBlank String beliefId) {
        
        var superseding = beliefRelationshipService.findSupersedingBeliefs(beliefId, agentId);
        return Response.ok(superseding).build();
    }
    
    /**
     * Gets the deprecation chain for a belief.
     */
    @GET
    @Path("/belief/{beliefId}/deprecation-chain")
    @Operation(summary = "Get deprecation chain for a belief")
    public Response getDeprecationChain(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("beliefId") @NotBlank String beliefId) {
        
        var chain = beliefRelationshipService.findDeprecationChain(beliefId, agentId);
        return Response.ok(chain).build();
    }
    
    /**
     * Gets related beliefs within a certain depth.
     */
    @GET
    @Path("/belief/{beliefId}/related")
    @Operation(summary = "Get related beliefs")
    public Response getRelatedBeliefs(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("beliefId") @NotBlank String beliefId,
            @QueryParam("maxDepth") @DefaultValue("3") int maxDepth) {
        
        Set<String> relatedBeliefs = beliefRelationshipService.findRelatedBeliefs(beliefId, agentId, maxDepth);
        return Response.ok(relatedBeliefs).build();
    }
    
    /**
     * Gets the complete knowledge graph for an agent.
     * 
     * @deprecated Use /snapshot-graph or /efficient-statistics for better performance with large graphs
     */
    @GET
    @Path("/knowledge-graph")
    @Operation(summary = "Get complete knowledge graph", 
               description = "⚠️ DEPRECATED: Use /snapshot-graph for better performance")
    @ApiResponse(responseCode = "200", description = "Knowledge graph retrieved successfully")
    @Deprecated
    public Response getKnowledgeGraph(@PathParam("agentId") @NotBlank String agentId) {
        BeliefKnowledgeGraph graph = beliefRelationshipService.getKnowledgeGraph(agentId);
        return Response.ok(graph).build();
    }
    
    /**
     * Gets the active knowledge graph for an agent.
     * 
     * @deprecated Use /snapshot-graph?includeInactive=false for better performance
     */
    @GET
    @Path("/active-knowledge-graph")
    @Operation(summary = "Get active knowledge graph",
               description = "⚠️ DEPRECATED: Use /snapshot-graph?includeInactive=false for better performance")
    @Deprecated
    public Response getActiveKnowledgeGraph(@PathParam("agentId") @NotBlank String agentId) {
        BeliefKnowledgeGraph graph = beliefRelationshipService.getActiveKnowledgeGraph(agentId);
        return Response.ok(graph).build();
    }
    
    /**
     * Gets knowledge graph statistics.
     * 
     * @deprecated Use /efficient-statistics for better performance
     */
    @GET
    @Path("/statistics")
    @Operation(summary = "Get knowledge graph statistics",
               description = "⚠️ DEPRECATED: Use /efficient-statistics for better performance")
    @Deprecated
    public Response getKnowledgeGraphStatistics(@PathParam("agentId") @NotBlank String agentId) {
        Map<String, Object> statistics = beliefRelationshipService.getKnowledgeGraphStatistics(agentId);
        return Response.ok(statistics).build();
    }
    
    /**
     * Validates the knowledge graph structure.
     * 
     * @deprecated Use /efficient-validation for better performance
     */
    @GET
    @Path("/validate")
    @Operation(summary = "Validate knowledge graph structure",
               description = "⚠️ DEPRECATED: Use /efficient-validation for better performance")
    @Deprecated
    public Response validateKnowledgeGraph(@PathParam("agentId") @NotBlank String agentId) {
        List<String> issues = beliefRelationshipService.validateKnowledgeGraph(agentId);
        
        Map<String, Object> result = Map.of(
            "isValid", issues.isEmpty(),
            "issues", issues
        );
        
        return Response.ok(result).build();
    }
    
    /**
     * Finds belief clusters.
     */
    @GET
    @Path("/clusters")
    @Operation(summary = "Find belief clusters")
    public Response findBeliefClusters(
            @PathParam("agentId") @NotBlank String agentId,
            @QueryParam("strengthThreshold") @DefaultValue("0.8") double strengthThreshold) {
        
        Map<String, Set<String>> clusters = beliefRelationshipService.findBeliefClusters(agentId, strengthThreshold);
        return Response.ok(clusters).build();
    }
    
    /**
     * Finds potential conflicts in beliefs.
     */
    @GET
    @Path("/conflicts")
    @Operation(summary = "Find potential belief conflicts")
    public Response findPotentialConflicts(@PathParam("agentId") @NotBlank String agentId) {
        List<Map<String, Object>> conflicts = beliefRelationshipService.findPotentialConflicts(agentId);
        return Response.ok(conflicts).build();
    }
    
    /**
     * Finds shortest path between two beliefs.
     */
    @GET
    @Path("/path/{sourceBeliefId}/{targetBeliefId}")
    @Operation(summary = "Find shortest path between beliefs")
    public Response findShortestPath(
            @PathParam("agentId") @NotBlank String agentId,
            @PathParam("sourceBeliefId") @NotBlank String sourceBeliefId,
            @PathParam("targetBeliefId") @NotBlank String targetBeliefId) {
        
        List<BeliefRelationship> path = beliefRelationshipService.findShortestPath(sourceBeliefId, targetBeliefId, agentId);
        return Response.ok(path).build();
    }
    
    // ========================================
    // EFFICIENT OPERATIONS
    // ========================================
    
    /**
     * Gets comprehensive graph statistics using efficient database operations.
     */
    @GET
    @Path("/efficient-statistics")
    @Operation(summary = "Get comprehensive graph statistics (efficient)")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public Response getEfficientGraphStatistics(@PathParam("agentId") @NotBlank String agentId) {
        Map<String, Object> statistics = beliefRelationshipService.getEfficientGraphStatistics(agentId);
        return Response.ok(statistics).build();
    }
    
    /**
     * Validates graph structure using efficient database queries.
     */
    @GET
    @Path("/efficient-validation")
    @Operation(summary = "Validate knowledge graph structure (efficient)")
    @ApiResponse(responseCode = "200", description = "Validation completed successfully")
    public Response performEfficientGraphValidation(@PathParam("agentId") @NotBlank String agentId) {
        List<String> issues = beliefRelationshipService.performEfficientGraphValidation(agentId);
        
        Map<String, Object> result = Map.of(
            "isValid", issues.isEmpty(),
            "issues", issues,
            "issueCount", issues.size()
        );
        
        return Response.ok(result).build();
    }
    
    /**
     * Creates a lightweight snapshot graph for small datasets.
     */
    @GET
    @Path("/snapshot-graph")
    @Operation(summary = "Create snapshot graph for small datasets")
    @ApiResponse(responseCode = "200", description = "Snapshot graph created successfully")
    public Response createSnapshotGraph(
            @PathParam("agentId") @NotBlank String agentId,
            @QueryParam("includeInactive") @DefaultValue("false") boolean includeInactive) {
        
        BeliefKnowledgeGraph graph = beliefRelationshipService.createSnapshotGraph(agentId, includeInactive);
        return Response.ok(graph).build();
    }
    
    /**
     * Creates a filtered snapshot graph based on specific criteria.
     */
    @POST
    @Path("/filtered-snapshot")
    @Operation(summary = "Create filtered snapshot graph")
    @ApiResponse(responseCode = "200", description = "Filtered snapshot created successfully")
    public Response createFilteredSnapshot(
            @PathParam("agentId") @NotBlank String agentId,
            @QueryParam("maxBeliefs") @DefaultValue("1000") int maxBeliefs,
            FilterRequest filterRequest) {
        
        Set<String> beliefIds = filterRequest != null && filterRequest.getBeliefIds() != null 
            ? new HashSet<>(filterRequest.getBeliefIds()) : null;
        Set<RelationshipType> relationshipTypes = filterRequest != null && filterRequest.getRelationshipTypes() != null 
            ? new HashSet<>(filterRequest.getRelationshipTypes()) : null;
        
        BeliefKnowledgeGraph graph = beliefRelationshipService.createFilteredSnapshot(
            agentId, beliefIds, relationshipTypes, maxBeliefs);
        return Response.ok(graph).build();
    }
    
    /**
     * Creates a graph snapshot optimized for export operations.
     */
    @GET
    @Path("/export-graph")
    @Operation(summary = "Create export-optimized graph")
    @ApiResponse(responseCode = "200", description = "Export graph created successfully")
    public Response createExportGraph(
            @PathParam("agentId") @NotBlank String agentId,
            @QueryParam("format") @DefaultValue("json") String format) {
        
        BeliefKnowledgeGraph graph = beliefRelationshipService.createExportGraph(agentId, format);
        return Response.ok(graph).build();
    }
    
    /**
     * Filter request DTO for filtered snapshot operations.
     */
    public static class FilterRequest {
        private List<String> beliefIds;
        private List<RelationshipType> relationshipTypes;
        
        public List<String> getBeliefIds() { return beliefIds; }
        public void setBeliefIds(List<String> beliefIds) { this.beliefIds = beliefIds; }
        
        public List<RelationshipType> getRelationshipTypes() { return relationshipTypes; }
        public void setRelationshipTypes(List<RelationshipType> relationshipTypes) { this.relationshipTypes = relationshipTypes; }
    }
    
    /**
     * Exports the knowledge graph.
     */
    @GET
    @Path("/export")
    @Operation(summary = "Export knowledge graph")
    @Produces({MediaType.APPLICATION_JSON, "text/plain"})
    public Response exportKnowledgeGraph(
            @PathParam("agentId") @NotBlank String agentId,
            @QueryParam("format") @DefaultValue("json") String format) {
        
        try {
            String exported = beliefRelationshipService.exportKnowledgeGraph(agentId, format);
            
            String mediaType = format.equalsIgnoreCase("json") ? MediaType.APPLICATION_JSON : "text/plain";
            return Response.ok(exported, mediaType).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    /**
     * Performs cleanup on the knowledge graph.
     */
    @DELETE
    @Path("/cleanup")
    @Operation(summary = "Cleanup old inactive relationships")
    public Response cleanupKnowledgeGraph(
            @PathParam("agentId") @NotBlank String agentId,
            @QueryParam("olderThanDays") @DefaultValue("365") int olderThanDays) {
        
        int cleanedUp = beliefRelationshipService.cleanupKnowledgeGraph(agentId, olderThanDays);
        
        return Response.ok(Map.of(
            "message", "Cleanup completed successfully",
            "removedRelationships", cleanedUp
        )).build();
    }
    
    // Request DTOs
    
    public static class CreateRelationshipRequest {
        @NotBlank
        private String sourceBeliefId;
        
        @NotBlank
        private String targetBeliefId;
        
        @NotNull
        private RelationshipType relationshipType;
        
        private double strength = 1.0;
        
        private Map<String, Object> metadata;
        
        // Getters and setters
        public String getSourceBeliefId() { return sourceBeliefId; }
        public void setSourceBeliefId(String sourceBeliefId) { this.sourceBeliefId = sourceBeliefId; }
        
        public String getTargetBeliefId() { return targetBeliefId; }
        public void setTargetBeliefId(String targetBeliefId) { this.targetBeliefId = targetBeliefId; }
        
        public RelationshipType getRelationshipType() { return relationshipType; }
        public void setRelationshipType(RelationshipType relationshipType) { this.relationshipType = relationshipType; }
        
        public double getStrength() { return strength; }
        public void setStrength(double strength) { this.strength = strength; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    public static class CreateTemporalRelationshipRequest extends CreateRelationshipRequest {
        private Instant effectiveFrom;
        private Instant effectiveUntil;
        
        public Instant getEffectiveFrom() { return effectiveFrom; }
        public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }
        
        public Instant getEffectiveUntil() { return effectiveUntil; }
        public void setEffectiveUntil(Instant effectiveUntil) { this.effectiveUntil = effectiveUntil; }
    }
    
    public static class DeprecateBeliefRequest {
        @NotBlank
        private String oldBeliefId;
        
        @NotBlank
        private String newBeliefId;
        
        private String reason;
        
        public String getOldBeliefId() { return oldBeliefId; }
        public void setOldBeliefId(String oldBeliefId) { this.oldBeliefId = oldBeliefId; }
        
        public String getNewBeliefId() { return newBeliefId; }
        public void setNewBeliefId(String newBeliefId) { this.newBeliefId = newBeliefId; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    public static class UpdateRelationshipRequest {
        private double strength;
        private Map<String, Object> metadata;
        
        public double getStrength() { return strength; }
        public void setStrength(double strength) { this.strength = strength; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}