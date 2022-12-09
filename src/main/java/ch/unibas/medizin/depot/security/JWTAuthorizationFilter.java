package ch.unibas.medizin.depot.security;

import ch.unibas.medizin.depot.config.DepotProperties;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
public class JWTAuthorizationFilter extends OncePerRequestFilter {

    private final String HEADER = "Authorization";
    private final String PREFIX = "Bearer ";

    private final JWTVerifier verifier;

    public JWTAuthorizationFilter(DepotProperties depotProperties) {
        Algorithm algorithm = Algorithm.HMAC256(depotProperties.jwtSecret());
        this.verifier = JWT.require(algorithm).withIssuer("depot").build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        if (checkJWTToken(httpServletRequest)) {
            var decodedJWT = validateToken(httpServletRequest);
            setUpSpringAuthentication(decodedJWT);
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    private DecodedJWT validateToken(HttpServletRequest httpServletRequest) {
        var token = httpServletRequest.getHeader(HEADER).replace(PREFIX, "");
        return verifier.verify(token);
    }

    private void setUpSpringAuthentication(DecodedJWT decodedJWT) {
        var realmAndSubject = String.format("%s%s%s", decodedJWT.getClaim("realm").asString(),
                String.valueOf(Character.LINE_SEPARATOR), decodedJWT.getSubject());

        var grantedAuthorities = new ArrayList<GrantedAuthority>();
        if (decodedJWT.getClaim("mode").asString().contains("r")) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_READ"));
        }
        if (decodedJWT.getClaim("mode").asString().contains("w")) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_WRITE"));
        }

        var authenticationToken = new UsernamePasswordAuthenticationToken(realmAndSubject, null, grantedAuthorities);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    private boolean checkJWTToken(HttpServletRequest httpServletRequest) {
        String authenticationHeader = httpServletRequest.getHeader(HEADER);
        return authenticationHeader != null && authenticationHeader.startsWith(PREFIX);
    }
}
