package ch.unibas.medizin.depot.validation;

import ch.unibas.medizin.depot.util.DepotUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

public record PathValidator() implements ConstraintValidator<PathConstraint, String> {

    @Override
    public boolean isValid(@Nullable String path, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.hasText(path) && DepotUtil.isValidTenantOrRealm(path);
    }

}
