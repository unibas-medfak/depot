package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.dto.AccessTokenRequestDto;
import ch.unibas.medizin.depot.dto.AccessTokenResponseDto;
import ch.unibas.medizin.depot.exception.InvalidRequestException;
import ch.unibas.medizin.depot.service.AccessTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Validator;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@NullMarked
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Admin API")
public class AdminController {

    private final AccessTokenService accessTokenService;

    private final Validator validator;

    public AdminController(AccessTokenService accessTokenService, Validator validator) {
        this.accessTokenService = accessTokenService;
        this.validator = validator;
    }

    @PostMapping("/register")
    @Operation(summary = "Retrieve a token which provides access to the given realm")
    public ResponseEntity<AccessTokenResponseDto> register(@RequestBody final AccessTokenRequestDto accessTokenRequestDto) {
        throwIfInvalid(accessTokenRequestDto);

        final var accessTokenResponseDto = accessTokenService.requestTokenString(accessTokenRequestDto);
        return ResponseEntity.ok(accessTokenResponseDto);
    }

    @PostMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Retrieve a QR code which provides access to the given realm")
    public byte[] qr(@RequestBody final AccessTokenRequestDto accessTokenRequestDto) {
        throwIfInvalid(accessTokenRequestDto);

        return accessTokenService.requestTokenQr(accessTokenRequestDto);
    }

    private void throwIfInvalid(final AccessTokenRequestDto accessTokenRequestDto) {
        for (final var violation : validator.validate(accessTokenRequestDto)) {
            // String.valueOf keeps a missing field reportable as "null" instead of throwing:
            // getInvalidValue() is null for a @NotNull or @NotBlank violation on an absent field.
            throw new InvalidRequestException(
                    violation.getPropertyPath().toString(),
                    String.valueOf(violation.getInvalidValue()),
                    violation.getMessage());
        }
    }

}
