package ai.headkey.memory.interfaces;

import ai.headkey.memory.dto.IngestionResult;
import ai.headkey.memory.dto.MemoryInput;
import ai.headkey.memory.exceptions.InvalidInputException;

/**
 * Interface for the Information Ingestion Module (IIM).
 * 
 * The IIM serves as the orchestrator for new information entering the memory system.
 * It coordinates the entire ingestion pipeline by validating input, calling the
 * Contextual Categorization Engine (CCE), Memory Encoding System (MES), and
 * Belief Reinforcement & Conflict Analyzer (BRCA) in sequence.
 * 
 * This interface follows the Single Responsibility Principle by focusing solely
 * on orchestrating the intake of data, without handling the specifics of
 * categorization, storage, or belief management.
 * 
 * @since 1.0
 */
public interface InformationIngestionModule {
    
    /**
     * Ingests raw information into the memory system.
     * 
     * This method orchestrates the complete ingestion pipeline:
     * 1. Validates the input data
     * 2. Calls the CCE to categorize the content
     * 3. Uses the MES to encode and store the memory
     * 4. Triggers the BRCA to update beliefs based on the new information
     * 
     * The operation is synchronous and returns once the memory is stored and
     * initial belief updates are complete.
     * 
     * @param input The raw input data containing content, metadata, and context
     * @return IngestionResult containing memory ID, category, status, and updated beliefs
     * @throws InvalidInputException if the input fails validation
     * @throws IllegalArgumentException if the input is null
     * 
     * @since 1.0
     */
    IngestionResult ingest(MemoryInput input) throws InvalidInputException;
    
    /**
     * Validates input data before ingestion.
     * 
     * This method performs comprehensive validation of the MemoryInput to ensure
     * it meets the required format and contains all necessary information for
     * successful processing. It checks:
     * - Required fields are present and non-null
     * - Content length and format constraints
     * - Agent ID validity
     * - Metadata structure and values
     * 
     * This method can be called independently for validation purposes or
     * is used internally prior to calling ingest().
     * 
     * @param input The input data to validate
     * @throws InvalidInputException if validation fails, with specific details
     *         about what validation rule was violated
     * @throws IllegalArgumentException if the input is null
     * 
     * @since 1.0
     */
    void validateInput(MemoryInput input) throws InvalidInputException;
    
    /**
     * Performs a dry run of the ingestion process without actually storing data.
     * 
     * This method executes the full ingestion pipeline (validation, categorization)
     * but stops before encoding and storing the memory or updating beliefs.
     * It's useful for:
     * - Testing input validity
     * - Previewing categorization results
     * - Debugging ingestion issues
     * - Batch processing validation
     * 
     * @param input The input data to process in dry run mode
     * @return IngestionResult with categorization info but no memory ID,
     *         and encoded flag set to false
     * @throws InvalidInputException if the input fails validation
     * @throws IllegalArgumentException if the input is null
     * 
     * @since 1.0
     */
    IngestionResult dryRunIngest(MemoryInput input) throws InvalidInputException;
    
    /**
     * Gets statistics about ingestion operations.
     * 
     * Returns metrics and statistics about the ingestion module's performance
     * and operation history. This can include:
     * - Total number of memories ingested
     * - Success/failure rates
     * - Average processing times
     * - Category distribution
     * - Error frequencies
     * 
     * The exact structure of the returned object may vary based on implementation
     * but should provide useful insights for monitoring and optimization.
     * 
     * @return A map containing various statistics and metrics
     * 
     * @since 1.0
     */
    java.util.Map<String, Object> getIngestionStatistics();
    
    /**
     * Checks if the ingestion module is healthy and ready to process requests.
     * 
     * Performs a health check of the ingestion module and its dependencies.
     * This includes verifying connectivity and availability of:
     * - Contextual Categorization Engine (CCE)
     * - Memory Encoding System (MES)
     * - Belief Reinforcement & Conflict Analyzer (BRCA)
     * 
     * @return true if the module and all dependencies are healthy and ready
     * 
     * @since 1.0
     */
    boolean isHealthy();
}