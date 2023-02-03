package ch.unibas.medizin.depot.config;

import ch.unibas.medizin.depot.util.ErrorResponse;
import ch.unibas.medizin.depot.util.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(final ResponseStatusException exception) {
        var errorResponse = new ErrorResponse(exception.getStatusCode().value(), exception.getClass().getSimpleName(), exception.getMessage(), List.of());
        return new ResponseEntity<>(errorResponse, exception.getStatusCode());
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

