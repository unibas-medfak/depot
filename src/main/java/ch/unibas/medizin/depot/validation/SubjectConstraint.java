package ch.unibas.medizin.depot.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SubjectValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SubjectConstraint {
    String message() default "must not contain control characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
