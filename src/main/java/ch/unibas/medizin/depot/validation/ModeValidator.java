package ch.unibas.medizin.depot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Locale;

public record ModeValidator() implements ConstraintValidator<ModeConstraint, String> {

    @Override
    public boolean isValid(@Nullable String mode, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.hasText(mode) && !StringUtils.hasText(mode.toLowerCase(Locale.getDefault()).replaceAll("[rwd]", ""));
    }

}
