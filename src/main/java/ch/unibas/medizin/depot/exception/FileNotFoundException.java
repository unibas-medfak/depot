package ch.unibas.medizin.depot.exception;

public class FileNotFoundException extends RuntimeException {

    private final String file;

    public FileNotFoundException(String file) {
        super("File " + file + " not found");
        this.file = file;
    }

    public String getFile() {
        return file;
    }

}
