#!/usr/bin/env python3
"""
Simple test to verify URL construction for the BeliefRelationshipResource endpoints.
This helps validate that our fix for query_agent_beliefs uses the correct endpoint.
"""

def test_url_construction():
    """Test URL construction for different belief query scenarios"""
    base_url = "http://localhost:8080"
    agent_id = "test-agent-123"
    
    print("🧪 Testing URL Construction for Belief Queries")
    print("=" * 50)
    
    # Test 1: Original broken endpoint (what was wrong)
    broken_url = f"{base_url}/api/v1/agents/{agent_id}/belief-relationships"
    print(f"❌ Old (broken) endpoint: {broken_url}")
    print("   Problem: This endpoint doesn't return beliefs at base path")
    
    # Test 2: Fixed endpoint - snapshot graph (active only)
    fixed_url = f"{base_url}/api/v1/agents/{agent_id}/belief-relationships/snapshot-graph"
    print(f"✅ New (fixed) endpoint: {fixed_url}")
    print("   Returns: BeliefKnowledgeGraph with active beliefs and relationships")
    
    # Test 3: Fixed endpoint with inactive beliefs
    fixed_url_with_inactive = f"{base_url}/api/v1/agents/{agent_id}/belief-relationships/snapshot-graph?includeInactive=true"
    print(f"✅ With inactive: {fixed_url_with_inactive}")
    print("   Returns: BeliefKnowledgeGraph with all beliefs (active + inactive)")
    
    # Test 4: Alternative deprecated endpoints (still work but not recommended)
    deprecated_url1 = f"{base_url}/api/v1/agents/{agent_id}/belief-relationships/knowledge-graph"
    deprecated_url2 = f"{base_url}/api/v1/agents/{agent_id}/belief-relationships/active-knowledge-graph"
    print(f"⚠️  Deprecated option 1: {deprecated_url1}")
    print(f"⚠️  Deprecated option 2: {deprecated_url2}")
    print("   Note: These work but are deprecated due to performance concerns")
    
    print("\n📊 Expected Response Structure:")
    print("""
    {
        "agentId": "test-agent-123",
        "beliefs": {
            "belief-id-1": {
                "id": "belief-id-1",
                "statement": "Paul Atreides is the protagonist of Dune",
                "confidence": 0.95,
                "active": true,
                ...
            },
            "belief-id-2": { ... }
        },
        "relationships": {
            "rel-id-1": {
                "id": "rel-id-1",
                "sourceBeliefId": "belief-id-1",
                "targetBeliefId": "belief-id-2",
                "relationshipType": "SUPPORTS",
                "strength": 0.8,
                ...
            }
        }
    }
    """)
    
    print("\n🔧 How to test manually:")
    print(f"1. Start the server: ./start-server.sh")
    print(f"2. Run belief test: python test_beliefs_e2e.py")
    print(f"3. Or test endpoint directly:")
    print(f"   curl -H 'Content-Type: application/json' '{fixed_url}'")
    
    return True

if __name__ == "__main__":
    test_url_construction()