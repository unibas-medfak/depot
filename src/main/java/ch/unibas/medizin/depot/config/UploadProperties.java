package ch.unibas.medizin.depot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties(prefix = "depot.upload")
public record UploadProperties(
        DataSize maxSize,
        DataSize chunkSize,
        Duration ttl,
        int maxSessions
) {
    public UploadProperties {
        if (maxSize == null || maxSize.toBytes() <= 0) {
            maxSize = DataSize.ofGigabytes(10);
        }
        if (chunkSize == null || chunkSize.toBytes() <= 0) {
            chunkSize = DataSize.ofMegabytes(8);
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            ttl = Duration.ofHours(24);
        }
        if (maxSessions <= 0) {
            maxSessions = 10;
        }
    }
}
