package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.AccessTokenRequestDto;
import ch.unibas.medizin.depot.dto.AccessTokenResponseDto;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.nayuki.qrcodegen.QrCode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAccessTokenService implements AccessTokenService {

    private final DepotProperties depotProperties;

    private final PasswordEncoder passwordEncoder;

    private final LogService logService;

    @Override
    public AccessTokenResponseDto requestTokenString(AccessTokenRequestDto accessTokenRequestDto) {
        var token = getToken(accessTokenRequestDto);
        return new AccessTokenResponseDto(token);
    }

    @Override
    public byte[] requestTokenQr(AccessTokenRequestDto accessTokenRequestDto) {
        var token = getToken(accessTokenRequestDto);
        var qrCode = QrCode.encodeText(token, QrCode.Ecc.LOW);
        return toImage(qrCode);
    }

    @SneakyThrows
    private byte[] toImage(QrCode qr) {
        var scale = 4;
        var border = 10;
        var result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                boolean color = qr.getModule(x / scale - border, y / scale - border);
                result.setRGB(x, y, color ? 0x000000 : 0xFFFFFF);
            }
        }

        var byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(result, "png", byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    private String getToken(AccessTokenRequestDto accessTokenRequestDto) {
        if (!passwordEncoder.matches(accessTokenRequestDto.password(), depotProperties.adminPassword())) {
            log.error("Token request with invalid password");
            throw new AccessDeniedException("invalid password");
        }

        log.info("Token requested with realm={} subject={} mode={} expirationDate={}",
                accessTokenRequestDto.realm(),
                accessTokenRequestDto.subject(),
                accessTokenRequestDto.mode(),
                accessTokenRequestDto.expirationDate());

        var logString = String.format("%s %s %s", accessTokenRequestDto.realm(), accessTokenRequestDto.mode(), accessTokenRequestDto.expirationDate());
        logService.log(LogService.EventType.TOKEN, accessTokenRequestDto.subject(), logString);

        var zoneId = StringUtils.hasText(depotProperties.timeZone()) ? ZoneId.of(depotProperties.timeZone()) : ZoneId.systemDefault();
        var expirationDate = accessTokenRequestDto.expirationDate().atStartOfDay().atZone(zoneId).toInstant();

        return JWT.create()
                .withIssuer("depot")
                .withClaim("realm", accessTokenRequestDto.realm())
                .withClaim("mode", accessTokenRequestDto.mode().toLowerCase())
                .withSubject(accessTokenRequestDto.subject())
                .withExpiresAt(expirationDate)
                .sign(Algorithm.HMAC256(depotProperties.jwtSecret()));
    }

}
