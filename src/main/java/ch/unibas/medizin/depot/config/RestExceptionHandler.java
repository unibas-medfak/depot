package ch.unibas.medizin.depot.config;

import ch.unibas.medizin.depot.exception.FileAlreadyExistsAsFolderException;
import ch.unibas.medizin.depot.exception.FileNotFoundException;
import ch.unibas.medizin.depot.exception.FolderAlreadyExistsAsFileException;
import ch.unibas.medizin.depot.exception.PathNotFoundException;
import ch.unibas.medizin.depot.util.ErrorResponse;
import ch.unibas.medizin.depot.util.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice(annotations = RestController.class)
public record RestExceptionHandler() {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(PathNotFoundException.class)
    public ProblemDetail handlePathNotFoundException(PathNotFoundException pathNotFoundException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, pathNotFoundException.getLocalizedMessage());
        problemDetails.setTitle("Path Not Found");
        problemDetails.setProperty("path", pathNotFoundException.getPath());
        return problemDetails;
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ProblemDetail handleFileNotFoundException(FileNotFoundException fileNotFoundException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, fileNotFoundException.getLocalizedMessage());
        problemDetails.setTitle("File Not Found");
        problemDetails.setProperty("file", fileNotFoundException.getFile());
        return problemDetails;
    }

    @ExceptionHandler(FileAlreadyExistsAsFolderException.class)
    public ProblemDetail handleFileAlreadyExistsAsFolderException(FileAlreadyExistsAsFolderException fileAlreadyExistsAsFolderException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, fileAlreadyExistsAsFolderException.getLocalizedMessage());
        problemDetails.setTitle("File Exists");
        problemDetails.setProperty("file", fileAlreadyExistsAsFolderException.getFile());
        return problemDetails;
    }

    @ExceptionHandler(FolderAlreadyExistsAsFileException.class)
    public ProblemDetail handleFolderAlreadyExistsAsFileException(FolderAlreadyExistsAsFileException folderAlreadyExistsAsFileException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, folderAlreadyExistsAsFileException.getLocalizedMessage());
        problemDetails.setTitle("Folder Exists");
        problemDetails.setProperty("folder", folderAlreadyExistsAsFileException.getPath());
        return problemDetails;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(final MethodArgumentNotValidException exception) {
        final var bindingResult = exception.getBindingResult();
        final var fieldErrors = bindingResult.getFieldErrors()
                .stream()
                .map(error -> new FieldError(error.getField(), error.getCode()))
                .collect(Collectors.toList());
        var errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getClass().getSimpleName(), "", fieldErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(final AccessDeniedException exception) {
        log.error(exception.getMessage());
        final var errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), exception.getClass().getSimpleName(), "", List.of());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleThrowable(final Throwable exception) {
        log.error(exception.getMessage(), exception);
        final var errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getClass().getSimpleName(), "", List.of());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}

