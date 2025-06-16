package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.interfaces.BeliefExtractionService;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Simple pattern-based implementation of BeliefExtractionService.
 * 
 * This implementation uses regular expressions and keyword matching to extract
 * beliefs from memory content. It's designed for development, testing, and
 * demonstration purposes. A production system would likely use more sophisticated
 * NLP and AI-powered analysis.
 * 
 * The extraction logic includes:
 * - Preference detection (favorite, like, dislike patterns)
 * - Fact extraction (is/are/was/were patterns)
 * - Relationship detection (knows, friend, married patterns)
 * - Location extraction (lives, located, from patterns)
 * - Negation handling (not, never, no patterns)
 * 
 * @since 1.0
 */
public class SimplePatternBeliefExtractionService implements BeliefExtractionService {

    // Pattern matchers for belief extraction
    private final Pattern preferencePattern = Pattern.compile("(?i)(favorite|prefer|like|love|enjoy|hate|dislike)\\s+(.+)");
    private final Pattern factPattern = Pattern.compile("(?i)(.+)\\s+(is|are|was|were)\\s+(.+)");
    private final Pattern relationshipPattern = Pattern.compile("(?i)(.+)\\s+(knows|friend|married|related|knows)\\s+(.+)");
    private final Pattern locationPattern = Pattern.compile("(?i)(.+)\\s+(lives?|located|from)\\s+(.+)");
    private final Pattern negationPattern = Pattern.compile("(?i)(not|never|no|don't|doesn't|isn't|aren't)");
    
    // Confidence patterns
    private final Pattern certaintyPattern = Pattern.compile("(?i)(definitely|certainly|absolutely|sure|positive)");
    private final Pattern uncertaintyPattern = Pattern.compile("(?i)(maybe|perhaps|possibly|might|could|probably)");

    @Override
    public List<ExtractedBelief> extractBeliefs(String content, String agentId, CategoryLabel category) {
        System.out.println("SimplePatternBeliefExtractionService: Extracting beliefs from content: '" + content + "' for agent: " + agentId);
        
        if (content == null || content.trim().isEmpty()) {
            System.out.println("SimplePatternBeliefExtractionService: Content is null or empty, no beliefs extracted");
            return new ArrayList<>();
        }
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        List<ExtractedBelief> beliefs = new ArrayList<>();
        String normalizedContent = content.toLowerCase().trim();
        
        System.out.println("SimplePatternBeliefExtractionService: Normalized content: '" + normalizedContent + "'");
        
        // Extract preferences
        extractPreferences(normalizedContent, agentId, beliefs);
        
        // Extract facts
        extractFacts(normalizedContent, agentId, beliefs);
        
        // Extract relationships
        extractRelationships(normalizedContent, agentId, beliefs);
        
        // Extract locations
        extractLocations(normalizedContent, agentId, beliefs);
        
        // Set category for all extracted beliefs
        String beliefCategory = category != null ? category.getPrimary() : "general";
        for (ExtractedBelief belief : beliefs) {
            belief.addMetadata("originalCategory", beliefCategory);
        }
        
        System.out.println("SimplePatternBeliefExtractionService: Extracted " + beliefs.size() + " beliefs from content");
        for (ExtractedBelief belief : beliefs) {
            System.out.println("SimplePatternBeliefExtractionService: Extracted belief: " + belief.getStatement());
        }
        
        return beliefs;
    }

