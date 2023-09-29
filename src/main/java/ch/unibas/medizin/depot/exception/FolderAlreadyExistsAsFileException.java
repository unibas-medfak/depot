package ch.unibas.medizin.depot.exception;

public class FolderAlreadyExistsAsFileException extends RuntimeException {

    private final String path;

    public FolderAlreadyExistsAsFileException(String path) {
        super("Folder " + path + " already exists as file");
        this.path = path;
    }

    public String getPath() {
        return path;
    }

}
