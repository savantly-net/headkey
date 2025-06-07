package ai.headkey.memory.exceptions;

/**
 * Exception thrown when a requested memory record cannot be found in the storage system.
 * This exception indicates that a memory with the specified ID does not exist or
 * has been deleted/archived and is no longer accessible.
 */
public class MemoryNotFoundException extends Exception {
    
    /**
     * The memory ID that could not be found.
     */
    private final String memoryId;
    
    /**
     * The agent ID context (optional).
     */
    private final String agentId;
    
    /**
     * The operation that triggered the search for the missing memory.
     */
    private final String operation;
    
    /**
     * Default constructor.
     */
    public MemoryNotFoundException() {
        super();
        this.memoryId = null;
        this.agentId = null;
        this.operation = null;
    }
    
    /**
     * Constructor with message.
     * 
     * @param message The error message
     */
    public MemoryNotFoundException(String message) {
        super(message);
        this.memoryId = null;
        this.agentId = null;
        this.operation = null;
    }
    
    /**
     * Constructor with message and memory ID.
     * 
     * @param message The error message
     * @param memoryId The memory ID that was not found
     */
    public MemoryNotFoundException(String message, String memoryId) {
        super(message);
        this.memoryId = memoryId;
        this.agentId = null;
        this.operation = null;
    }
    
    /**
     * Constructor with message, memory ID, and agent context.
     * 
     * @param message The error message
     * @param memoryId The memory ID that was not found
     * @param agentId The agent ID context
     */
    public MemoryNotFoundException(String message, String memoryId, String agentId) {
        super(message);
        this.memoryId = memoryId;
        this.agentId = agentId;
        this.operation = null;
    }
    
    /**
     * Full constructor with all context information.
     * 
     * @param message The error message
     * @param memoryId The memory ID that was not found
     * @param agentId The agent ID context
     * @param operation The operation that triggered the search
     */
    public MemoryNotFoundException(String message, String memoryId, String agentId, String operation) {
        super(message);
        this.memoryId = memoryId;
        this.agentId = agentId;
        this.operation = operation;
    }
    
    /**
     * Constructor with message, memory ID, and cause.
     * 
     * @param message The error message
     * @param memoryId The memory ID that was not found
     * @param cause The underlying cause
     */
    public MemoryNotFoundException(String message, String memoryId, Throwable cause) {
        super(message, cause);
        this.memoryId = memoryId;
        this.agentId = null;
        this.operation = null;
    }
    
    /**
     * Gets the memory ID that could not be found.
     * 
     * @return The memory ID, or null if not specified
     */
    public String getMemoryId() {
        return memoryId;
    }
    
    /**
     * Gets the agent ID context.
     * 
     * @return The agent ID, or null if not specified
     */
    public String getAgentId() {
        return agentId;
    }
    
    /**
     * Gets the operation that triggered the search for the missing memory.
     * 
     * @return The operation name, or null if not specified
     */
    public String getOperation() {
        return operation;
    }
    
    /**
     * Checks if this exception has memory ID context.
     * 
     * @return true if memory ID is available
     */
    public boolean hasMemoryId() {
        return memoryId != null;
    }
    
    /**
     * Checks if this exception has agent context.
     * 
     * @return true if agent ID is available
     */
    public boolean hasAgentContext() {
        return agentId != null;
    }
    
    /**
     * Creates a detailed error message including all available context.
     * 
     * @return A comprehensive error message
     */
    public String getDetailedMessage() {
        StringBuilder message = new StringBuilder();
        message.append(getMessage());
        
        if (memoryId != null) {
            message.append(" (MemoryId: ").append(memoryId);
            if (agentId != null) {
                message.append(", AgentId: ").append(agentId);
            }
            if (operation != null) {
                message.append(", Operation: ").append(operation);
            }
            message.append(")");
        }
        
        return message.toString();
    }
    
    /**
     * Creates a MemoryNotFoundException for a specific memory ID.
     * 
     * @param memoryId The memory ID that was not found
     * @return A MemoryNotFoundException with the specified memory ID
     */
    public static MemoryNotFoundException forMemoryId(String memoryId) {
        return new MemoryNotFoundException(
            "Memory not found with ID: " + memoryId, 
            memoryId
        );
    }
    
    /**
     * Creates a MemoryNotFoundException for a specific memory ID and agent.
     * 
     * @param memoryId The memory ID that was not found
     * @param agentId The agent ID context
     * @return A MemoryNotFoundException with the specified context
     */
    public static MemoryNotFoundException forMemoryAndAgent(String memoryId, String agentId) {
        return new MemoryNotFoundException(
            String.format("Memory not found with ID: %s for agent: %s", memoryId, agentId),
            memoryId,
            agentId
        );
    }
    
    /**
     * Creates a MemoryNotFoundException for a retrieval operation.
     * 
     * @param memoryId The memory ID that was not found
     * @param operation The operation that was being performed
     * @return A MemoryNotFoundException with operation context
     */
    public static MemoryNotFoundException forOperation(String memoryId, String operation) {
        return new MemoryNotFoundException(
            String.format("Memory not found with ID: %s during %s operation", memoryId, operation),
            memoryId,
            null,
            operation
        );
    }
    
    @Override
    public String toString() {
        return "MemoryNotFoundException{" +
                "message='" + getMessage() + '\'' +
                ", memoryId='" + memoryId + '\'' +
                ", agentId='" + agentId + '\'' +
                ", operation='" + operation + '\'' +
                '}';
    }
}