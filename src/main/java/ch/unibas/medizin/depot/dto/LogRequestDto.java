package ch.unibas.medizin.depot.dto;

import jakarta.validation.constraints.NotBlank;

public record LogRequestDto(@NotBlank String password) {
}
