package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.dto.AccessTokenRequestDto;
import ch.unibas.medizin.depot.dto.AccessTokenResponseDto;
import ch.unibas.medizin.depot.service.AccessTokenService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AccessTokenService accessTokenService;

    @PostMapping("/register")
    @Operation(summary = "Retrieve a token which provides access to the given realm")
    public ResponseEntity<AccessTokenResponseDto> register(@RequestBody @Valid final AccessTokenRequestDto accessTokenRequestDto) {
        var accessTokenResponseDto = accessTokenService.requestTokenString(accessTokenRequestDto);
        return ResponseEntity.ok(accessTokenResponseDto);
    }

    @PostMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Retrieve a QR code which provides access to the given realm")
    public byte[] qr(@RequestBody @Valid final AccessTokenRequestDto accessTokenRequestDto) {
        return accessTokenService.requestTokenQr(accessTokenRequestDto);
    }

}
