package ch.unibas.medizin.depot;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.config.RateLimitProperties;
import ch.unibas.medizin.depot.config.TikaRuntimeHints;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.Locale;

@EnableConfigurationProperties({DepotProperties.class, RateLimitProperties.class})
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@OpenAPIDefinition(info = @Info(title = "Depot API", version = "1.0", description = "Secure file storage API"))
@SecurityScheme(name = "depotapi", scheme = "bearer", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
@ImportRuntimeHints(TikaRuntimeHints.class)
public class DepotApplication {

    @SuppressWarnings("UnnecessaryModifier")
    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        SpringApplication.run(DepotApplication.class, args);
    }

}