    @Override
    public double calculateSimilarity(String statement1, String statement2) {
        if (statement1 == null || statement2 == null) {
            return 0.0;
        }
        
        String s1 = statement1.toLowerCase().trim();
        String s2 = statement2.toLowerCase().trim();
        
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        // Simple similarity calculation using word overlap
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        // Filter out common words
        Set<String> commonWords = Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by");
        set1.removeAll(commonWords);
        set2.removeAll(commonWords);
        
        if (set1.isEmpty() && set2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    @Override
    public boolean areConflicting(String statement1, String statement2, String category1, String category2) {
        if (statement1 == null || statement2 == null) {
            return false;
        }
        
        String s1 = statement1.toLowerCase();
        String s2 = statement2.toLowerCase();
        
        // Check for direct negation patterns
        boolean hasNegation1 = negationPattern.matcher(s1).find();
        boolean hasNegation2 = negationPattern.matcher(s2).find();
        
        // If one is negative and other is positive, and they're about similar topics
        if (hasNegation1 != hasNegation2) {
            double similarity = calculateSimilarity(s1, s2);
            return similarity > 0.6; // Threshold for considering statements similar enough to conflict
        }
        
        // Check for mutually exclusive preferences in same category
        if ("preference".equals(category1) && "preference".equals(category2)) {
            return checkMutuallyExclusivePreferences(s1, s2);
        }
        
        // Check for contradictory facts
        if ("fact".equals(category1) && "fact".equals(category2)) {
            return checkContradictoryFacts(s1, s2);
        }
        
        return false;
    }

    @Override
    public String extractCategory(String statement) {
        if (statement == null || statement.trim().isEmpty()) {
            return "general";
        }
        
        String normalized = statement.toLowerCase();
        
        if (preferencePattern.matcher(normalized).find()) {
            return "preference";
        }
        if (factPattern.matcher(normalized).find()) {
            return "fact";
        }
        if (relationshipPattern.matcher(normalized).find()) {
            return "relationship";
        }
        if (locationPattern.matcher(normalized).find()) {
            return "location";
        }
        
        return "general";
    }

    @Override
    public double calculateConfidence(String content, String statement, ExtractionContext context) {
        if (content == null || statement == null) {
            return 0.5; // Default confidence
        }
        
        double baseConfidence = 0.6;
        String normalized = content.toLowerCase();
        
        // Boost confidence for certainty markers
        if (certaintyPattern.matcher(normalized).find()) {
            baseConfidence += 0.2;
        }
        
        // Reduce confidence for uncertainty markers
        if (uncertaintyPattern.matcher(normalized).find()) {
            baseConfidence -= 0.2;
        }
        
        // Boost confidence for repeated mentions
        String[] words = statement.toLowerCase().split("\\s+");
        for (String word : words) {
            if (word.length() > 3) {
                long occurrences = Arrays.stream(normalized.split("\\s+"))
                    .filter(w -> w.contains(word))
                    .count();
                if (occurrences > 1) {
                    baseConfidence += 0.1;
                    break;
                }
            }
        }
        
        // Consider source reliability if available in context
        if (context != null && context.getSourceMemory() != null) {
            Object reliability = context.getSourceMemory().getMetadata().getProperty("reliability");
            if (reliability instanceof Number) {
                double reliabilityScore = ((Number) reliability).doubleValue();
                baseConfidence = baseConfidence * reliabilityScore;
            }
        }
        
        return Math.max(0.0, Math.min(1.0, baseConfidence));
    }

    @Override
    public boolean isHealthy() {
        // Simple health check - patterns are compiled and ready
        try {
            preferencePattern.pattern();
            factPattern.pattern();
            relationshipPattern.pattern();
            locationPattern.pattern();
            negationPattern.pattern();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceType", "SimplePatternBeliefExtractionService");
        info.put("version", "1.0");
        info.put("description", "Pattern-based belief extraction using regular expressions");
        info.put("supportedLanguages", Arrays.asList("English"));
        info.put("extractionCapabilities", Arrays.asList("preferences", "facts", "relationships", "locations"));
        info.put("performanceCharacteristics", "Fast, lightweight, rule-based");
        info.put("limitations", "Simple pattern matching, English only, limited semantic understanding");
        return info;
    }

    // ========== Private Helper Methods ==========

    /**
     * Extracts preference-based beliefs.
     */
    private void extractPreferences(String content, String agentId, List<ExtractedBelief> beliefs) {
        System.out.println("SimplePatternBeliefExtractionService: Checking preference patterns for: '" + content + "'");
        System.out.println("SimplePatternBeliefExtractionService: Preference pattern match: " + preferencePattern.matcher(content).find());
        
        if (preferencePattern.matcher(content).find()) {
            System.out.println("SimplePatternBeliefExtractionService: Preference pattern matched, extracting preferences...");
            
            // Simple preference extraction
            if (content.contains("favorite")) {
                System.out.println("SimplePatternBeliefExtractionService: Found 'favorite' keyword");
                int favoriteIdx = content.indexOf("favorite");
                String remainder = content.substring(favoriteIdx);
                String statement = "User has preference: " + remainder;
                double confidence = calculateBasicConfidence(content);
                boolean positive = !negationPattern.matcher(content).find();
                
                ExtractedBelief belief = new ExtractedBelief(statement, agentId, "preference", confidence, positive);
                belief.addTag("preference");
                belief.addTag("favorite");
                beliefs.add(belief);
                System.out.println("SimplePatternBeliefExtractionService: Added favorite belief: " + statement);
            }
            
            // Handle like/dislike patterns
            if (content.contains("like") && !content.contains("dislike")) {
                System.out.println("SimplePatternBeliefExtractionService: Found 'like' keyword");
                String statement = "User likes: " + extractLikeObject(content);
                double confidence = calculateBasicConfidence(content);
                boolean positive = !negationPattern.matcher(content).find();
                
                ExtractedBelief belief = new ExtractedBelief(statement, agentId, "preference", confidence, positive);
                belief.addTag("preference");
                belief.addTag("like");
                beliefs.add(belief);
                System.out.println("SimplePatternBeliefExtractionService: Added like belief: " + statement);
            }
            
            if (content.contains("dislike") || content.contains("hate")) {
                System.out.println("SimplePatternBeliefExtractionService: Found 'dislike' or 'hate' keyword");
                String statement = "User dislikes: " + extractDislikeObject(content);
                double confidence = calculateBasicConfidence(content);
                
                ExtractedBelief belief = new ExtractedBelief(statement, agentId, "preference", confidence, true);
                belief.addTag("preference");
                belief.addTag("dislike");
                beliefs.add(belief);
                System.out.println("SimplePatternBeliefExtractionService: Added dislike belief: " + statement);
            }
        } else {
            System.out.println("SimplePatternBeliefExtractionService: No preference pattern matched for content");
        }
    }

    /**
     * Extracts fact-based beliefs.
     */
    private void extractFacts(String content, String agentId, List<ExtractedBelief> beliefs) {
        System.out.println("SimplePatternBeliefExtractionService: Checking fact patterns for: '" + content + "'");
        System.out.println("SimplePatternBeliefExtractionService: Fact pattern match: " + factPattern.matcher(content).find());
        
        if (factPattern.matcher(content).find()) {
            System.out.println("SimplePatternBeliefExtractionService: Fact pattern matched, extracting facts...");
            
            // Extract simple fact statements
            String statement = "Fact: " + content;
            double confidence = calculateBasicConfidence(content);
            boolean positive = !negationPattern.matcher(content).find();
            
            ExtractedBelief belief = new ExtractedBelief(statement, agentId, "fact", confidence, positive);
            belief.addTag("fact");
            beliefs.add(belief);
            System.out.println("SimplePatternBeliefExtractionService: Added fact belief: " + statement);
        } else {
            System.out.println("SimplePatternBeliefExtractionService: No fact pattern matched for content");
        }
    }

    /**
     * Extracts relationship-based beliefs.
     */
    private void extractRelationships(String content, String agentId, List<ExtractedBelief> beliefs) {
        System.out.println("SimplePatternBeliefExtractionService: Checking relationship patterns for: '" + content + "'");
        System.out.println("SimplePatternBeliefExtractionService: Relationship pattern match: " + relationshipPattern.matcher(content).find());
        
        if (relationshipPattern.matcher(content).find()) {
            System.out.println("SimplePatternBeliefExtractionService: Relationship pattern matched, extracting relationships...");
            
            // Extract relationship statements
            String statement = "Relationship: " + content;
            double confidence = calculateBasicConfidence(content);
            boolean positive = !negationPattern.matcher(content).find();
            
            ExtractedBelief belief = new ExtractedBelief(statement, agentId, "relationship", confidence, positive);
            belief.addTag("relationship");
            beliefs.add(belief);
            System.out.println("SimplePatternBeliefExtractionService: Added relationship belief: " + statement);
        } else {
            System.out.println("SimplePatternBeliefExtractionService: No relationship pattern matched for content");
        }
    }

    /**
     * Extracts location-based beliefs.
     */
    private void extractLocations(String content, String agentId, List<ExtractedBelief> beliefs) {
        System.out.println("SimplePatternBeliefExtractionService: Checking location patterns for: '" + content + "'");
        System.out.println("SimplePatternBeliefExtractionService: Location pattern match: " + locationPattern.matcher(content).find());
        
        if (locationPattern.matcher(content).find()) {
            System.out.println("SimplePatternBeliefExtractionService: Location pattern matched, extracting locations...");
            
            // Extract location statements
            String statement = "Location: " + content;
            double confidence = calculateBasicConfidence(content);
            boolean positive = !negationPattern.matcher(content).find();
            
            ExtractedBelief belief = new ExtractedBelief(statement, agentId, "location", confidence, positive);
            belief.addTag("location");
            beliefs.add(belief);
            System.out.println("SimplePatternBeliefExtractionService: Added location belief: " + statement);
        } else {
            System.out.println("SimplePatternBeliefExtractionService: No location pattern matched for content");
        }
    }

    private double calculateBasicConfidence(String content) {
        double confidence = 0.6; // Base confidence
        
        if (certaintyPattern.matcher(content).find()) {
            confidence += 0.2;
        }
        if (uncertaintyPattern.matcher(content).find()) {
            confidence -= 0.2;
        }
        
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    private String extractLikeObject(String content) {
        int likeIdx = content.indexOf("like");
        if (likeIdx >= 0 && likeIdx + 5 < content.length()) {
            String remainder = content.substring(likeIdx + 5).trim();
            int endIdx = Math.min(remainder.length(), 50); // Limit length
            return remainder.substring(0, endIdx);
        }
        return "something";
    }

    private String extractDislikeObject(String content) {
        int dislikeIdx = content.indexOf("dislike");
        int hateIdx = content.indexOf("hate");
        int startIdx = -1;
        
        if (dislikeIdx >= 0) {
            startIdx = dislikeIdx + 8;
        } else if (hateIdx >= 0) {
            startIdx = hateIdx + 5;
        }
        
        if (startIdx >= 0 && startIdx < content.length()) {
            String remainder = content.substring(startIdx).trim();
            int endIdx = Math.min(remainder.length(), 50); // Limit length
            return remainder.substring(0, endIdx);
        }
        return "something";
    }

    private boolean checkMutuallyExclusivePreferences(String statement1, String statement2) {
        // Simple check for opposing preferences
        boolean s1Positive = !negationPattern.matcher(statement1).find();
        boolean s2Positive = !negationPattern.matcher(statement2).find();
        
        if (s1Positive != s2Positive) {
            // One is positive, one is negative - check if they're about the same thing
            return calculateSimilarity(statement1, statement2) > 0.7;
        }
        
        return false;
    }

    private boolean checkContradictoryFacts(String statement1, String statement2) {
        // Look for contradictory factual statements
        // This is a simplified implementation
        return statement1.contains("is not") && statement2.contains("is") ||
               statement1.contains("is") && statement2.contains("is not") ||
               statement1.contains("was not") && statement2.contains("was") ||
               statement1.contains("was") && statement2.contains("was not");
    }
}