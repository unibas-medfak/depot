package ch.unibas.medizin.depot.config;

import ch.unibas.medizin.depot.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestControllerAdvice(annotations = RestController.class)
public record RestExceptionHandler() {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(PathNotFoundException.class)
    public ProblemDetail handlePathNotFoundException(final PathNotFoundException pathNotFoundException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, pathNotFoundException.getLocalizedMessage());
        problemDetails.setTitle("Path not found");
        problemDetails.setProperty("path", pathNotFoundException.getPath());
        return problemDetails;
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ProblemDetail handleFileNotFoundException(final FileNotFoundException fileNotFoundException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, fileNotFoundException.getLocalizedMessage());
        problemDetails.setTitle("File not found");
        problemDetails.setProperty("file", fileNotFoundException.getFile());
        return problemDetails;
    }

    @ExceptionHandler(FileAlreadyExistsAsFolderException.class)
    public ProblemDetail handleFileAlreadyExistsAsFolderException(final FileAlreadyExistsAsFolderException fileAlreadyExistsAsFolderException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, fileAlreadyExistsAsFolderException.getLocalizedMessage());
        problemDetails.setTitle("File exists");
        problemDetails.setProperty("file", fileAlreadyExistsAsFolderException.getFile());
        return problemDetails;
    }

    @ExceptionHandler(FolderAlreadyExistsAsFileException.class)
    public ProblemDetail handleFolderAlreadyExistsAsFileException(final FolderAlreadyExistsAsFileException folderAlreadyExistsAsFileException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, folderAlreadyExistsAsFileException.getLocalizedMessage());
        problemDetails.setTitle("Folder exists");
        problemDetails.setProperty("folder", folderAlreadyExistsAsFileException.getPath());
        return problemDetails;
    }

    @ExceptionHandler(DestinationAlreadyExistsException.class)
    public ProblemDetail handleDestinationAlreadyExistsException(final DestinationAlreadyExistsException destinationAlreadyExistsException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, destinationAlreadyExistsException.getLocalizedMessage());
        problemDetails.setTitle("Destination exists");
        problemDetails.setProperty("path", destinationAlreadyExistsException.getPath());
        return problemDetails;
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequestException(final InvalidRequestException invalidRequestException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, invalidRequestException.getLocalizedMessage());
        problemDetails.setTitle("Invalid request");
        problemDetails.setProperty(invalidRequestException.getPropertyPath(), invalidRequestException.getValue());
        return problemDetails;
    }

    @ExceptionHandler(UploadException.class)
    public ProblemDetail handleUploadException(final UploadException uploadException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(uploadException.getStatus(), uploadException.getLocalizedMessage());
        problemDetails.setTitle(uploadException.getTitle());
        if (uploadException.getBytesReceived() != null) {
            problemDetails.setProperty("bytesReceived", uploadException.getBytesReceived());
        }
        return problemDetails;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(final AuthenticationException authenticationException, final HttpServletRequest request) {
        log.warn("Authentication failed {} {} {} {}",
                kv("method", request.getMethod()), kv("uri", request.getRequestURI()), kv("client", request.getRemoteAddr()), kv("reason", authenticationException.getMessage()));
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, authenticationException.getLocalizedMessage());
        problemDetails.setTitle("Unauthorized");
        return problemDetails;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(final AccessDeniedException accessDeniedException, final HttpServletRequest request) {
        log.warn("Access denied {} {} {}",
                kv("method", request.getMethod()), kv("uri", request.getRequestURI()), kv("client", request.getRemoteAddr()));
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, accessDeniedException.getLocalizedMessage());
        problemDetails.setTitle("Access denied");
        return problemDetails;
    }

}

