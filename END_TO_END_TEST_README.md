# End-to-End Belief Analysis Test

This guide explains how to run the complete end-to-end test for the HeadKey belief analysis system using the Dune Chapter 1 content. The test is implemented as a Python script for better structure, error handling, and extensibility.

## 🎯 What This Test Does

The end-to-end test demonstrates the complete HeadKey memory ingestion and belief analysis pipeline:

1. **Content Ingestion** - Processes the entire Dune Chapter 1 text
2. **Belief Extraction** - Identifies and creates beliefs from the literary content
3. **Belief Analysis** - Shows new beliefs, reinforced beliefs, conflicts, and relationships
4. **System Validation** - Verifies all components are working correctly

## 🚀 Quick Start

### Step 1: Start the HeadKey Application

```bash
# Start PostgreSQL database
docker compose up -d

# Start HeadKey application in development mode
./gradlew rest:quarkusDev
```

Wait for the startup message:
```
INFO  [io.quarkus] (Quarkus Main Thread) rest 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.23.2) started in X.XXXs. Listening on: http://localhost:8080
```

### Step 2: Run the End-to-End Test

In a **new terminal window**:

```bash
# Run the Python belief analysis test
python test_beliefs_e2e.py
```

Or with custom parameters:

```bash
# Run with custom agent ID and input file
python test_beliefs_e2e.py --agent-id my-agent --input-file data/input/my-file.md

# Run with different server URL
python test_beliefs_e2e.py --base-url http://localhost:9090

# Run with verbose output
python test_beliefs_e2e.py --verbose
```

## 📊 Expected Test Output

The test will show detailed output including:

### ✅ System Health Checks
- Server connectivity verification
- Database connection status
- Component initialization status

### 🧪 Dry Run Preview
```
🔍 Dry Run Belief Analysis Results:
   📋 New Beliefs: 15 beliefs
      • ID: belief-001 | Statement: Paul Atreides is the ducal heir | Confidence: 0.85
      • ID: belief-002 | Statement: Arrakis is known as Dune | Confidence: 0.95
      ...
   📋 Reinforced Beliefs: 3 beliefs
   📋 Weakened Beliefs: 0 beliefs
   ⚡ Conflicts Detected: 1
```

### 💾 Actual Ingestion Results
```
🎯 Actual Ingestion Belief Analysis Results:
   🆔 Memory ID: mem_20241211_001
   📋 New Beliefs Created: 15 beliefs
   📋 Reinforced Beliefs: 3 beliefs
   📊 Analysis Statistics:
      • Processing Time: 1,250ms
      • Total Beliefs Examined: 45
      • Overall Confidence: 0.87
```

### 📈 System Statistics
- Memory system performance metrics
- Belief analysis statistics
- Database usage information

## 🎨 Test Content

The test uses `data/input/dune-ch1.md` which contains:
- **Literary Content**: Frank Herbert's Dune Chapter 1
- **Character Information**: Paul Atreides, Jessica, Reverend Mother
- **World Building**: Arrakis, Caladan, spice melange
- **Plot Elements**: Gom jabbar test, dreams, political intrigue

This rich content demonstrates the system's ability to extract complex beliefs from narrative text.

## 🔧 Test Components

### 1. Input Validation
- Verifies content format and structure
- Validates agent ID and metadata
- Checks content length limits

### 2. Dry Run Analysis
- Previews belief extraction without storage
- Shows categorization results
- Demonstrates conflict detection

### 3. Full Ingestion Pipeline
- Complete memory encoding and storage
- Belief creation and relationship mapping
- Real-time conflict analysis

### 4. Result Verification
- Belief count validation
- Confidence score analysis
- Processing time metrics

## 🛠️ Configuration Options

### Command Line Arguments
```bash
# Change agent ID
python test_beliefs_e2e.py --agent-id your-custom-agent-id

# Use different input content
python test_beliefs_e2e.py --input-file data/input/your-file.md

# Change server URL
python test_beliefs_e2e.py --base-url http://localhost:9090

# Enable verbose output
python test_beliefs_e2e.py --verbose
```

### Multiple Configuration Example
```bash
# Test with custom settings
python test_beliefs_e2e.py \
    --agent-id production-agent \
    --input-file data/input/custom-content.md \
    --base-url http://localhost:8080 \
    --verbose
```

