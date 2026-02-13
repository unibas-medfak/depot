package ch.unibas.medizin.depot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PutFileResponse")
public record PutFileResponseDto(long bytes,
                                 String hash) {
}
