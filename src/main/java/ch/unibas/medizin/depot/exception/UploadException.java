package ch.unibas.medizin.depot.exception;

import org.springframework.http.HttpStatus;
import org.jspecify.annotations.Nullable;

public class UploadException extends RuntimeException {

    private final HttpStatus status;

    private final String title;

    @Nullable
    private final Long bytesReceived;

    public UploadException(HttpStatus status, String title, String detail, @Nullable Long bytesReceived) {
        super(detail);
        this.status = status;
        this.title = title;
        this.bytesReceived = bytesReceived;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    @Nullable
    public Long getBytesReceived() {
        return bytesReceived;
    }
}
