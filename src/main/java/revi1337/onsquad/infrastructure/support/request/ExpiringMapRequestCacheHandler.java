package revi1337.onsquad.infrastructure.support.request;

import java.util.concurrent.TimeUnit;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import revi1337.onsquad.common.aspect.RequestCacheHandler;
import revi1337.onsquad.common.aspect.RequestCacheHandlerChain;

/**
 * DefaultRequestCacheHandler For RequestCacheHandlerExecutionChain
 *
 * @see RequestCacheHandlerChain
 */
@Order(2)
@Component
public final class ExpiringMapRequestCacheHandler implements RequestCacheHandler {

    private static final int MAX_CACHE_SIZE = 10_000;
    private static final ExpiringMap<String, String> REQUEST_CACHE = ExpiringMap.builder()
            .maxSize(MAX_CACHE_SIZE)
            .variableExpiration()
            .build();

    @Override
    public Boolean isFirstRequest(String key, String value, long timeout, TimeUnit unit) {
        String cacheValue = REQUEST_CACHE.get(key);
        if (cacheValue == null) {
            REQUEST_CACHE.put(key, value, ExpirationPolicy.CREATED, timeout, unit);
            return true;
        }

        return false;
    }
}
