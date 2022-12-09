package ch.unibas.medizin.depot.util;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldError {
    private String field;
    private String errorCode;
}
