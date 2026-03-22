package revi1337.onsquad.common.aspect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RequestCacheHandlerChain implements RequestCacheHandler {

    private static final String NAVIGATE_NEXT_HANDLER_LOG_FORMAT = "[{} 에서 예외 발생, Cause : {}] 다음 RequestCacheHandler 를 적용합니다.";
    private static final String MESSAGE_NO_CACHE_APPLICABLE_MSG = "[모든 RequestCacheHandler 들을 사용할 수 없습니다.]";

    private final List<RequestCacheHandler> requestCacheHandlers;

    public RequestCacheHandlerChain() {
        this.requestCacheHandlers = new ArrayList<>();
    }

    @Autowired
    public RequestCacheHandlerChain(List<RequestCacheHandler> requestCacheHandlers) {
        this.requestCacheHandlers = requestCacheHandlers;
    }

    @Override
    public Boolean isFirstRequest(String key, String value, long timeout, TimeUnit unit) {
        for (RequestCacheHandler requestCacheHandler : requestCacheHandlers) {
            try {
                Boolean firstRequest = requestCacheHandler.isFirstRequest(key, value, timeout, unit);
                if (firstRequest != null) {
                    return firstRequest;
                }
            } catch (RuntimeException exception) {
                log.debug(NAVIGATE_NEXT_HANDLER_LOG_FORMAT, requestCacheHandler.getClass().getSimpleName(), exception.getCause().getClass().getSimpleName());
            }
        }
        throw new IllegalArgumentException(MESSAGE_NO_CACHE_APPLICABLE_MSG);
    }

    public void addBefore(RequestCacheHandler requestCacheHandler, Class<? extends RequestCacheHandler> clazz) {
        for (int i = 0; i < requestCacheHandlers.size(); i++) {
            RequestCacheHandler currentHandler = requestCacheHandlers.get(i);
            if (clazz.isAssignableFrom(currentHandler.getClass())) {
                requestCacheHandlers.add(i, requestCacheHandler);
                return;
            }
        }
        addFirst(requestCacheHandler);
    }

    public void addAfter(RequestCacheHandler requestCacheHandler, Class<? extends RequestCacheHandler> clazz) {
        for (int i = 0; i < requestCacheHandlers.size(); i++) {
            RequestCacheHandler currentHandler = requestCacheHandlers.get(i);
            if (clazz.isAssignableFrom(currentHandler.getClass())) {
                if (i + 1 < requestCacheHandlers.size()) {
                    requestCacheHandlers.add(i + 1, requestCacheHandler);
                } else {
                    addLast(requestCacheHandler);
                }
                return;
            }
        }
        addFirst(requestCacheHandler);
    }

    public void addFirst(RequestCacheHandler requestCacheHandler) {
        if (requestCacheHandlers.isEmpty()) {
            requestCacheHandlers.add(requestCacheHandler);
            return;
        }
        requestCacheHandlers.add(0, requestCacheHandler);
    }

    public void addLast(RequestCacheHandler requestCacheHandler) {
        requestCacheHandlers.add(requestCacheHandler);
    }

    public List<RequestCacheHandler> getRequestCacheHandlers() {
        return requestCacheHandlers.stream().toList();
    }
}
