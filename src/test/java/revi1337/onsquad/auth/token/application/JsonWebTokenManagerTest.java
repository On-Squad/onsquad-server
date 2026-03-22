package revi1337.onsquad.auth.token.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import revi1337.onsquad.auth.token.domain.error.TokenException;
import revi1337.onsquad.auth.token.domain.model.ClaimsParser;
import revi1337.onsquad.auth.token.domain.model.JsonWebToken;
import revi1337.onsquad.auth.token.domain.model.RefreshToken;
import revi1337.onsquad.auth.token.infrastructure.TokenProperties;
import revi1337.onsquad.auth.token.infrastructure.persistence.RedisRefreshTokenStorage;
import revi1337.onsquad.common.config.system.properties.OnsquadProperties;
import revi1337.onsquad.common.container.RedisTestContainerInitializer;
import revi1337.onsquad.member.application.dto.MemberSummary;
import revi1337.onsquad.member.domain.vo.UserType;

@TestPropertySource(properties = {
        "onsquad.token.access-token.attributes.expired=5m",
        "onsquad.token.access-token.attributes.secret-key=11111111111111111111111111111111111111111111111111111111111111111111111111111111",
        "onsquad.token.refresh-token.attributes.expired=1d",
        "onsquad.token.refresh-token.attributes.secret-key=22222222222222222222222222222222222222222222222222222222222222222222222222222222"
})
@EnableConfigurationProperties({TokenProperties.class, OnsquadProperties.class})
@Import({JsonWebTokenProvider.class, JsonWebTokenEvaluator.class, RedisRefreshTokenStorage.class})
@ContextConfiguration(initializers = RedisTestContainerInitializer.class)
@ImportAutoConfiguration(RedisAutoConfiguration.class)
@ExtendWith(SpringExtension.class)
class JsonWebTokenManagerTest {

    private final JsonWebTokenManager jsonWebTokenManager;
    private final RedisRefreshTokenStorage refreshTokenStorage;
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public JsonWebTokenManagerTest(
            TokenProperties tokenProperties,
            JsonWebTokenProvider jsonWebTokenProvider,
            JsonWebTokenEvaluator jsonWebTokenEvaluator,
            RedisRefreshTokenStorage refreshTokenStorage,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.refreshTokenStorage = refreshTokenStorage;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jsonWebTokenManager = new JsonWebTokenManager(
                tokenProperties,
                jsonWebTokenProvider,
                jsonWebTokenEvaluator,
                refreshTokenStorage
        );
    }

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("토큰을 발행하면 AccessToken과 RefreshToken을 반환하고, RefreshToken을 저장소에 저장한다.")
    void issueJsonWebTokenSuccess() {
        MemberSummary summary = new MemberSummary(1L, "revi1337@gmail.com", "password", UserType.GENERAL);
        Instant now = Instant.now();

        JsonWebToken issuedToken = jsonWebTokenManager.issueJsonWebToken(summary, now);

        assertSoftly(softly -> {
            softly.assertThat(issuedToken).isNotNull();
            softly.assertThat(issuedToken.accessToken()).isNotNull();
            softly.assertThat(issuedToken.refreshToken()).isNotNull();

            Optional<RefreshToken> storedToken = refreshTokenStorage.findTokenBy(summary.id());
            softly.assertThat(storedToken).isPresent();
            softly.assertThat(storedToken.get().value()).isEqualTo(issuedToken.refreshToken());
        });
    }

    @Test
    @DisplayName("발행된 AccessToken을 성공적으로 검증한다.")
    void verifyAccessTokenSuccess() {
        MemberSummary summary = new MemberSummary(1L, "revi1337@gmail.com", "password", UserType.GENERAL);
        JsonWebToken issuedToken = jsonWebTokenManager.issueJsonWebToken(summary, Instant.now());

        ClaimsParser claimsParser = jsonWebTokenManager.verifyAccessToken(issuedToken.accessToken());

        assertSoftly(softly -> {
            softly.assertThat(claimsParser.parseIdentity()).isEqualTo(summary.id());
            softly.assertThat(claimsParser.parseEmail()).isEqualTo(summary.email());
            softly.assertThat(claimsParser.parseUserType()).isEqualTo(summary.userType());
        });
    }

    @Test
    @DisplayName("RefreshToken 검증 시, 저장소에 있는 값과 일치하면 성공한다.")
    void verifyRefreshTokenSuccess() {
        MemberSummary summary = new MemberSummary(1L, "revi1337@gmail.com", "password", UserType.GENERAL);
        JsonWebToken issuedToken = jsonWebTokenManager.issueJsonWebToken(summary, Instant.now());

        ClaimsParser claimsParser = jsonWebTokenManager.verifyRefreshToken(issuedToken.refreshToken());

        assertThat(claimsParser.parseIdentity()).isEqualTo(summary.id());
    }

    @Test
    @DisplayName("RefreshToken 검증 시, 저장소에 토큰이 없거나 값이 다르면 NotFoundRefresh 예외가 발생한다.")
    void verifyRefreshTokenFail() {
        MemberSummary summary = new MemberSummary(1L, "revi1337@gmail.com", "password", UserType.GENERAL);
        JsonWebToken issuedToken = jsonWebTokenManager.issueJsonWebToken(summary, Instant.now());

        refreshTokenStorage.deleteTokenBy(summary.id());

        assertThatThrownBy(() -> jsonWebTokenManager.verifyRefreshToken(issuedToken.refreshToken()))
                .isInstanceOf(TokenException.NotFoundRefresh.class);
    }
}
