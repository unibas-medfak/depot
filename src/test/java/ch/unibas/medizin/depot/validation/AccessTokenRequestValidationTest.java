package ch.unibas.medizin.depot.validation;

import ch.unibas.medizin.depot.dto.AccessTokenRequestDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccessTokenRequestValidationTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validateUser() {
        var accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", "r", Instant.now().plus(1, ChronoUnit.DAYS));
        var violations = validator.validate(accessTokenRequestDto);
        assertTrue(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", "rw", Instant.now());
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc13", "user", "rwD", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertTrue(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", "wr", Instant.now().minus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", " ", "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "", "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", null, Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", "", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", "fw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "password", "rw", null);
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "a%b", "password", "w", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "a\"b", "password", "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "realm$_", "password", "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", " ", "password", "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "", "password", "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", " ", "56aBc1é3", "password", "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56@a-B_c1.é3", "password", "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertTrue(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto(null, "pw", "56@a-B_c1.é3", "password", "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", null, "56@a-B_c1.é3", "password", "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", null, "password", "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56@a-B_c1.é3", null, "rw", Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56@a-B_c1.é3", "password", null, Instant.now().plus(1, ChronoUnit.DAYS));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56@a-B_c1.é3", "password", "rw", null);
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());
    }

}
