package ch.unibas.medizin.depot.security;

import ch.unibas.medizin.depot.config.DepotProperties;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@NullMarked
public class JWTAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JWTAuthorizationFilter.class);

    // Unit separator: a single ISO control character, so it can never appear in a tenant,
    // realm or subject (DepotUtil rejects ISO control characters in all three).
    public static final String TOKEN_DATA_DELIMITER = "\u001F";

    private final String HEADER = "Authorization";

    private final String PREFIX = "Bearer ";

    private final JWTVerifier verifier;

    public JWTAuthorizationFilter(final DepotProperties depotProperties) {
        final var algorithm = Algorithm.HMAC256(depotProperties.getJwtSecret());
        this.verifier = JWT.require(algorithm).withIssuer("depot").build();
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final FilterChain filterChain) throws ServletException, IOException {
        if (!checkJWTToken(httpServletRequest)) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        final DecodedJWT decodedJWT;
        try {
            decodedJWT = validateToken(httpServletRequest);
        } catch (JWTVerificationException e) {
            log.warn("Rejected JWT {} {} {} {}",
                    kv("method", httpServletRequest.getMethod()), kv("uri", httpServletRequest.getRequestURI()), kv("client", httpServletRequest.getRemoteAddr()), kv("reason", e.getMessage()));
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        final var tenant = decodedJWT.getClaim("tenant").asString();
        final var realm = decodedJWT.getClaim("realm").asString();
        final var subject = decodedJWT.getSubject();
        final var mode = decodedJWT.getClaim("mode").asString();

        if (tenant == null || realm == null || subject == null || mode == null) {
            log.warn("Rejected JWT with missing claims {} {} {}",
                    kv("method", httpServletRequest.getMethod()), kv("uri", httpServletRequest.getRequestURI()), kv("client", httpServletRequest.getRemoteAddr()));
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        setUpSpringAuthentication(tenant, realm, subject, mode);

        // Exposed as separate fields in the JSON log for everything logged during this request
        MDC.put("tenant", tenant);
        MDC.put("realm", realm);
        MDC.put("subject", subject);
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            MDC.remove("tenant");
            MDC.remove("realm");
            MDC.remove("subject");
        }
    }

    private DecodedJWT validateToken(final HttpServletRequest httpServletRequest) {
        final var token = httpServletRequest.getHeader(HEADER).replace(PREFIX, "");
        return verifier.verify(token);
    }

    private void setUpSpringAuthentication(final String tenant, final String realm, final String subject, final String mode) {
        final var tenantRealmAndSubject = String.format(
                "%s%s%s%s%s",
                tenant,
                JWTAuthorizationFilter.TOKEN_DATA_DELIMITER,
                realm,
                JWTAuthorizationFilter.TOKEN_DATA_DELIMITER,
                subject
        );

        final var grantedAuthorities = new ArrayList<GrantedAuthority>();
        if (mode.contains("r")) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_READ"));
        }
        if (mode.contains("w")) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_WRITE"));
        }
        if (mode.contains("d")) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_DELETE"));
        }

        final var authenticationToken = new UsernamePasswordAuthenticationToken(tenantRealmAndSubject, null, grantedAuthorities);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    private boolean checkJWTToken(final HttpServletRequest httpServletRequest) {
        final var authenticationHeader = httpServletRequest.getHeader(HEADER);
        return authenticationHeader != null && authenticationHeader.startsWith(PREFIX);
    }
}
