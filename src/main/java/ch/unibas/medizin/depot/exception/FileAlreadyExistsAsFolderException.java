package ch.unibas.medizin.depot.exception;

public class FileAlreadyExistsAsFolderException extends RuntimeException {

    private final String file;

    public FileAlreadyExistsAsFolderException(String file) {
        super("File " + file + " already exists as folder");
        this.file = file;
    }

    public String getFile() {
        return file;
    }

}
