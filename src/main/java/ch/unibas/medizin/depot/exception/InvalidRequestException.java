package ch.unibas.medizin.depot.exception;

public class InvalidRequestException extends RuntimeException {

    private final String propertyPath;

    private final String value;

    public InvalidRequestException(String propertyPath, String value, String detail) {
        super(detail);
        this.propertyPath = propertyPath;
        this.value = value;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public String getValue() {
        return value;
    }
}
