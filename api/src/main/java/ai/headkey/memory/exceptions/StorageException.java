package ai.headkey.memory.exceptions;

/**
 * Exception thrown when storage operations fail in the Memory Encoding System (MES).
 * This exception indicates problems with the underlying storage backend such as
 * database connectivity issues, disk space problems, or data corruption.
 */
public class StorageException extends RuntimeException {
    
    /**
     * The operation that was being performed when the error occurred.
     */
    private final String operation;
    
    /**
     * The memory ID involved in the operation (if applicable).
     */
    private final String memoryId;
    
    /**
     * The error code for categorizing the type of storage error.
     */
    private final String errorCode;
    
    /**
     * Default constructor.
     */
    public StorageException() {
        super();
        this.operation = null;
        this.memoryId = null;
        this.errorCode = null;
    }
    
    /**
     * Constructor with message.
     * 
     * @param message The error message
     */
    public StorageException(String message) {
        super(message);
        this.operation = null;
        this.memoryId = null;
        this.errorCode = null;
    }
    
    /**
     * Constructor with message and cause.
     * 
     * @param message The error message
     * @param cause The underlying cause
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
        this.operation = null;
        this.memoryId = null;
        this.errorCode = null;
    }
    
    /**
     * Constructor with operation context.
     * 
     * @param message The error message
     * @param operation The storage operation that failed
     */
    public StorageException(String message, String operation) {
        super(message);
        this.operation = operation;
        this.memoryId = null;
        this.errorCode = null;
    }
    
    /**
     * Constructor with operation and memory context.
     * 
     * @param message The error message
     * @param operation The storage operation that failed
     * @param memoryId The memory ID involved in the operation
     */
    public StorageException(String message, String operation, String memoryId) {
        super(message);
        this.operation = operation;
        this.memoryId = memoryId;
        this.errorCode = null;
    }
    
    /**
     * Full constructor with all context information.
     * 
     * @param message The error message
     * @param operation The storage operation that failed
     * @param memoryId The memory ID involved in the operation
     * @param errorCode The error code categorizing the failure
     * @param cause The underlying cause
     */
    public StorageException(String message, String operation, String memoryId, String errorCode, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.memoryId = memoryId;
        this.errorCode = errorCode;
    }
    
    /**
     * Gets the operation that was being performed when the error occurred.
     * 
     * @return The operation name, or null if not specified
     */
    public String getOperation() {
        return operation;
    }
    
    /**
     * Gets the memory ID that was involved in the failed operation.
     * 
     * @return The memory ID, or null if not applicable
     */
    public String getMemoryId() {
        return memoryId;
    }
    
    /**
     * Gets the error code categorizing the type of storage error.
     * 
     * @return The error code, or null if not specified
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Checks if this exception has operation context information.
     * 
     * @return true if operation is available
     */
    public boolean hasOperationContext() {
        return operation != null;
    }
    
    /**
     * Checks if this exception has memory context information.
     * 
     * @return true if memory ID is available
     */
    public boolean hasMemoryContext() {
        return memoryId != null;
    }
    
    /**
     * Creates a detailed error message including all available context.
     * 
     * @return A comprehensive error message
     */
    public String getDetailedMessage() {
        StringBuilder message = new StringBuilder();
        message.append(getMessage());
        
        if (operation != null) {
            message.append(" (Operation: ").append(operation);
            if (memoryId != null) {
                message.append(", MemoryId: ").append(memoryId);
            }
            if (errorCode != null) {
                message.append(", ErrorCode: ").append(errorCode);
            }
            message.append(")");
        }
        
        return message.toString();
    }
    
    /**
     * Creates a StorageException for connection failures.
     * 
     * @param message The error message
     * @param cause The underlying cause
     * @return A StorageException with connection error context
     */
    public static StorageException connectionError(String message, Throwable cause) {
        return new StorageException(message, "CONNECTION", null, "CONN_FAILED", cause);
    }
    
    /**
     * Creates a StorageException for encoding failures.
     * 
     * @param message The error message
     * @param memoryId The memory ID that failed to encode
     * @param cause The underlying cause
     * @return A StorageException with encoding error context
     */
    public static StorageException encodingError(String message, String memoryId, Throwable cause) {
        return new StorageException(message, "ENCODE", memoryId, "ENCODE_FAILED", cause);
    }
    
    /**
     * Creates a StorageException for retrieval failures.
     * 
     * @param message The error message
     * @param memoryId The memory ID that failed to retrieve
     * @param cause The underlying cause
     * @return A StorageException with retrieval error context
     */
    public static StorageException retrievalError(String message, String memoryId, Throwable cause) {
        return new StorageException(message, "RETRIEVE", memoryId, "RETRIEVE_FAILED", cause);
    }
    
    /**
     * Creates a StorageException for deletion failures.
     * 
     * @param message The error message
     * @param memoryId The memory ID that failed to delete
     * @param cause The underlying cause
     * @return A StorageException with deletion error context
     */
    public static StorageException deletionError(String message, String memoryId, Throwable cause) {
        return new StorageException(message, "DELETE", memoryId, "DELETE_FAILED", cause);
    }
    
    /**
     * Creates a StorageException for capacity/space issues.
     * 
     * @param message The error message
     * @return A StorageException with capacity error context
     */
    public static StorageException capacityError(String message) {
        return new StorageException(message, "CAPACITY_CHECK", null, "INSUFFICIENT_SPACE", null);
    }
    
    @Override
    public String toString() {
        return "StorageException{" +
                "message='" + getMessage() + '\'' +
                ", operation='" + operation + '\'' +
                ", memoryId='" + memoryId + '\'' +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}