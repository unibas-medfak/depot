package ch.unibas.medizin.depot.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ModeValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ModeConstraint {
    String message() default "must contain r w and/or d";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
