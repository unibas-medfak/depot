package ch.unibas.medizin.depot.service;

import ch.unibas.medizin.depot.dto.AccessTokenRequestDto;
import ch.unibas.medizin.depot.dto.AccessTokenResponseDto;

public interface AccessTokenService {
    AccessTokenResponseDto requestTokenString(AccessTokenRequestDto accessTokenRequestDto);
    byte[] requestTokenQr(AccessTokenRequestDto accessTokenRequestDto);
}
