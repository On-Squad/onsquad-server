package revi1337.onsquad.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import revi1337.onsquad.auth.config.AuthenticateArgumentResolver;
import revi1337.onsquad.auth.application.JsonWebTokenEvaluator;

import java.util.List;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JsonWebTokenEvaluator jsonWebTokenEvaluator;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthenticateArgumentResolver(jsonWebTokenEvaluator));
    }
}
