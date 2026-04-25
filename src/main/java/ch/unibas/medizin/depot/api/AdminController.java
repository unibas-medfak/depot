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
        final var violations = validator.validate(accessTokenRequestDto);

        for (var violation : violations) {
            throw new InvalidRequestException(violation.getPropertyPath().toString(), violation.getInvalidValue().toString(), violation.getMessage());
        }

        final var accessTokenResponseDto = accessTokenService.requestTokenString(accessTokenRequestDto);
        return ResponseEntity.ok(accessTokenResponseDto);
    }

    @PostMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Retrieve a QR code which provides access to the given realm")
    public byte[] qr(@RequestBody final AccessTokenRequestDto accessTokenRequestDto) {
        final var violations = validator.validate(accessTokenRequestDto);

        for (final var violation : violations) {
            throw new InvalidRequestException(violation.getPropertyPath().toString(), violation.getInvalidValue().toString(), violation.getMessage());
        }

        return accessTokenService.requestTokenQr(accessTokenRequestDto);
    }

}
