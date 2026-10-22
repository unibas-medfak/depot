package ch.unibas.medizin.depot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "UploadStatus")
public record UploadStatusDto(
        String uploadId,
        long size,
        long bytesReceived,
        @Schema(description = "Maximum number of bytes accepted per chunk") long chunkSize,
        Instant expiresAt) {
}
