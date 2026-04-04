package revi1337.onsquad.auth.security.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import revi1337.onsquad.auth.security.error.UnsupportedLoginUrlMethod;
import revi1337.onsquad.common.error.CommonErrorCode;
import revi1337.onsquad.common.util.ObjectMapperUtils;

public class JsonWebTokenLoginFilter extends AbstractAuthenticationProcessingFilter {

    private static final String DEFAULT_LOGIN_URL = "/api/auth/login";
    private static final String ALLOW_HTTP_METHOD = "POST";
    private static final AntPathRequestMatcher LOGIN_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher(DEFAULT_LOGIN_URL, ALLOW_HTTP_METHOD);

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Qualifier("handlerExceptionResolver")
    @Autowired
    private HandlerExceptionResolver handlerExceptionResolver;

    public JsonWebTokenLoginFilter(
            AuthenticationManager authenticationManager,
            ObjectMapper objectMapper,
            Validator validator
    ) {
        super(LOGIN_ANT_PATH_REQUEST_MATCHER, authenticationManager);
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        if (!PatternMatchUtils.simpleMatch(DEFAULT_LOGIN_URL, request.getRequestURI())) {
            return false;
        }

        boolean matches = LOGIN_ANT_PATH_REQUEST_MATCHER.matches(request);
        if (!matches && !response.isCommitted()) {
            try {
                getFailureHandler().onAuthenticationFailure(request, response,
                        new UnsupportedLoginUrlMethod(
                                CommonErrorCode.METHOD_NOT_SUPPORT,
                                new HttpRequestMethodNotSupportedException(
                                        request.getMethod(), Collections.singleton(HttpMethod.POST.name())
                                )
                        )
                );
            } catch (ServletException | IOException ignored) {
            }
        }

        return matches;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            final String jsonString = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            final LoginRequest loginRequest = ObjectMapperUtils.deserialize(objectMapper, jsonString, LoginRequest.class);
            if (!isValidOrHandleError(loginRequest, request, response)) {
                return null;
            }

            final UsernamePasswordAuthenticationToken unauthenticatedToken = UsernamePasswordAuthenticationToken
                    .unauthenticated(loginRequest.email(), loginRequest.password());

            return getAuthenticationManager().authenticate(unauthenticatedToken);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean isValidOrHandleError(LoginRequest loginReq, HttpServletRequest request, HttpServletResponse response) {
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginReq);
        if (violations.isEmpty()) {
            return true;
        }

        ConstraintViolationException violationException = new ConstraintViolationException(violations);
        handlerExceptionResolver.resolveException(request, response, null, violationException);
        return false;
    }
}
