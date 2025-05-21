package revi1337.onsquad.auth.application.token;

import static revi1337.onsquad.auth.error.TokenErrorCode.INVALID_TOKEN_SIGNATURE;
import static revi1337.onsquad.auth.error.TokenErrorCode.TOKEN_EXPIRED;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.security.Key;
import org.springframework.stereotype.Component;
import revi1337.onsquad.auth.config.properties.TokenProperties;
import revi1337.onsquad.auth.config.properties.TokenProperties.AccessTokenAttributes;
import revi1337.onsquad.auth.config.properties.TokenProperties.RefreshTokenAttributes;
import revi1337.onsquad.auth.error.exception.AuthTokenException;

@Component
public class JsonWebTokenEvaluator {

    private final AccessTokenAttributes accessTokenAttributes;
    private final RefreshTokenAttributes refreshTokenAttributes;

    public JsonWebTokenEvaluator(TokenProperties tokenProperties) {
        this.accessTokenAttributes = tokenProperties.accessTokenAttributes();
        this.refreshTokenAttributes = tokenProperties.refreshTokenAttributes();
    }

    public ClaimsParser verifyAccessToken(String accessToken) {
        String accessTokenSecretKey = accessTokenAttributes.tokenAttributes().secretKey();
        return new ClaimsParser(verifyToken(accessToken, accessTokenSecretKey));
    }

    public ClaimsParser verifyRefreshToken(String refreshToken) {
        String refreshTokenSecretKey = refreshTokenAttributes.tokenAttributes().secretKey();
        return new ClaimsParser(verifyToken(refreshToken, refreshTokenSecretKey));
    }

    private Claims verifyToken(String token, String secretKey) {
        try {
            return extractAllClaims(token, secretKey);
        } catch (SignatureException e) {
            throw new AuthTokenException.InvalidTokenSignature(INVALID_TOKEN_SIGNATURE);
        } catch (ExpiredJwtException e) {
            throw new AuthTokenException.TokenExpired(TOKEN_EXPIRED);
        }
    }

    private Claims extractAllClaims(String token, String secretKey) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey(secretKey))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey(String keyType) {
        return Keys.hmacShaKeyFor(keyType.getBytes());
    }
}
