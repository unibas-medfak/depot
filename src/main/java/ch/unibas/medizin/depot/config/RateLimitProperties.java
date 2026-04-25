package ch.unibas.medizin.depot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "depot.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int maxAttempts,
        Duration window
) {
    public RateLimitProperties {
        if (maxAttempts <= 0) {
            maxAttempts = 5;
        }
        if (window == null || window.isZero() || window.isNegative()) {
            window = Duration.ofMinutes(15);
        }
    }
}
