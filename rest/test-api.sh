#!/bin/bash

# Test script for HeadKey Memory Ingestion REST API
# This script demonstrates the basic functionality of the API

set -e

BASE_URL="http://localhost:8080/api/v1/memory"

echo "🚀 Testing HeadKey Memory Ingestion REST API"
echo "=============================================="

# Function to make HTTP requests with proper formatting
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3

    echo "📡 $method $endpoint"
    if [ -n "$data" ]; then
        curl -s -X "$method" \
             -H "Content-Type: application/json" \
             -d "$data" \
             "$BASE_URL$endpoint" | jq '.' || echo "Response: $(curl -s -X "$method" -H "Content-Type: application/json" -d "$data" "$BASE_URL$endpoint")"
    else
        curl -s -X "$method" "$BASE_URL$endpoint" | jq '.' || echo "Response: $(curl -s -X "$method" "$BASE_URL$endpoint")"
    fi
    echo ""
}

# Check if the server is running
echo "🔍 Checking if server is running..."
if ! curl -s "$BASE_URL/health" > /dev/null; then
    echo "❌ Server is not running at $BASE_URL"
    echo "💡 Start the server with: ./gradlew :rest:quarkusDev"
    exit 1
fi
echo "✅ Server is running!"
echo ""

# Test 1: Health Check
echo "🏥 Test 1: Health Check"
make_request "GET" "/health"

# Test 2: Get Statistics
echo "📊 Test 2: Get Statistics"
make_request "GET" "/statistics"

# Test 3: Validate Input
echo "✅ Test 3: Validate Input"
validate_data='{
    "agent_id": "test-user-001",
    "content": "This is a test memory for validation",
    "source": "api"
}'
make_request "POST" "/validate" "$validate_data"

# Test 4: Dry Run Ingestion
echo "🧪 Test 4: Dry Run Memory Ingestion"
dry_run_data='{
    "agent_id": "test-user-001",
    "content": "I just learned how to use the HeadKey API effectively",
    "source": "api",
    "metadata": {
        "importance": "medium",
        "tags": ["learning", "api"],
        "confidence": 0.9
    },
    "dry_run": true
}'
make_request "POST" "/ingest" "$dry_run_data"

# Test 5: Actual Memory Ingestion
echo "💾 Test 5: Actual Memory Ingestion"
ingest_data='{
    "agent_id": "test-user-001",
    "content": "I successfully tested the HeadKey Memory Ingestion API",
    "source": "api",
    "metadata": {
        "importance": "high",
        "tags": ["success", "testing", "api"],
        "confidence": 1.0,
        "test_run": true
    }
}'
MEMORY_RESPONSE=$(make_request "POST" "/ingest" "$ingest_data")
echo "$MEMORY_RESPONSE"

# Extract memory ID from response for further testing
MEMORY_ID=$(echo "$MEMORY_RESPONSE" | jq -r '.memory_id // empty' 2>/dev/null)
if [ -n "$MEMORY_ID" ]; then
    echo "🎯 Memory successfully created with ID: $MEMORY_ID"
else
    echo "⚠️  Memory ID not found in response"
fi
echo ""

# Test 6: Error Case - Invalid Input
echo "❌ Test 6: Error Handling - Invalid Input"
invalid_data='{
    "agent_id": "",
    "content": "",
    "source": "invalid_source"
}'
make_request "POST" "/ingest" "$invalid_data"

# Test 7: Error Case - Missing Required Fields
echo "❌ Test 7: Error Handling - Missing Fields"
missing_data='{
    "content": "Content without agent ID"
}'
make_request "POST" "/ingest" "$missing_data"

# Test 8: Content with Special Characters
echo "🌍 Test 8: Unicode and Special Characters"
unicode_data='{
    "agent_id": "test-user-unicode",
    "content": "Special chars: !@#$%^&*() and Unicode: 你好世界 🚀 ✨",
    "source": "api",
    "metadata": {
        "encoding_test": true
    }
}'
make_request "POST" "/ingest" "$unicode_data"

# Test 9: Large Content (within limits)
echo "📄 Test 9: Large Content Handling"
large_content="This is a test with longer content. "
for i in {1..50}; do
    large_content+="This sentence is repeated to create larger content for testing purposes. "
done

large_data=$(cat <<EOF
{
    "agent_id": "test-user-large",
    "content": "$large_content",
    "source": "api",
    "metadata": {
        "size_test": true,
        "estimated_length": ${#large_content}
    }
}
EOF
)
make_request "POST" "/ingest" "$large_data"

# Final Statistics
echo "📈 Final Statistics After Testing"
make_request "GET" "/statistics"

echo "🎉 API Testing Complete!"
echo "========================"
echo ""
echo "💡 Tips:"
echo "   - View API documentation at: http://localhost:8080/swagger-ui"
echo "   - Check OpenAPI spec at: http://localhost:8080/openapi"
echo "   - Monitor health at: http://localhost:8080/api/v1/memory/health"
echo ""
echo "🔧 Requirements:"
echo "   - curl (for HTTP requests)"
echo "   - jq (for JSON formatting - optional but recommended)"
echo "   - Server running on http://localhost:8080"
