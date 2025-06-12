# Test Content for Belief Extraction Debugging

This document contains test content examples that should trigger belief extraction in the system. Use these to test if the belief extraction pipeline is working correctly.

## Simple Preference Beliefs

These should trigger the preference extraction patterns in `SimplePatternBeliefExtractionService`:

### Favorite Pattern
```
My favorite color is blue
I love pizza
My favorite food is sushi
```

### Like Pattern
```
I like chocolate
I enjoy reading books
I love playing guitar
```

### Dislike Pattern
```
I dislike broccoli
I hate rainy days
I don't like spicy food
```

## Fact-Based Beliefs

These should trigger the fact extraction patterns:

### Is/Are Pattern
```
The sky is blue
Dogs are loyal animals
My car is red
The weather is nice today
```

### Was/Were Pattern
```
Yesterday was sunny
The meeting was productive
The cats were sleeping
```

## Relationship Beliefs

These should trigger relationship extraction:

### Friend Pattern
```
John is my friend
Sarah knows my brother
Mike is married to Lisa
```

### Family Pattern
```
Tom is my brother
Lisa is related to John
```

## Location Beliefs

These should trigger location extraction:

### Lives Pattern
```
I live in New York
John lives in California
She lives near the beach
```

### From Pattern
```
I am from Texas
He is from Germany
```

### Located Pattern
```
The store is located downtown
My office is located on Main Street
```

## Complex Content Examples

These are more realistic examples that might contain multiple beliefs:

### Personal Information
```
Hi, I'm Sarah and I live in Seattle. My favorite food is Italian cuisine, especially pasta. I work as a software engineer and I really enjoy coding. I don't like crowded places, but I love hiking in the mountains on weekends.
```

### Preferences and Facts
```
I prefer tea over coffee. My dog Max is a golden retriever and he is very friendly. I usually work from home, but sometimes I go to the office downtown. I don't like working late hours.
```

### Social Information
```
Yesterday I met with my friend Alex for lunch. We went to that new restaurant on 5th Street. The food was amazing - I especially loved their chocolate cake. Alex told me he is moving to Portland next month.
```

## Expected Belief Extractions

When testing with the above content, you should see beliefs extracted like:

### From "My favorite color is blue"
- Type: preference
- Statement: "User has preference: favorite color is blue"
- Tags: preference, favorite

### From "I like chocolate"
- Type: preference  
- Statement: "User likes: chocolate"
- Tags: preference, like

### From "The sky is blue"
- Type: fact
- Statement: "Fact: the sky is blue"
- Tags: fact

### From "John is my friend"
- Type: relationship
- Statement: "Relationship: john is my friend"
- Tags: relationship

### From "I live in New York"
- Type: location
- Statement: "Location: i live in new york"
- Tags: location

## Debugging Checklist

When testing belief extraction, check the logs for:

1. **Service Selection**: Which BeliefExtractionService is being used?
   - Look for: "using SimplePatternBeliefExtractionService" vs "Creating LangChain4JBeliefExtractionService"

2. **Content Processing**: Is the content being processed?
   - Look for: "Extracting beliefs from content: '...' for agent: ..."

3. **Pattern Matching**: Are the patterns matching?
   - Look for: "Preference pattern match: true/false"
   - Look for: "Fact pattern match: true/false"
   - Look for: "Relationship pattern match: true/false"
   - Look for: "Location pattern match: true/false"

4. **Belief Creation**: Are beliefs being created?
   - Look for: "Added [type] belief: ..."
   - Look for: "Extracted X beliefs from content"

5. **Belief Analysis**: Is the analysis working?
   - Look for: "Belief analysis result: X new beliefs, Y reinforced beliefs"

6. **Configuration**: Is belief analysis enabled?
   - Look for: "enableBeliefAnalysis: true"

## Common Issues and Solutions

### No Beliefs Extracted
- Check if content matches the simple patterns
- Verify that content is lowercase normalized correctly
- Ensure the regex patterns are working

### Patterns Not Matching
- The SimplePatternBeliefExtractionService uses very specific patterns
- Content must contain exact keywords: "favorite", "like", "is", "lives", etc.
- Try the exact test phrases above first

### API Key Issues
- If using LangChain4J, ensure OPENAI_API_KEY environment variable is set
- Check logs for "OPENAI_API_KEY present: YES/NO"

### Transaction Issues
- Beliefs might be created but not committed to database
- Check if the JPA transaction boundaries are correct

## Testing Commands

### Test with cURL
```bash
curl -X POST http://localhost:8080/api/memory/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "test-agent-001",
    "content": "My favorite color is blue and I like pizza",
    "source": "test"
  }'
```

### Expected Response
```json
{
  "memoryId": "mem_...",
  "agentId": "test-agent-001",
  "status": "SUCCESS",
  "encodedSuccessfully": true,
  "beliefUpdateResult": {
    "newBeliefs": [
      {
        "statement": "User has preference: favorite color is blue",
        "category": "preference"
      },
      {
        "statement": "User likes: pizza", 
        "category": "preference"
      }
    ]
  }
}
```

## Debugging Steps

1. **Start with Simple Content**: Use exact phrases like "I like pizza"
2. **Check Logs**: Enable debug logging and watch for the patterns above
3. **Verify Service**: Confirm which extraction service is being used
4. **Test Patterns**: Use the regex patterns directly to test content
5. **Check Database**: Verify beliefs are being saved to the database
6. **Test API Key**: If using OpenAI, verify the API key is working

Remember: The SimplePatternBeliefExtractionService is very basic and requires exact keyword matches. For production use, consider configuring the LangChain4J service with a proper OpenAI API key.