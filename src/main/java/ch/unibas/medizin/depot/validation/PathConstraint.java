package ch.unibas.medizin.depot.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PathValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface PathConstraint {
    String message() default "must only contain characters and numbers";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
