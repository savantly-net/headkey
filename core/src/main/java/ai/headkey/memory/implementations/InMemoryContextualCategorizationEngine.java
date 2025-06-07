package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the Contextual Categorization Engine (CCE).
 * 
 * This implementation provides rule-based categorization using keyword matching
 * and simple heuristics. It's designed for development, testing, and demonstration
 * purposes. The categorization logic is configurable and can be extended with
 * additional rules and categories.
 * 
 * Note: This implementation uses simple pattern matching and does not include
 * advanced NLP or machine learning capabilities that would be present in a
 * production system.
 * 
 * @since 1.0
 */
public class InMemoryContextualCategorizationEngine implements ContextualCategorizationEngine {
    
    private volatile double confidenceThreshold = 0.6;
    
    // Predefined categories and their keywords
    private final Map<String, Set<String>> categoryKeywords;
    private final Map<String, Set<String>> categorySubcategories;
    private final Set<String> availableCategories;
    
    // Pattern for extracting common entities
    private final Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private final Pattern phonePattern = Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b|\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b");
    private final Pattern urlPattern = Pattern.compile("https?://[\\w\\.-]+\\.[a-zA-Z]{2,}[\\w\\.-]*/?[\\w\\.-?=%&]*");
    private final Pattern datePattern = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b|\\b\\d{4}-\\d{2}-\\d{2}\\b");
    private final Pattern timePattern = Pattern.compile("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:AM|PM)?\\b");
    private final Pattern numberPattern = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b");
    
    // Statistics tracking
    private long totalCategorizations = 0;
    private long totalBatchCategorizations = 0;
    private long totalTagExtractions = 0;
    private final Map<String, Long> categoryFrequency = new ConcurrentHashMap<>();
    private final Map<String, Double> categoryConfidenceSum = new ConcurrentHashMap<>();
    
    /**
     * Creates a new in-memory categorization engine with default categories.
     */
    public InMemoryContextualCategorizationEngine() {
        this.categoryKeywords = new ConcurrentHashMap<>();
        this.categorySubcategories = new ConcurrentHashMap<>();
        this.availableCategories = ConcurrentHashMap.newKeySet();
        
        initializeDefaultCategories();
    }
    
    /**
     * Initializes the default categories and their associated keywords.
     */
    private void initializeDefaultCategories() {
        // Personal category
        addCategory("personal", 
            Set.of("family", "friend", "birthday", "anniversary", "personal", "private", "emotion", 
                   "feeling", "love", "relationship", "spouse", "child", "parent", "sibling"),
            Set.of("family", "friends", "emotions", "relationships", "memories"));
        
        // Work category
        addCategory("work", 
            Set.of("meeting", "project", "deadline", "task", "colleague", "boss", "office", 
                   "business", "professional", "work", "job", "career", "client", "customer"),
            Set.of("meetings", "projects", "tasks", "colleagues", "business"));
        
        // Knowledge category
        addCategory("knowledge", 
            Set.of("fact", "information", "learn", "study", "research", "data", "knowledge", 
                   "education", "science", "history", "technology", "definition", "concept"),
            Set.of("facts", "research", "education", "science", "technology"));
        
        // Event category
        addCategory("event", 
            Set.of("event", "conference", "party", "celebration", "appointment", "schedule", 
                   "calendar", "meeting", "gathering", "ceremony", "festival"),
            Set.of("conferences", "parties", "appointments", "ceremonies", "festivals"));
        
        // Location category
        addCategory("location", 
            Set.of("place", "location", "address", "city", "country", "restaurant", "store", 
                   "building", "street", "map", "directions", "travel", "destination"),
            Set.of("places", "addresses", "travel", "restaurants", "stores"));
        
        // Health category
        addCategory("health", 
            Set.of("health", "medical", "doctor", "hospital", "medicine", "symptom", "treatment", 
                   "exercise", "fitness", "diet", "nutrition", "wellness", "therapy"),
            Set.of("medical", "fitness", "nutrition", "wellness", "therapy"));
        
        // Finance category
        addCategory("finance", 
            Set.of("money", "finance", "budget", "expense", "income", "investment", "bank", 
                   "payment", "bill", "cost", "price", "financial", "economic"),
            Set.of("budget", "expenses", "investments", "banking", "payments"));
        
        // Entertainment category
        addCategory("entertainment", 
            Set.of("movie", "music", "book", "game", "entertainment", "hobby", "sport", "fun", 
                   "leisure", "recreation", "tv", "show", "concert", "performance"),
            Set.of("movies", "music", "books", "games", "sports", "hobbies"));
        
        // Communication category
        addCategory("communication", 
            Set.of("email", "phone", "message", "call", "text", "communication", "conversation", 
                   "chat", "discuss", "talk", "contact", "correspondence"),
            Set.of("emails", "messages", "calls", "conversations", "contacts"));
        
        // Default/General category for unclassified content
        addCategory("general", 
            Set.of("note", "reminder", "thought", "idea", "misc", "other", "general"),
            Set.of("notes", "reminders", "thoughts", "ideas", "misc"));
    }
    
