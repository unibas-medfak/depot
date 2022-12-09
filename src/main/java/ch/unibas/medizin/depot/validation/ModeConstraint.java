package ch.unibas.medizin.depot.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ModeValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ModeConstraint {
    String message() default "must be r or w or rw";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
