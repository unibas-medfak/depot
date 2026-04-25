package ch.unibas.medizin.depot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Info")
public record InfoDto(String version, String github, String swagger) {
}
