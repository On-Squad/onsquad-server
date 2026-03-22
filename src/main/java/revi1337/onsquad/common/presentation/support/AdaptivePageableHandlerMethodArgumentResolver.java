package revi1337.onsquad.common.presentation.support;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public class AdaptivePageableHandlerMethodArgumentResolver extends PageableHandlerMethodArgumentResolver {

    private final SortHandlerMethodArgumentResolver sortResolver;

    public AdaptivePageableHandlerMethodArgumentResolver(SortHandlerMethodArgumentResolver sortResolver) {
        super(sortResolver);
        this.sortResolver = sortResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AdaptivePageable.class);
    }

    @Override
    public Pageable resolveArgument(MethodParameter methodParameter, @Nullable ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
        AdaptivePageable annotation = methodParameter.getParameterAnnotation(AdaptivePageable.class);
        if (annotation == null) {
            return super.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
        }
        String pageStr = getPageParameterValue(methodParameter, webRequest, annotation);
        String sizeStr = getSizeParameterValue(methodParameter, webRequest, annotation);
        Pageable pageable = getPageable(methodParameter, pageStr, sizeStr);
        Sort sort = resolveSortable(methodParameter, mavContainer, webRequest, binderFactory, annotation);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private String getPageParameterValue(MethodParameter methodParameter, NativeWebRequest webRequest, AdaptivePageable annotation) {
        String pageStr = webRequest.getParameter(getParameterNameToUse(getPageParameterName(), methodParameter));
        if (pageStr == null) {
            pageStr = String.valueOf(annotation.page());
        }
        return pageStr;
    }

    private String getSizeParameterValue(MethodParameter methodParameter, NativeWebRequest webRequest, AdaptivePageable annotation) {
        String sizeStr = webRequest.getParameter(getParameterNameToUse(getSizeParameterName(), methodParameter));
        if (sizeStr == null) {
            sizeStr = String.valueOf(annotation.size());
        }
        return sizeStr;
    }

    private Sort resolveSortable(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
                                 NativeWebRequest webRequest, WebDataBinderFactory binderFactory, AdaptivePageable annotation) {
        if (annotation.allowWebSort()) {
            return sortResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
        }
        if (annotation.defaultSort().length > 0) {
            return Sort.by(annotation.direction(), annotation.defaultSort());
        }
        return Sort.unsorted();
    }
}
