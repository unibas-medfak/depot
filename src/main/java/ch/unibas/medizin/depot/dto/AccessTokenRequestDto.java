package ch.unibas.medizin.depot.dto;

import ch.unibas.medizin.depot.validation.ModeConstraint;
import ch.unibas.medizin.depot.validation.PathConstraint;
import ch.unibas.medizin.depot.validation.SubjectConstraint;
import tools.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(name = "AccessTokenRequest")
public record AccessTokenRequestDto(@Schema(example = "acme") @NotBlank @Size(max = 64) @PathConstraint String tenant,
                                    @Schema(example = "Top$ec3rit!") @NotBlank @Size(max = 64) String password,
                                    @Schema(description = "root folder", example = "exam101") @NotBlank @Size(max = 64) @PathConstraint String realm,
                                    @Schema(description = "client identifier", example = "iPad #213") @NotBlank @Size(max = 64) @SubjectConstraint String subject,
                                    @Schema(description = "access mode", allowableValues = {"r", "w", "d", "rw", "rd", "wd", "rwd"}) @NotBlank @ModeConstraint String mode,
                                    @Schema(description = "expiration as ISO date (yyyy-MM-dd, interpreted as UTC start of day) or full ISO date-time (e.g. 2025-12-31T23:59:59Z)", example = "2025-12-31") @NotNull @Future @JsonDeserialize(using = ExpirationDeserializer.class) Instant expirationDate) {
}
