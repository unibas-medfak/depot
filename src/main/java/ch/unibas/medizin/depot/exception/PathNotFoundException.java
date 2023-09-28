package ch.unibas.medizin.depot.exception;

public class PathNotFoundException extends RuntimeException {

    private final String path;

    public PathNotFoundException(String path) {
        super("Path " + path + " not found.");
        this.path = path;
    }

    public String getPath() {
        return path;
    }

}
