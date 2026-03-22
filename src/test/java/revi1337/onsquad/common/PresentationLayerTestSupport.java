package revi1337.onsquad.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import revi1337.onsquad.auth.token.application.JsonWebTokenManager;
import revi1337.onsquad.auth.token.domain.model.ClaimsParser;
import revi1337.onsquad.common.config.PresentationLayerConfiguration;
import revi1337.onsquad.common.config.web.WebMvcConfig;
import revi1337.onsquad.member.domain.vo.UserType;

@Import({WebMvcConfig.class, PresentationLayerConfiguration.class})
@WebMvcTest
@ExtendWith({RestDocumentationExtension.class})
public abstract class PresentationLayerTestSupport {

    protected static final String AUTHORIZATION_HEADER_KEY = HttpHeaders.AUTHORIZATION;
    protected static final String AUTHORIZATION_HEADER_VALUE = "Bearer header.payload.signature";
    protected static final String ACCESS_TOKEN = "header.access-token-payload.signature";
    protected static final String REFRESH_TOKEN = "header.refresh-token-payload.signature";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected JsonWebTokenManager jsonWebTokenManager;

    @BeforeEach
    protected void setUp(
            final WebApplicationContext applicationContext,
            final RestDocumentationContextProvider provider
    ) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(MockMvcRestDocumentation.documentationConfiguration(provider))
                .alwaysDo(MockMvcResultHandlers.print())
                .addFilter(new CharacterEncodingFilter("UTF-8", true))
                .build();

        ClaimsParser claimsParser = mock(ClaimsParser.class);
        when(claimsParser.parseIdentity()).thenReturn(1L);
        when(claimsParser.parseUserType()).thenReturn(UserType.GENERAL);
        when(jsonWebTokenManager.verifyAccessToken(any())).thenReturn(claimsParser);
    }
}
