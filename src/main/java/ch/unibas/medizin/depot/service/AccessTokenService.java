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
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Locale;

@Service
@NullMarked
public record AccessTokenService(
        DepotProperties depotProperties,
        AuthorizationService authorizationService,
        LogService logService,
        ObjectMapper objectMapper
) {

    private static final Logger log = LoggerFactory.getLogger(AccessTokenService.class);

    public AccessTokenResponseDto requestTokenString(final AccessTokenRequestDto accessTokenRequestDto) {
        final var token = getToken(accessTokenRequestDto);
        return new AccessTokenResponseDto(token);
    }

    public byte[] requestTokenQr(final AccessTokenRequestDto accessTokenRequestDto) {
        final var host = depotProperties.getHost();
        final var token = getToken(accessTokenRequestDto);
        final var qrCodePayload = new QrCodePayloadDto(host, token);

        try {
            final var jsonQrCodePayload = objectMapper.writeValueAsString(qrCodePayload);
            final var qrCode = QrCode.encodeText(jsonQrCodePayload, QrCode.Ecc.LOW);
            return toImage(qrCode);
        } catch (JsonProcessingException e) {
            log.error("Could not encode payload", e);
            throw new RuntimeException(e);
        }
    }

    private byte[] toImage(QrCode qr) {
        final var scale = 4;
        final var border = 10;
        final var result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                boolean color = qr.getModule(x / scale - border, y / scale - border);
                result.setRGB(x, y, color ? 0x000000 : 0xFFFFFF);
            }
        }

        final var byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(result, "png", byteArrayOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private String getToken(AccessTokenRequestDto accessTokenRequestDto) {
        authorizationService.throwIfAdminPasswordMismatches(accessTokenRequestDto.tenant(), accessTokenRequestDto.password());

        log.info("Token requested with tenant={} realm={} subject={} mode={} expirationDate={}",
                accessTokenRequestDto.tenant(),
                accessTokenRequestDto.realm(),
                accessTokenRequestDto.subject(),
                accessTokenRequestDto.mode(),
                accessTokenRequestDto.expirationDate());

        final var logString = String.format("%s %s %s %s", accessTokenRequestDto.tenant(), accessTokenRequestDto.realm(), accessTokenRequestDto.mode(), accessTokenRequestDto.expirationDate());
        logService.log(accessTokenRequestDto.tenant(), LogService.EventType.TOKEN, accessTokenRequestDto.subject(), logString);

        final var zoneId = StringUtils.hasText(depotProperties.getTimeZone()) ? ZoneId.of(depotProperties.getTimeZone()) : ZoneId.systemDefault();
        final var expirationDate = accessTokenRequestDto.expirationDate().atStartOfDay().atZone(zoneId).toInstant();

        return JWT.create()
                .withIssuer("depot")
                .withClaim("tenant", accessTokenRequestDto.tenant())
                .withClaim("realm", accessTokenRequestDto.realm())
                .withClaim("mode", accessTokenRequestDto.mode().toLowerCase(Locale.getDefault()))
                .withSubject(accessTokenRequestDto.subject())
                .withExpiresAt(expirationDate)
                .sign(Algorithm.HMAC256(depotProperties.getJwtSecret()));
    }

}