### Test Parameters
The script supports various test scenarios:
- **Dry Run Only**: Preview without storing
- **Full Pipeline**: Complete ingestion and analysis
- **Custom Metadata**: Additional content classification

## 📋 API Endpoints Used

The test exercises these key endpoints:

### Memory Ingestion
- `POST /api/v1/memory/validate` - Input validation
- `POST /api/v1/memory/dry-run` - Preview analysis
- `POST /api/v1/memory/ingest` - Full ingestion

### System Monitoring
- `GET /api/v1/memory/health` - Health checks
- `GET /api/v1/memory/statistics` - Performance metrics
- `GET /api/v1/system/config` - System configuration

### Belief Analysis
- `GET /api/v1/agents/{agentId}/belief-relationships` - Belief relationships

## 🔍 Troubleshooting

### Server Not Starting
```bash
# Check PostgreSQL
docker compose ps

# Check port availability
lsof -i :8080

# View server logs
./gradlew rest:quarkusDev --debug
```

### Test Failures
```bash
# Verify server health
curl http://localhost:8080/api/v1/memory/health

# Check input file
ls -la data/input/dune-ch1.md

# Run with verbose output
python test_beliefs_e2e.py --verbose

# Test with different parameters
python test_beliefs_e2e.py --agent-id debug-agent --verbose
```

### Database Issues
```bash
# Reset database
docker compose down -v
docker compose up -d

# Check database connection
./gradlew rest:quarkusDev -Dquarkus.hibernate-orm.log.sql=true
```

## 📚 Understanding the Results

### Belief Types
- **New Beliefs**: Created from novel information
- **Reinforced Beliefs**: Existing beliefs with increased confidence
- **Weakened Beliefs**: Beliefs with reduced confidence due to conflicts
- **Conflicts**: Contradictory information requiring resolution

### Confidence Scores
- **0.9-1.0**: High confidence, clear statements
- **0.7-0.9**: Good confidence, likely accurate
- **0.5-0.7**: Medium confidence, some uncertainty
- **0.0-0.5**: Low confidence, questionable accuracy

### Processing Metrics
- **Processing Time**: End-to-end pipeline duration
- **Beliefs Examined**: Total beliefs considered during analysis
- **Memory Efficiency**: Storage and retrieval performance

## 🧪 Advanced Testing

### Custom Content Testing
```bash
# Add your own content
cp my-document.md data/input/

# Run test with your content
python test_beliefs_e2e.py --input-file data/input/my-document.md
```

### Performance Testing
```bash
# Test with larger content
cat data/input/*.md > data/input/combined.md

# Run performance test
python test_beliefs_e2e.py --input-file data/input/combined.md --verbose
```

### Batch Testing
```bash
# Test multiple files
for file in data/input/*.md; do
    echo "Testing $file..."
    python test_beliefs_e2e.py --input-file "$file"
done
```

## 🎯 Success Criteria

A successful test run should show:
- ✅ Server starts without errors
- ✅ Database connections established
- ✅ Input validation passes
- ✅ Beliefs are extracted from content
- ✅ Processing completes within reasonable time
- ✅ No critical errors in logs
- ✅ Statistics show expected activity

## 🐍 Python Script Features

### Advantages over Shell Script
- **Better Error Handling**: Structured exception handling and detailed error messages
- **Colored Output**: ANSI color codes for improved readability
- **Command Line Arguments**: Flexible configuration without editing files
- **JSON Processing**: Native JSON parsing and formatting
- **Type Safety**: Python type hints for better code quality
- **Extensibility**: Easy to add new test cases and features

### Dependencies
The script uses only Python standard library modules:
- `requests` - For HTTP API calls
- `json` - For JSON processing
- `pathlib` - For file path handling
- `argparse` - For command line arguments

Install requests if not available:
```bash
pip install requests
```

## 📞 Support

If you encounter issues:
1. Check this troubleshooting guide
2. Review server logs for error details
3. Run with `--verbose` flag for detailed output
4. Verify all dependencies are installed (Python 3.7+, requests)
5. Ensure proper Java and database versions

## 🚀 Next Steps

After successful testing:
1. Explore the Swagger UI at `http://localhost:8080/swagger-ui`
2. Test with your own content: `python test_beliefs_e2e.py --input-file your-file.md`
3. Experiment with different agent configurations
4. Monitor system performance with larger datasets
5. Integrate with your applications using the REST API
6. Extend the Python script for automated testing workflows