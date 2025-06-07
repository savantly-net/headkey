package ai.headkey.memory.exceptions;

/**
 * Exception thrown when input validation fails during memory operations.
 * This exception is used by the Information Ingestion Module (IIM) and other
 * components when they encounter invalid or malformed input data.
 */
public class InvalidInputException extends Exception {
    
    /**
     * The field name that caused the validation error (optional).
     */
    private final String fieldName;
    
    /**
     * The invalid value that caused the error (optional).
     */
    private final Object invalidValue;
    
    /**
     * Default constructor.
     */
    public InvalidInputException() {
        super();
        this.fieldName = null;
        this.invalidValue = null;
    }
    
    /**
     * Constructor with message.
     * 
     * @param message The error message
     */
    public InvalidInputException(String message) {
        super(message);
        this.fieldName = null;
        this.invalidValue = null;
    }
    
    /**
     * Constructor with message and cause.
     * 
     * @param message The error message
     * @param cause The underlying cause
     */
    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
        this.fieldName = null;
        this.invalidValue = null;
    }
    
    /**
     * Constructor with field-specific validation error.
     * 
     * @param message The error message
     * @param fieldName The name of the field that failed validation
     * @param invalidValue The invalid value
     */
    public InvalidInputException(String message, String fieldName, Object invalidValue) {
        super(message);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }
    
    /**
     * Constructor with field-specific validation error and cause.
     * 
     * @param message The error message
     * @param fieldName The name of the field that failed validation
     * @param invalidValue The invalid value
     * @param cause The underlying cause
     */
    public InvalidInputException(String message, String fieldName, Object invalidValue, Throwable cause) {
        super(message, cause);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }
    
    /**
     * Gets the field name that caused the validation error.
     * 
     * @return The field name, or null if not specified
     */
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * Gets the invalid value that caused the error.
     * 
     * @return The invalid value, or null if not specified
     */
    public Object getInvalidValue() {
        return invalidValue;
    }
    
    /**
     * Checks if this exception has field-specific information.
     * 
     * @return true if field name is available
     */
    public boolean hasFieldInfo() {
        return fieldName != null;
    }
    
    /**
     * Creates a detailed error message including field information if available.
     * 
     * @return A comprehensive error message
     */
    public String getDetailedMessage() {
        StringBuilder message = new StringBuilder();
        message.append(getMessage());
        
        if (fieldName != null) {
            message.append(" (Field: ").append(fieldName);
            if (invalidValue != null) {
                message.append(", Value: ").append(invalidValue);
            }
            message.append(")");
        }
        
        return message.toString();
    }
    
    @Override
    public String toString() {
        return "InvalidInputException{" +
                "message='" + getMessage() + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", invalidValue=" + invalidValue +
                '}';
    }
}