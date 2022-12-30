package ch.unibas.medizin.depot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "File")
public record FileDto(@NotBlank String name,
                      @NotNull FileType type) {

    public enum FileType {
        FILE, FOLDER
    }

}
