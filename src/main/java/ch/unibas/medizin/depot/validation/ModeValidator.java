package ch.unibas.medizin.depot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public record ModeValidator() implements ConstraintValidator<ModeConstraint, String> {

    @Override
    public boolean isValid(String mode, ConstraintValidatorContext constraintValidatorContext) {
        return "r".equalsIgnoreCase(mode) || "w".equalsIgnoreCase(mode) || "rw".equalsIgnoreCase(mode);
    }

}
