package revi1337.onsquad.auth.token.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Duration;
import java.time.Instant;
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
import revi1337.onsquad.auth.token.domain.error.TokenException;
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
@Import({JsonWebTokenEvaluator.class, JsonWebTokenProvider.class})
@ExtendWith(SpringExtension.class)
class JsonWebTokenEvaluatorTest {

    @Autowired
    private JsonWebTokenEvaluator jsonWebTokenEvaluator;

    @Autowired
    private JsonWebTokenProvider jsonWebTokenProvider;

    @Autowired
    private TokenProperties tokenProperties;

    private static final String SUBJECT = "test-subject";
    private static final Map<String, Object> ACCESS_TOKEN_EXTRA_CLAIMS = Map.of(
            ClaimsParser.IDENTITY_CLAIM, 1L,
            ClaimsParser.EMAIL_CLAIM, "test@email.com",
            ClaimsParser.USERTYPE_CLAIM, UserType.GENERAL
    );
    private static final Map<String, Long> REFRESH_TOKEN_EXTRA_CLAIMS = Collections.singletonMap(ClaimsParser.IDENTITY_CLAIM, 2L);

    @Test
    @DisplayName("유효한 AccessToken 검증 및 클레임 파싱에 성공한다.")
    void verifyAccessTokenSuccess() {
        TokenAttributes attributes = tokenProperties.accessToken().attributes();
        AccessToken accessToken = jsonWebTokenProvider.generateAccessToken(SUBJECT, ACCESS_TOKEN_EXTRA_CLAIMS, attributes, Instant.now());

        ClaimsParser parser = jsonWebTokenEvaluator.verifyAccessToken(accessToken.value(), attributes.secretKey());

        assertSoftly(softly -> {
            softly.assertThat(parser).isNotNull();
            softly.assertThat(parser.parseIdentity()).isEqualTo(1L);
            softly.assertThat(parser.parseEmail()).isEqualTo("test@email.com");
            softly.assertThat(parser.parseUserType()).isEqualTo(UserType.GENERAL);
        });
    }

    @Test
    @DisplayName("유효한 RefreshToken 검증 및 클레임 파싱에 성공한다.")
    void verifyRefreshTokenSuccess() {
        TokenAttributes attributes = tokenProperties.accessToken().attributes();
        RefreshToken refreshToken = jsonWebTokenProvider.generateRefreshToken(SUBJECT, REFRESH_TOKEN_EXTRA_CLAIMS, attributes, Instant.now());

        ClaimsParser parser = jsonWebTokenEvaluator.verifyRefreshToken(refreshToken.value(), attributes.secretKey());

        assertSoftly(softly -> {
            softly.assertThat(parser).isNotNull();
            softly.assertThat(parser.parseIdentity()).isEqualTo(2L);
        });
    }

    @Test
    @DisplayName("잘못된 서명(SecretKey)으로 검증 시 InvalidTokenSignature 예외가 발생한다.")
    void verifyTokenInvalidSignature() {
        TokenAttributes attributes = tokenProperties.accessToken().attributes();
        AccessToken accessToken = jsonWebTokenProvider.generateAccessToken(SUBJECT, ACCESS_TOKEN_EXTRA_CLAIMS, attributes, Instant.now());
        String wrongSecretKey = "wrong-secret-key-at-least-64-bytes-long-1111111111111111111111111111111111";

        assertThatThrownBy(() -> jsonWebTokenEvaluator.verifyAccessToken(accessToken.value(), wrongSecretKey))
                .isInstanceOf(TokenException.InvalidTokenSignature.class);
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 TokenExpired 예외가 발생한다.")
    void verifyTokenExpired() {
        TokenAttributes expiredAttributes = new TokenAttributes(Duration.ofMinutes(-10), tokenProperties.accessToken().attributes().secretKey());
        AccessToken accessToken = jsonWebTokenProvider.generateAccessToken(SUBJECT, ACCESS_TOKEN_EXTRA_CLAIMS, expiredAttributes, Instant.now());

        assertThatThrownBy(() -> jsonWebTokenEvaluator.verifyAccessToken(accessToken.value(), expiredAttributes.secretKey()))
                .isInstanceOf(TokenException.TokenExpired.class);
    }
}
