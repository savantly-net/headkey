package ai.headkey.memory.langchain4j.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object for belief extraction response results.
 * 
 * This class represents the result of a belief extraction operation from AI services,
 * containing a list of extracted beliefs with their associated metadata, confidence
 * scores, and reasoning.
 * 
 * Used by BeliefExtractionAiService implementations to return structured
 * belief extraction results from Large Language Models.
 * 
 * @since 1.0
 */
public class BeliefExtractionResponse {
    
    /**
     * List of beliefs extracted from the analyzed content.
     * Each belief contains the statement, category, confidence, and other metadata.
     */
    private List<BeliefData> beliefs;
    
    /**
     * Default constructor.
     */
    public BeliefExtractionResponse() {
        this.beliefs = new ArrayList<>();
    }
    
    /**
     * Constructor with beliefs list.
     * 
     * @param beliefs The list of extracted beliefs
     */
    public BeliefExtractionResponse(List<BeliefData> beliefs) {
        this.beliefs = beliefs != null ? new ArrayList<>(beliefs) : new ArrayList<>();
    }
    
    /**
     * Gets the list of extracted beliefs.
     * 
     * @return A copy of the beliefs list
     */
    public List<BeliefData> getBeliefs() {
        return beliefs != null ? new ArrayList<>(beliefs) : new ArrayList<>();
    }
    
    /**
     * Sets the list of extracted beliefs.
     * 
     * @param beliefs The beliefs to set
     */
    public void setBeliefs(List<BeliefData> beliefs) {
        this.beliefs = beliefs != null ? new ArrayList<>(beliefs) : new ArrayList<>();
    }
    
    /**
     * Adds a belief to the response.
     * 
     * @param belief The belief to add
     */
    public void addBelief(BeliefData belief) {
        if (belief != null) {
            if (this.beliefs == null) {
                this.beliefs = new ArrayList<>();
            }
            this.beliefs.add(belief);
        }
    }
    
    /**
     * Gets the number of extracted beliefs.
     * 
     * @return The count of beliefs
     */
    public int getBeliefCount() {
        return beliefs != null ? beliefs.size() : 0;
    }
    
    /**
     * Checks if any beliefs were extracted.
     * 
     * @return true if beliefs were found
     */
    public boolean hasBeliefs() {
        return beliefs != null && !beliefs.isEmpty();
    }
    
    /**
     * Gets beliefs of a specific category.
     * 
     * @param category The category to filter by
     * @return List of beliefs matching the category
     */
    public List<BeliefData> getBeliefsOfCategory(String category) {
        if (beliefs == null || category == null) {
            return new ArrayList<>();
        }
        
        return beliefs.stream()
            .filter(belief -> category.equals(belief.getCategory()))
            .toList();
    }
    
    /**
     * Gets beliefs with confidence above a threshold.
     * 
     * @param threshold The minimum confidence threshold (0.0 to 1.0)
     * @return List of high-confidence beliefs
     */
    public List<BeliefData> getHighConfidenceBeliefs(double threshold) {
        if (beliefs == null) {
            return new ArrayList<>();
        }
        
        return beliefs.stream()
            .filter(belief -> belief.getConfidence() >= threshold)
            .toList();
    }
    
    /**
     * Gets positive beliefs (affirmative statements).
     * 
     * @return List of positive beliefs
     */
    public List<BeliefData> getPositiveBeliefs() {
        if (beliefs == null) {
            return new ArrayList<>();
        }
        
        return beliefs.stream()
            .filter(BeliefData::isPositive)
            .toList();
    }
    
    /**
     * Gets negative beliefs (negative statements).
     * 
     * @return List of negative beliefs
     */
    public List<BeliefData> getNegativeBeliefs() {
        if (beliefs == null) {
            return new ArrayList<>();
        }
        
        return beliefs.stream()
            .filter(belief -> !belief.isPositive())
            .toList();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeliefExtractionResponse that = (BeliefExtractionResponse) o;
        return Objects.equals(beliefs, that.beliefs);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(beliefs);
    }
    
    @Override
    public String toString() {
        return "BeliefExtractionResponse{" +
                "beliefs=" + beliefs +
                ", beliefCount=" + getBeliefCount() +
                '}';
    }
}