package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.config.DepotProperties;
import ch.unibas.medizin.depot.dto.AccessTokenRequestDto;
import ch.unibas.medizin.depot.dto.AccessTokenResponseDto;
import ch.unibas.medizin.depot.dto.QrCodePayloadDto;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nayuki.qrcodegen.QrCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;

@Service
public class DefaultAccessTokenService implements AccessTokenService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAccessTokenService.class);

    private final DepotProperties depotProperties;

    private final AuthorizationService authorizationService;

    private final LogService logService;

    private final ObjectMapper objectMapper;

    public DefaultAccessTokenService(DepotProperties depotProperties, AuthorizationService authorizationService, LogService logService, ObjectMapper objectMapper) {
        this.depotProperties = depotProperties;
        this.authorizationService = authorizationService;
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AccessTokenResponseDto requestTokenString(AccessTokenRequestDto accessTokenRequestDto) {
        var token = getToken(accessTokenRequestDto);
        return new AccessTokenResponseDto(token);
    }

    @Override
    public byte[] requestTokenQr(AccessTokenRequestDto accessTokenRequestDto) {
        var host = depotProperties.getHost();
        var token = getToken(accessTokenRequestDto);
        var qrCodePayload = new QrCodePayloadDto(host, token);

        try {
            var jsonQrCodePayload = objectMapper.writeValueAsString(qrCodePayload);
            var qrCode = QrCode.encodeText(jsonQrCodePayload, QrCode.Ecc.LOW);
            return toImage(qrCode);
        } catch (JsonProcessingException e) {
            log.error("Could not encode payload", e);
            throw new RuntimeException(e);
        }
    }

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
        try {
            ImageIO.write(result, "png", byteArrayOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private String getToken(AccessTokenRequestDto accessTokenRequestDto) {
        if (authorizationService.adminPasswordMismatches(accessTokenRequestDto.password())) {
            return "";
        }

        log.info("Token requested with realm={} subject={} mode={} expirationDate={}",
                accessTokenRequestDto.realm(),
                accessTokenRequestDto.subject(),
                accessTokenRequestDto.mode(),
                accessTokenRequestDto.expirationDate());

        var logString = String.format("%s %s %s", accessTokenRequestDto.realm(), accessTokenRequestDto.mode(), accessTokenRequestDto.expirationDate());
        logService.log(LogService.EventType.TOKEN, accessTokenRequestDto.subject(), logString);

        var zoneId = StringUtils.hasText(depotProperties.getTimeZone()) ? ZoneId.of(depotProperties.getTimeZone()) : ZoneId.systemDefault();
        var expirationDate = accessTokenRequestDto.expirationDate().atStartOfDay().atZone(zoneId).toInstant();

        return JWT.create()
                .withIssuer("depot")
                .withClaim("realm", accessTokenRequestDto.realm())
                .withClaim("mode", accessTokenRequestDto.mode().toLowerCase())
                .withSubject(accessTokenRequestDto.subject())
                .withExpiresAt(expirationDate)
                .sign(Algorithm.HMAC256(depotProperties.getJwtSecret()));
    }

}
