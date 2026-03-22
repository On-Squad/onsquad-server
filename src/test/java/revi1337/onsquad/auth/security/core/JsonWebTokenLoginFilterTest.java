package revi1337.onsquad.auth.security.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import revi1337.onsquad.auth.support.CurrentMember;
import revi1337.onsquad.auth.token.domain.model.AccessToken;
import revi1337.onsquad.auth.token.domain.model.JsonWebToken;
import revi1337.onsquad.auth.token.domain.model.RefreshToken;
import revi1337.onsquad.common.PresentationLayerTestSupport;
import revi1337.onsquad.member.domain.vo.UserType;

@WebMvcTest(JsonWebTokenLoginFilter.class)
class JsonWebTokenLoginFilterTest extends PresentationLayerTestSupport {

    @MockBean
    private JsonWebTokenUserDetailsService jsonWebTokenUserDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    protected void setUp(
            final WebApplicationContext applicationContext,
            final RestDocumentationContextProvider restDocumentationProvider
    ) {
        JsonWebTokenAuthenticationProvider provider = new JsonWebTokenAuthenticationProvider(passwordEncoder, jsonWebTokenUserDetailsService);
        AuthenticationManager authenticationManager = new ProviderManager(provider);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        JsonWebTokenFailureHandler failureHandler = new JsonWebTokenFailureHandler();
        HandlerExceptionResolver exceptionResolver = applicationContext.getBean("handlerExceptionResolver", HandlerExceptionResolver.class);
        ReflectionTestUtils.setField(failureHandler, "handlerExceptionResolver", exceptionResolver);

        JsonWebTokenLoginFilter filter = new JsonWebTokenLoginFilter(authenticationManager, objectMapper, validator);
        filter.setAuthenticationSuccessHandler(new JsonWebTokenSuccessHandler(jsonWebTokenManager, objectMapper));
        filter.setAuthenticationFailureHandler(failureHandler);

        this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(MockMvcRestDocumentation.documentationConfiguration(restDocumentationProvider))
                .alwaysDo(MockMvcResultHandlers.print())
                .addFilter(new CharacterEncodingFilter("UTF-8", true))
                .addFilter(filter)
                .build();
    }

    @Test
    @DisplayName("로그인에 성공을 문서화한다.")
    void success() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@gmail.com", "test-password");
        CurrentMember currentMember = new CurrentMember(
                1L,
                "test@gmail.com",
                "test-password",
                UserType.GENERAL,
                Set.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(jsonWebTokenUserDetailsService.loadUserByUsername("test@gmail.com")).thenReturn(currentMember);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        JsonWebToken expectedToken = JsonWebToken.create(new AccessToken(ACCESS_TOKEN), new RefreshToken(REFRESH_TOKEN));
        when(jsonWebTokenManager.issueJsonWebToken(any(), any())).thenReturn(expectedToken);

        mockMvc.perform(post("/api/auth/login")
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .contentType(APPLICATION_JSON))
                .andDo(document("auth/success/login",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseBody()
                ));
    }
}
