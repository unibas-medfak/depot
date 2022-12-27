package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.dto.AccessTokenRequestDto;
import ch.unibas.medizin.depot.dto.AccessTokenResponseDto;
import ch.unibas.medizin.depot.dto.LogRequestDto;
import ch.unibas.medizin.depot.service.AccessTokenService;
import ch.unibas.medizin.depot.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AccessTokenService accessTokenService;

    private final AdminService adminService;

    public AdminController(AccessTokenService accessTokenService, AdminService adminService) {
        this.accessTokenService = accessTokenService;
        this.adminService = adminService;
    }

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

    @PostMapping("/log")
    @Operation(summary = "Retrieve the last 100 logged events")
    public List<String> log(@RequestBody @Valid final LogRequestDto logRequestDto) {
        return adminService.getLastLogLines(logRequestDto);
    }

}
