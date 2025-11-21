package ch.unibas.medizin.depot.config;

import ch.unibas.medizin.depot.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = RestController.class)
public record RestExceptionHandler() {

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

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvlidRequestException(final InvalidRequestException invalidRequestException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, invalidRequestException.getLocalizedMessage());
        problemDetails.setTitle("Invalid request");
        problemDetails.setProperty(invalidRequestException.getPropertyPath(), invalidRequestException.getValue());
        return problemDetails;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(final AccessDeniedException accessDeniedException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, accessDeniedException.getLocalizedMessage());
        problemDetails.setTitle("Access denied");
        return problemDetails;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(final AuthenticationException authenticationException) {
        var problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, authenticationException.getLocalizedMessage());
        problemDetails.setTitle("Authentication failed");
        return problemDetails;
    }

}

