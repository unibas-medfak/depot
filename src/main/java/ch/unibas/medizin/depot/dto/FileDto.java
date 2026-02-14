package ch.unibas.medizin.depot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(name = "File")
public record FileDto(@NotBlank String name,
                      @NotNull FileType type,
                      @NotNull Long size,
                      @NotNull Instant modified,
                      @NotNull String hash) {

    public enum FileType {
        FILE, FOLDER
    }

}
