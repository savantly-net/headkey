# BeliefRelationshipResource Query Fix

## Problem Description

The `query_agent_beliefs` method in `test_beliefs_e2e.py` was failing because it was trying to access the base URL of the `BeliefRelationshipResource` (`/api/v1/agents/{agentId}/belief-relationships`), but this resource doesn't provide a simple list endpoint at its base path.

### Original Issue
```python
# This was the problematic code:
belief_url = f"{self.base_url}/api/v1/agents/{self.agent_id}/belief-relationships"
```

The `BeliefRelationshipResource` is designed for managing relationships between beliefs, not for listing beliefs directly. Its base path doesn't return belief data.

## Solution

### 1. Updated Endpoint Usage

Changed from the base endpoint to the snapshot graph endpoint:

```python
# Fixed code:
belief_url = f"{self.base_url}/api/v1/agents/{self.agent_id}/belief-relationships/snapshot-graph"
```

### 2. Available Endpoints for Belief Queries

The `BeliefRelationshipResource` provides several endpoints that can return belief data:

| Endpoint | Status | Description | Use Case |
|----------|--------|-------------|----------|
| `/snapshot-graph` | ✅ **Recommended** | Lightweight snapshot for small datasets | < 1000 beliefs |
| `/knowledge-graph` | ⚠️ **Deprecated** | Complete knowledge graph | Legacy support |
| `/active-knowledge-graph` | ⚠️ **Deprecated** | Active beliefs only | Legacy support |

### 3. Data Structure Changes

The snapshot graph endpoint returns a `BeliefKnowledgeGraph` structure:

```json
{
  "agentId": "agent-123",
  "beliefs": {
    "belief-id-1": {
      "id": "belief-id-1",
      "statement": "Paul Atreides is the protagonist",
      "confidence": 0.95,
      "active": true,
      "createdAt": "2024-01-01T10:00:00Z"
    }
  },
  "relationships": {
    "rel-id-1": {
      "id": "rel-id-1",
      "sourceBeliefId": "belief-id-1",
      "targetBeliefId": "belief-id-2",
      "relationshipType": "SUPPORTS",
      "strength": 0.8
    }
  }
}
```

## Implementation Changes

### 1. Updated `query_agent_beliefs` Method

```python
def query_agent_beliefs(self, include_inactive: bool = False) -> Optional[Dict]:
    """Query beliefs for the agent using the knowledge graph snapshot"""
    self.print_section("🔍 Querying Agent's Beliefs")
    print(f"Checking beliefs for agent: {self.agent_id}")
    
    # Use the snapshot-graph endpoint to get beliefs and relationships
    belief_url = f"{self.base_url}/api/v1/agents/{self.agent_id}/belief-relationships/snapshot-graph"
    if include_inactive:
        belief_url += "?includeInactive=true"
        print("Including inactive beliefs and relationships")
    
    try:
        req = urllib.request.Request(belief_url, headers=self.headers)
        with urllib.request.urlopen(req) as response:
            if response.status == 200:
                response_data = response.read().decode('utf-8')
                response_json = json.loads(response_data)
                self.print_success("Retrieved agent's knowledge graph snapshot")
                print(json.dumps(response_json, indent=2))
                return response_json
            else:
                self.print_warning("Knowledge graph snapshot endpoint not available or accessible")
                return None
    except Exception as e:
        self.print_warning(f"Knowledge graph snapshot endpoint not available or accessible: {str(e)}")
        return None
```

### 2. Added Knowledge Graph Display Method

```python
def display_knowledge_graph(self, knowledge_graph: Optional[Dict]):
    """Display beliefs and relationships from a knowledge graph structure"""
    if not knowledge_graph:
        print("   📋 Knowledge Graph: No data available")
        return
        
    beliefs = knowledge_graph.get('beliefs', {})
    relationships = knowledge_graph.get('relationships', {})
    
    if not beliefs:
        print("   📋 Knowledge Graph: 0 beliefs")
        return
        
    count = len(beliefs)
    print(f"   📋 Knowledge Graph: {count} beliefs")
    
    for belief_id, belief in beliefs.items():
        statement = belief.get('statement', 'N/A')
        confidence = belief.get('confidence', 'N/A')
        active = belief.get('active', True)
        status = "Active" if active else "Inactive"
        print(f"      • ID: {belief_id} | Statement: {statement} | Confidence: {confidence} | Status: {status}")
        
    if relationships:
        rel_count = len(relationships)
        print(f"   🔗 Relationships: {rel_count}")
```

### 3. Updated Test Flow

```python
def run_full_test(self):
    # ... existing steps ...
    
    # Step 6: Query agent beliefs
    knowledge_graph = self.query_agent_beliefs()
    if knowledge_graph:
        self.display_knowledge_graph(knowledge_graph)
    
    # ... rest of test ...
    
    # Step 8: Print summary
    self.print_test_summary(len(content), dry_run_result, ingest_result, knowledge_graph)
```

### 4. Enhanced Test Summary

Added knowledge graph statistics to the test summary:

```python
# Display knowledge graph stats if available
if knowledge_graph:
    beliefs = knowledge_graph.get('beliefs', {})
    relationships = knowledge_graph.get('relationships', {})
    print(f"\n🕸️ Knowledge Graph Status:")
    print(f"   • Total Beliefs in Graph: {len(beliefs)}")
    print(f"   • Total Relationships: {len(relationships)}")
```

## Additional Features

### Query Parameters Support

The fix includes support for the `includeInactive` query parameter:

```python
# Include inactive beliefs and relationships
knowledge_graph = self.query_agent_beliefs(include_inactive=True)
```

### Backward Compatibility

The original `display_beliefs` method is preserved for handling individual belief lists from ingestion results:

```python
def display_beliefs(self, belief_list: List[Dict], belief_type: str):
    """Display belief details in a formatted way"""
    # Used for ingestion result analysis (new_beliefs, reinforced_beliefs, etc.)
```

## Testing

### Manual Testing

1. Start the server:
   ```bash
   ./start-server.sh
   ```

2. Run the belief test:
   ```bash
   python test_beliefs_e2e.py
   ```

3. Test endpoint directly:
   ```bash
   curl -H 'Content-Type: application/json' \
     'http://localhost:8080/api/v1/agents/test-agent/belief-relationships/snapshot-graph'
   ```

### URL Construction Test

A test utility `test_url_construction.py` was created to verify proper URL construction:

```bash
python test_url_construction.py
```

## Best Practices Applied

1. **Use Recommended Endpoints**: Chose `snapshot-graph` over deprecated alternatives
2. **Performance Optimization**: Snapshot endpoint is optimized for small datasets
3. **Error Handling**: Added proper exception handling with descriptive messages
4. **Backward Compatibility**: Preserved existing functionality for ingestion analysis
5. **Documentation**: Clear method documentation and parameter descriptions
6. **Flexibility**: Added optional parameters for different query scenarios

## Future Considerations

1. **Large Datasets**: For graphs with > 1000 beliefs, consider using the filtered snapshot endpoints:
   - `/filtered-snapshot-graph`
   - `/export-graph`

2. **Performance Monitoring**: Monitor response times for large knowledge graphs

3. **Caching**: Consider implementing client-side caching for frequently accessed graphs

4. **Pagination**: Future enhancement could add pagination support for very large belief sets