package ch.unibas.medizin.depot.config;

import ch.unibas.medizin.depot.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(InvlidRequestException.class)
    public ProblemDetail handleInvlidRequestException(final InvlidRequestException invlidRequestException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, invlidRequestException.getLocalizedMessage());
        problemDetails.setTitle("Invalid request");
        problemDetails.setProperty(invlidRequestException.getPropertyPath(), invlidRequestException.getValue());
        return problemDetails;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(final AccessDeniedException accessDeniedException) {
        log.error(accessDeniedException.getMessage());
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, accessDeniedException.getLocalizedMessage());
        problemDetails.setTitle("Access denied");
        return problemDetails;
    }

}

