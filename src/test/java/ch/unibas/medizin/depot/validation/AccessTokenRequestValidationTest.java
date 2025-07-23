package ch.unibas.medizin.depot.validation;

import ch.unibas.medizin.depot.dto.AccessTokenRequestDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

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
        var accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", "r", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        var violations = validator.validate(accessTokenRequestDto);
        assertTrue(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", "rw", LocalDate.now(ZoneId.systemDefault()));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc13", "user", "rwD", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertTrue(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", "wr", LocalDate.now(ZoneId.systemDefault()).minusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", " ", "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "", "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", null, LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", "", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "user", "fw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56aBc1é3", "password", "rw", null);
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "a%b", "password", "w", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertTrue(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "a\"b", "password", "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "realm$_", "password", "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", " ", "password", "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "", "password", "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", " ", "56aBc1é3", "password", "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56@a-B_c1.é3", "password", "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertTrue(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto(null, "pw", "56@a-B_c1.é3", "password", "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", null, "56@a-B_c1.é3", "password", "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", null, "password", "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56@a-B_c1.é3", null, "rw", LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56@a-B_c1.é3", "password", null, LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());

        accessTokenRequestDto = new AccessTokenRequestDto("tenant", "pw", "56@a-B_c1.é3", "password", "rw", null);
        violations = validator.validate(accessTokenRequestDto);
        assertFalse(violations.isEmpty());
    }

}
