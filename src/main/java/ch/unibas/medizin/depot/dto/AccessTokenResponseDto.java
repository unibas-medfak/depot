package ch.unibas.medizin.depot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AccessTokenResponse")
public record AccessTokenResponseDto(String token) {
}
