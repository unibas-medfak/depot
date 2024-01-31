package ch.unibas.medizin.depot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public record ModeValidator() implements ConstraintValidator<ModeConstraint, String> {

    @Override
    public boolean isValid(String mode, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.hasText(mode) && !StringUtils.hasText(mode.toLowerCase().replaceAll("[rwd]", ""));
    }

}
