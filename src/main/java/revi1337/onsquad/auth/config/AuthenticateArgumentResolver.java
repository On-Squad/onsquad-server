package revi1337.onsquad.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import revi1337.onsquad.auth.application.JsonWebTokenEvaluator;
import revi1337.onsquad.auth.dto.AuthenticatedMember;
import revi1337.onsquad.auth.error.exception.AuthTokenException;

import static revi1337.onsquad.auth.error.AuthErrorCode.*;

@RequiredArgsConstructor
public class AuthenticateArgumentResolver implements HandlerMethodArgumentResolver {

    private static final int TOKEN_INDEX = 1;
    private static final String EXTRACT_KEY = "identifier";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JsonWebTokenEvaluator jsonWebTokenEvaluator;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Authenticate.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String authorizationHeader = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        validateAuthorizationHeader(authorizationHeader);

        String accessToken = authorizationHeader.split(" ")[TOKEN_INDEX];
        Long memberId = jsonWebTokenEvaluator.extractSpecificClaim(
                jsonWebTokenEvaluator.verifyAccessToken(accessToken),
                extracted -> extracted.get(EXTRACT_KEY, Long.class)
        );

        return AuthenticatedMember.of(memberId);
    }

    private void validateAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null) {
            throw new AuthTokenException.NeedToken(EMPTY_TOKEN);
        }

        if (!authorizationHeader.startsWith(TOKEN_PREFIX)) {
            throw new AuthTokenException.InvalidTokenFormat(INVALID_TOKEN_FORMAT);
        }
    }
}