    /**
     * Adds a new category with its keywords and subcategories.
     */
    private void addCategory(String category, Set<String> keywords, Set<String> subcategories) {
        availableCategories.add(category);
        categoryKeywords.put(category, new HashSet<>(keywords));
        categorySubcategories.put(category, new HashSet<>(subcategories));
    }
    
    @Override
    public CategoryLabel categorize(String content, Metadata meta) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        
        String processedContent = content.toLowerCase().trim();
        String bestCategory = "general";
        String bestSubcategory = null;
        double bestConfidence = 0.0;
        Set<String> extractedTags = new HashSet<>();
        
        // Check metadata for hints
        if (meta != null) {
            if (meta.getTags() != null) {
                extractedTags.addAll(meta.getTags());
            }
            
            // Check for explicit category hint in metadata
            if (meta.getProperties() != null && meta.getProperties().containsKey("categoryHint")) {
                String hint = meta.getProperties().get("categoryHint").toString().toLowerCase();
                if (availableCategories.contains(hint)) {
                    bestCategory = hint;
                    bestConfidence = 0.9; // High confidence for explicit hints
                }
            }
        }
        
        // If no explicit hint, perform keyword-based categorization
        if (bestConfidence < confidenceThreshold) {
            for (String category : availableCategories) {
                double score = calculateCategoryScore(processedContent, category);
                if (score > bestConfidence) {
                    bestConfidence = score;
                    bestCategory = category;
                    bestSubcategory = findBestSubcategory(processedContent, category);
                }
            }
        }
        
        // Extract additional tags from content
        extractedTags.addAll(extractTags(content));
        
        // Apply confidence threshold
        if (bestConfidence < confidenceThreshold) {
            bestCategory = "general";
            bestSubcategory = "unclassified";
            bestConfidence = 0.5; // Default confidence for general category
        }
        
        // Update statistics
        totalCategorizations++;
        categoryFrequency.merge(bestCategory, 1L, Long::sum);
        categoryConfidenceSum.merge(bestCategory, bestConfidence, Double::sum);
        
