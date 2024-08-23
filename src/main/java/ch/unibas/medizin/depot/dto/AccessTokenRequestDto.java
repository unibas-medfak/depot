package ch.unibas.medizin.depot.dto;

import ch.unibas.medizin.depot.validation.ModeConstraint;
import ch.unibas.medizin.depot.validation.PathConstraint;
import ch.unibas.medizin.depot.validation.SubjectConstraint;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(name = "AccessTokenRequest")
public record AccessTokenRequestDto(@Schema(example = "acme") @NotBlank @Size(max = 64) @PathConstraint String tenant,
                                    @Schema(example = "Top$ec3rit!") @NotBlank @Size(max = 64) String password,
                                    @Schema(description = "root folder", example = "exam") @NotBlank @Size(max = 64) @PathConstraint String realm,
                                    @Schema(description = "client identifier", example = "iPad #213") @NotBlank @Size(max = 64) @SubjectConstraint String subject,
                                    @Schema(description = "access mode", allowableValues = {"r", "w", "d", "rw", "rd", "wd", "rwd"}) @NotBlank @ModeConstraint String mode,
                                    @Schema(description = "date of expiration", example = "2025-12-31") @NotNull @Future @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") @JsonDeserialize(using = LocalDateDeserializer.class) @JsonSerialize(using = LocalDateSerializer.class) LocalDate expirationDate) {
}
