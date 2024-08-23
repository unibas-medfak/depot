package ch.unibas.medizin.depot.validation;

import ch.unibas.medizin.depot.util.DepotUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public record PathValidator() implements ConstraintValidator<PathConstraint, String> {

    @Override
    public boolean isValid(String path, ConstraintValidatorContext constraintValidatorContext) {
        return DepotUtil.isValidTenantOrRealm(path);
    }

}
