package revi1337.onsquad.auth.security.core;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import revi1337.onsquad.auth.support.CurrentMember;
import revi1337.onsquad.auth.token.application.JsonWebTokenManager;
import revi1337.onsquad.auth.token.domain.model.JsonWebToken;
import revi1337.onsquad.common.dto.RestResponse;
import revi1337.onsquad.common.util.ObjectMapperUtils;

@Slf4j
@RequiredArgsConstructor
public class JsonWebTokenSuccessHandler implements AuthenticationSuccessHandler {

    private static final String SERIALIZE_IO_ERROR_LOG_FORMAT = "인증 토큰 JSON 직렬화 또는 출력 중 예외 발생";
    private static final String LOGIN_LOG = "[사용자 로그인 성공] - id : {}, email : {}";

    private final JsonWebTokenManager jsonWebTokenManager;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication.getPrincipal() instanceof CurrentMember currentMember) {
            Instant now = Instant.now();
            JsonWebToken jsonWebToken = jsonWebTokenManager.issueJsonWebToken(currentMember.summary(), now);
            sendTokenResponseToClient(response, jsonWebToken);
            log.info(LOGIN_LOG, currentMember.id(), currentMember.email());
        }
    }

    private void sendTokenResponseToClient(HttpServletResponse response, JsonWebToken jsonWebToken) {
        try {
            response.setContentType(APPLICATION_JSON_VALUE);
            response.getWriter().write(ObjectMapperUtils.serializeToString(objectMapper, RestResponse.success(jsonWebToken)));

        } catch (IOException e) {
            log.error(SERIALIZE_IO_ERROR_LOG_FORMAT, e);
            throw new IllegalArgumentException(e);
        }
    }
}
