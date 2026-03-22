package revi1337.onsquad.auth.token.application;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import revi1337.onsquad.auth.token.domain.model.AccessToken;
import revi1337.onsquad.auth.token.domain.model.ClaimsParser;
import revi1337.onsquad.auth.token.domain.model.RefreshToken;
import revi1337.onsquad.auth.token.infrastructure.TokenProperties;
import revi1337.onsquad.auth.token.infrastructure.TokenProperties.TokenAttributes;
import revi1337.onsquad.common.config.system.properties.OnsquadProperties;
import revi1337.onsquad.member.domain.vo.UserType;

@TestPropertySource(properties = {
        "onsquad.token.access-token.attributes.expired=5m",
        "onsquad.token.access-token.attributes.secret-key=11111111111111111111111111111111111111111111111111111111111111111111111111111111",
        "onsquad.token.refresh-token.attributes.expired=1d",
        "onsquad.token.refresh-token.attributes.secret-key=22222222222222222222222222222222222222222222222222222222222222222222222222222222"
})
@EnableConfigurationProperties({TokenProperties.class, OnsquadProperties.class})
@Import(JsonWebTokenProvider.class)
@ExtendWith(SpringExtension.class)
class JsonWebTokenProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JsonWebTokenProvider jsonWebTokenProvider;

    @Autowired
    private TokenProperties tokenProperties;

    @Test
    @DisplayName("AccessToken 생성에 성공한다.")
    void generateAccessToken() {
        String subject = ClaimsParser.ACCESS_TOKEN_SUBJECT;
        Map<String, Object> extraClaims = Map.of(
                ClaimsParser.IDENTITY_CLAIM, 1L,
                ClaimsParser.EMAIL_CLAIM, "test@email.com",
                ClaimsParser.USERTYPE_CLAIM, UserType.GENERAL
        );
        TokenAttributes attributes = tokenProperties.accessToken().attributes();
        Instant now = Instant.now();

        AccessToken accessToken = jsonWebTokenProvider.generateAccessToken(subject, extraClaims, attributes, now);

        assertSoftly(softly -> {
            softly.assertThat(accessToken).isNotNull();
            Map<String, String> payload = parseAccessTokenPayload(accessToken);
            softly.assertThat(payload.get("sub")).isEqualTo(subject);
            softly.assertThat(payload.get("jti")).isNotNull();
            softly.assertThat(payload.get("iat")).isNotNull();
            softly.assertThat(payload.get("exp")).isNotNull();
            softly.assertThat(payload.get(ClaimsParser.IDENTITY_CLAIM)).isEqualTo("1");
            softly.assertThat(payload.get(ClaimsParser.EMAIL_CLAIM)).isEqualTo("test@email.com");
            softly.assertThat(payload.get(ClaimsParser.USERTYPE_CLAIM)).isEqualTo(UserType.GENERAL.name());
        });
    }

    @Test
    @DisplayName("RefreshToken 생성에 성공한다.")
    void generateRefreshToken() {
        String subject = "refresh_token";
        Map<String, Long> extraClaims = Collections.singletonMap(ClaimsParser.IDENTITY_CLAIM, 2L);
        TokenAttributes attributes = tokenProperties.refreshToken().attributes();
        Instant now = Instant.now();

        RefreshToken refreshToken = jsonWebTokenProvider.generateRefreshToken(subject, extraClaims, attributes, now);

        assertSoftly(softly -> {
            softly.assertThat(refreshToken).isNotNull();
            Map<String, String> payload = parseRefreshTokenPayload(refreshToken);
            softly.assertThat(payload.get("sub")).isEqualTo(subject);
            softly.assertThat(payload.get("jti")).isNotNull();
            softly.assertThat(payload.get("iat")).isNotNull();
            softly.assertThat(payload.get("exp")).isNotNull();
            softly.assertThat(payload.get(ClaimsParser.IDENTITY_CLAIM)).isEqualTo("2");
            softly.assertThat(refreshToken.identifier()).isEqualTo(2L);
        });
    }

    private Map<String, String> parseAccessTokenPayload(AccessToken accessToken) {
        try {
            return parsePayload(accessToken.value());
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, String> parseRefreshTokenPayload(RefreshToken refreshToken) {
        try {
            return parsePayload(refreshToken.value());
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, String> parsePayload(String token) throws IOException {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        return objectMapper.readValue(
                payload,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class)
        );
    }
}
