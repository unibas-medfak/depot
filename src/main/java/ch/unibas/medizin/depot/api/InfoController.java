package ch.unibas.medizin.depot.api;

import ch.unibas.medizin.depot.config.VersionHolder;
import ch.unibas.medizin.depot.dto.InfoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@NullMarked
@RestController
@Tag(name = "Info", description = "Public service info")
public class InfoController {

    private static final String GITHUB_URL = "https://github.com/unibas-medfak/depot";

    private static final String SWAGGER_URL = "/swagger-ui/index.html";

    private final VersionHolder versionHolder;

    public InfoController(VersionHolder versionHolder) {
        this.versionHolder = versionHolder;
    }

    @GetMapping("/info")
    @Operation(summary = "Service version and reference links")
    public ResponseEntity<InfoDto> info() {
        return ResponseEntity.ok(new InfoDto(versionHolder.getVersion(), GITHUB_URL, SWAGGER_URL));
    }

}
