package ch.unibas.medizin.depot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FileDto(@NotBlank String name,
                      @NotNull FileType type) {

    public enum FileType {
        FILE, FOLDER
    }

}
