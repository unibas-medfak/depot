package ch.unibas.medizin.depot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LogRequest")
public record LogRequestDto(@NotBlank String tenant, @NotBlank String password) {
}
