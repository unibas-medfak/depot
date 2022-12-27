package ch.unibas.medizin.depot.dto;

public record PutFileResponseDto(long bytes,
                                 String sha256) {
}
