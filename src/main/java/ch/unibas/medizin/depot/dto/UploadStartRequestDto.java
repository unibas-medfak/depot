package ch.unibas.medizin.depot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(name = "UploadStartRequest")
public record UploadStartRequestDto(
        @Schema(description = "Path where the file will be stored", example = "pictures/cats")
        @Nullable String path,
        @Schema(description = "Name of the file to be stored", example = "cat.png")
        @Nullable String filename,
        @Schema(description = "Total size of the file in bytes", example = "3221225472")
        long size,
        @Schema(description = "Whether the completion response shall contain a Murmur3 hash of the stored data", example = "true")
        boolean hash) {
}