        return new CategoryLabel(bestCategory, bestSubcategory, extractedTags, bestConfidence);
    }
    
    @Override
    public Set<String> extractTags(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        
        Set<String> tags = new HashSet<>();
        
        // Extract emails
        emailPattern.matcher(content).results()
            .forEach(match -> tags.add("email:" + match.group()));
        
        // Extract phone numbers
        phonePattern.matcher(content).results()
            .forEach(match -> tags.add("phone:" + match.group()));
        
        // Extract URLs
        urlPattern.matcher(content).results()
            .forEach(match -> tags.add("url:" + match.group()));
        
        // Extract dates
        datePattern.matcher(content).results()
            .forEach(match -> tags.add("date:" + match.group()));
        
        // Extract times
        timePattern.matcher(content).results()
            .forEach(match -> tags.add("time:" + match.group()));
        
        // Extract numbers
        numberPattern.matcher(content).results()
            .limit(5) // Limit to avoid too many number tags
            .forEach(match -> tags.add("number:" + match.group()));
        
        // Extract key phrases (simple approach - look for important words)
        String[] words = content.toLowerCase().split("\\s+");
        for (String word : words) {
            word = word.replaceAll("[^a-zA-Z0-9]", "");
            if (word.length() > 4 && isImportantWord(word)) {
                tags.add("keyword:" + word);
            }
        }
        
        totalTagExtractions++;
        return tags;
    }
    
    @Override
    public Map<String, CategoryLabel> categorizeBatch(Map<String, String> contentItems, Metadata commonMeta) {
        if (contentItems == null || contentItems.isEmpty()) {
            throw new IllegalArgumentException("Content items cannot be null or empty");
        }
        
        Map<String, CategoryLabel> results = new HashMap<>();
        
        for (Map.Entry<String, String> entry : contentItems.entrySet()) {
            try {
                CategoryLabel label = categorize(entry.getValue(), commonMeta);
                results.put(entry.getKey(), label);
            } catch (Exception e) {
                // Create a default label for failed categorizations
                CategoryLabel defaultLabel = new CategoryLabel("general", "error", 
                    Set.of("error:categorization_failed"), 0.1);
                results.put(entry.getKey(), defaultLabel);
            }
        }
        
        totalBatchCategorizations++;
        return results;
    }
    
    @Override
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    @Override
    public void setConfidenceThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Confidence threshold must be between 0.0 and 1.0");
        }
        this.confidenceThreshold = threshold;
    }
    
    @Override
    public List<String> getAvailableCategories() {
        return new ArrayList<>(availableCategories);
    }
    
    @Override
    public Set<String> getSubcategories(String primaryCategory) {
        if (primaryCategory == null) {
            throw new IllegalArgumentException("Primary category cannot be null");
        }
        
        Set<String> subcategories = categorySubcategories.get(primaryCategory);
        if (subcategories == null) {
            throw new IllegalArgumentException("Primary category not supported: " + primaryCategory);
        }
        
        return new HashSet<>(subcategories);
    }
    
    @Override
    public List<CategoryLabel> suggestAlternativeCategories(String content, Metadata meta, int maxSuggestions) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        if (maxSuggestions < 1) {
            throw new IllegalArgumentException("Max suggestions must be at least 1");
        }
        
        String processedContent = content.toLowerCase().trim();
        List<CategoryScore> scores = new ArrayList<>();
        
        // Calculate scores for all categories
        for (String category : availableCategories) {
            double score = calculateCategoryScore(processedContent, category);
            if (score > 0.1) { // Only include categories with some relevance
                String subcategory = findBestSubcategory(processedContent, category);
                Set<String> tags = extractTags(content);
                scores.add(new CategoryScore(category, subcategory, tags, score));
            }
        }
        
        // Sort by confidence (descending) and take top suggestions
        return scores.stream()
            .sorted((a, b) -> Double.compare(b.confidence, a.confidence))
            .limit(maxSuggestions)
            .map(cs -> new CategoryLabel(cs.category, cs.subcategory, cs.tags, cs.confidence))
            .collect(Collectors.toList());
    }
    
    @Override
    public void provideFeedback(String content, CategoryLabel assignedCategory, CategoryLabel correctCategory) {
        if (content == null || assignedCategory == null || correctCategory == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        }
        
        // In a production system, this would update ML models or adjust weights
        // For this in-memory implementation, we could adjust keyword weights or add new keywords
        // For now, we'll just track the feedback for potential future improvements
        
        String correctCat = correctCategory.getPrimary();
        if (!availableCategories.contains(correctCat)) {
            // Add new category if it doesn't exist
            availableCategories.add(correctCat);
            categoryKeywords.put(correctCat, new HashSet<>());
            categorySubcategories.put(correctCat, new HashSet<>());
        }
        
        // Extract keywords from content and associate with correct category
        String[] words = content.toLowerCase().split("\\s+");
        Set<String> keywords = categoryKeywords.get(correctCat);
        for (String word : words) {
            word = word.replaceAll("[^a-zA-Z0-9]", "");
            if (word.length() > 3 && isImportantWord(word)) {
                keywords.add(word);
            }
        }
    }
    
    @Override
    public Map<String, Object> getCategorizationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalCategorizations", totalCategorizations);
        stats.put("totalBatchCategorizations", totalBatchCategorizations);
        stats.put("totalTagExtractions", totalTagExtractions);
        stats.put("availableCategories", availableCategories.size());
        stats.put("confidenceThreshold", confidenceThreshold);
        
        // Category frequency distribution
        Map<String, Long> frequencyMap = new HashMap<>(categoryFrequency);
        stats.put("categoryFrequency", frequencyMap);
        
        // Average confidence by category
        Map<String, Double> avgConfidence = new HashMap<>();
        for (String category : categoryFrequency.keySet()) {
            double sum = categoryConfidenceSum.getOrDefault(category, 0.0);
            long count = categoryFrequency.get(category);
            avgConfidence.put(category, count > 0 ? sum / count : 0.0);
        }
        stats.put("averageConfidenceByCategory", avgConfidence);
        
        // Most popular category
        String mostPopular = categoryFrequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none");
        stats.put("mostPopularCategory", mostPopular);
        
        return stats;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Basic health checks
            if (categoryKeywords == null || categorySubcategories == null || availableCategories == null) {
                return false;
            }
            
            // Check that we have at least some categories configured
            if (availableCategories.isEmpty()) {
                return false;
            }
            
            // Check confidence threshold is in valid range
            if (confidenceThreshold < 0.0 || confidenceThreshold > 1.0) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Calculates a confidence score for a given category based on keyword matching.
     */
    private double calculateCategoryScore(String content, String category) {
        Set<String> keywords = categoryKeywords.get(category);
        if (keywords == null || keywords.isEmpty()) {
            return 0.0;
        }
        
        int matches = 0;
        int totalKeywords = keywords.size();
        
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                matches++;
            }
        }
        
        // Base score on match ratio
        double baseScore = (double) matches / totalKeywords;
        
        // Boost score for exact keyword matches
        if (matches > 0) {
            baseScore = Math.min(1.0, baseScore + 0.2);
        }
        
        return baseScore;
    }
    
    /**
     * Finds the best subcategory for given content within a primary category.
     */
    private String findBestSubcategory(String content, String primaryCategory) {
        Set<String> subcategories = categorySubcategories.get(primaryCategory);
        if (subcategories == null || subcategories.isEmpty()) {
            return null;
        }
        
        for (String subcategory : subcategories) {
            if (content.contains(subcategory)) {
                return subcategory;
            }
        }
        
        // Return first subcategory as default
        return subcategories.iterator().next();
    }
    
    /**
     * Determines if a word is considered important for tagging.
     */
    private boolean isImportantWord(String word) {
        // Filter out common words (simple stopword list)
        Set<String> stopWords = Set.of("the", "and", "for", "are", "but", "not", "you", "all", 
            "can", "had", "her", "was", "one", "our", "out", "day", "get", "has", "him", 
            "his", "how", "its", "may", "new", "now", "old", "see", "two", "who", "boy", 
            "did", "does", "let", "man", "put", "say", "she", "too", "use", "this", "that", 
            "with", "have", "from", "they", "know", "want", "been", "good", "much", "some", 
            "time", "very", "when", "come", "here", "just", "like", "long", "make", "many", 
            "over", "such", "take", "than", "them", "well", "were");
        
        return !stopWords.contains(word.toLowerCase());
    }
    
    /**
     * Helper class for storing category scores during alternative suggestions.
     */
    private static class CategoryScore {
        final String category;
        final String subcategory;
        final Set<String> tags;
        final double confidence;
        
        CategoryScore(String category, String subcategory, Set<String> tags, double confidence) {
            this.category = category;
            this.subcategory = subcategory;
            this.tags = tags;
            this.confidence = confidence;
        }
    }
}