package ch.unibas.medizin.depot.security;

import ch.unibas.medizin.depot.config.DepotProperties;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@NullMarked
public class JWTAuthorizationFilter extends OncePerRequestFilter {

    public static final String TOKEN_DATA_DELIMITER = String.valueOf(Character.LINE_SEPARATOR);

    private final String HEADER = "Authorization";

    private final String PREFIX = "Bearer ";

    private final JWTVerifier verifier;

    public JWTAuthorizationFilter(final DepotProperties depotProperties) {
        final var algorithm = Algorithm.HMAC256(depotProperties.getJwtSecret());
        this.verifier = JWT.require(algorithm).withIssuer("depot").build();
    }

    @Override
    protected void doFilterInternal(final @NonNull HttpServletRequest httpServletRequest, final @NonNull HttpServletResponse httpServletResponse, final @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (checkJWTToken(httpServletRequest)) {
            final var decodedJWT = validateToken(httpServletRequest);
            setUpSpringAuthentication(decodedJWT);
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    private DecodedJWT validateToken(final HttpServletRequest httpServletRequest) {
        final var token = httpServletRequest.getHeader(HEADER).replace(PREFIX, "");
        return verifier.verify(token);
    }

    private void setUpSpringAuthentication(final DecodedJWT decodedJWT) {
        final var tenantRealmAndSubject = String.format(
                "%s%s%s%s%s",
                decodedJWT.getClaim("tenant").asString(),
                JWTAuthorizationFilter.TOKEN_DATA_DELIMITER,
                decodedJWT.getClaim("realm").asString(),
                JWTAuthorizationFilter.TOKEN_DATA_DELIMITER,
                decodedJWT.getSubject()
        );

        final var grantedAuthorities = new ArrayList<GrantedAuthority>();
        if (decodedJWT.getClaim("mode").asString().contains("r")) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_READ"));
        }
        if (decodedJWT.getClaim("mode").asString().contains("w")) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_WRITE"));
        }
        if (decodedJWT.getClaim("mode").asString().contains("d")) {
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
