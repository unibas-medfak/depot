package ch.unibas.medizin.depot.exception;

public class DestinationAlreadyExistsException extends RuntimeException {

    private final String path;

    public DestinationAlreadyExistsException(String path) {
        super("Destination " + path + " already exists");
        this.path = path;
    }

    public String getPath() {
        return path;
    }

}
