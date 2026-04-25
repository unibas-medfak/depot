package ch.unibas.medizin.depot.security;

import ch.unibas.medizin.depot.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@NullMarked
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final Set<String> PROTECTED_PATHS = Set.of("/admin/register", "/admin/qr");

    private final RateLimitProperties properties;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!properties.enabled() || !PROTECTED_PATHS.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        final var clientId = request.getRemoteAddr();
        final var bucket = buckets.computeIfAbsent(clientId, k -> newBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded for {} on {}", clientId, request.getRequestURI());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"Rate limit exceeded\"}"
        );
    }

    private Bucket newBucket() {
        final var limit = Bandwidth.builder()
                .capacity(properties.maxAttempts())
                .refillIntervally(properties.maxAttempts(), properties.window())
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

}
