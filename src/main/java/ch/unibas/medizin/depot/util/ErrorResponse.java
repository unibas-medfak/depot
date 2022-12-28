package ch.unibas.medizin.depot.util;

import java.util.List;

public record ErrorResponse(Integer httpStatus,
                            String exception,
                            String message,
                            List<FieldError> fieldErrors) {
}
