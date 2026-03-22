package revi1337.onsquad.common.config.web;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import revi1337.onsquad.auth.support.AuthenticateArgumentResolver;
import revi1337.onsquad.auth.token.application.JsonWebTokenManager;
import revi1337.onsquad.category.presentation.converter.CategoryTypeConverter;
import revi1337.onsquad.common.presentation.support.AdaptivePageableHandlerMethodArgumentResolver;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JsonWebTokenManager jsonWebTokenManager;
    private final SortHandlerMethodArgumentResolver sortResolver;
    private final Optional<PageableHandlerMethodArgumentResolverCustomizer> pagingCustomizer;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(getAuthenticateArgumentResolver());
        resolvers.add(getPagingArgumentResolver());
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new CategoryTypeConverter());
    }

    private AuthenticateArgumentResolver getAuthenticateArgumentResolver() {
        return new AuthenticateArgumentResolver(jsonWebTokenManager);
    }

    private AdaptivePageableHandlerMethodArgumentResolver getPagingArgumentResolver() {
        AdaptivePageableHandlerMethodArgumentResolver pagingArgumentResolver = new AdaptivePageableHandlerMethodArgumentResolver(sortResolver);
        pagingCustomizer.ifPresent(customizer -> customizer.customize(pagingArgumentResolver));
        return pagingArgumentResolver;
    }
}
