package ch.unibas.medizin.depot.validation;

import ch.unibas.medizin.depot.util.DepotUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public record SubjectValidator() implements ConstraintValidator<SubjectConstraint, String> {

    @Override
    public boolean isValid(String realm, ConstraintValidatorContext constraintValidatorContext) {
        return DepotUtil.isValidSubject(realm);
    }

}
