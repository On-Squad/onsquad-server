package revi1337.onsquad.infrastructure.support.request;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import revi1337.onsquad.common.aspect.RequestCacheHandler;
import revi1337.onsquad.common.aspect.RequestCacheHandlerChain;

/**
 * RedisRequestCacheHandler For RequestCacheHandlerExecutionChain
 *
 * @see RequestCacheHandlerChain
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class RedisRequestCacheHandler implements RequestCacheHandler {

    public static final String EXCEPTION_LOG_FORMAT = "[Exception 발생: {} - Message: {}] ";

    private final StringRedisTemplate fastStringRedisTemplate;

    @Override
    public Boolean isFirstRequest(String key, String value, long timeout, TimeUnit unit) {
        try {
            var valueOperations = fastStringRedisTemplate.opsForValue();
            return valueOperations.setIfAbsent(key, value, timeout, unit);
        } catch (RuntimeException exception) {
            log.error(EXCEPTION_LOG_FORMAT, exception.getClass().getSimpleName(), exception.getMessage());
            throw exception;
        }
    }
}
