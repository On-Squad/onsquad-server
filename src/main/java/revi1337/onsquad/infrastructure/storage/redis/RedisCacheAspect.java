package revi1337.onsquad.infrastructure.storage.redis;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import revi1337.onsquad.common.constant.CacheConst.CacheFormat;
import revi1337.onsquad.common.util.ObjectMapperUtils;

@Order(Ordered.LOWEST_PRECEDENCE - 1)
@Aspect
@Component
@RequiredArgsConstructor
public class RedisCacheAspect {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

    @Around("@annotation(redisCache)")
    public Object handleRedisCache(ProceedingJoinPoint joinPoint, RedisCache redisCache) {
        String redisKey = generateRedisKey(joinPoint, redisCache);
        String cachedData = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedData != null) {
            return deserializeCachedData(cachedData, joinPoint);
        }
        try {
            Object result = joinPoint.proceed();
            if (shouldCache(redisCache, result)) {
                stringRedisTemplate.opsForValue().set(redisKey, serializeData(result), calculateTtl(redisCache));
            }
            return result;
        } catch (Throwable e) {
            throw new RuntimeException("Error executing method " + joinPoint.getSignature().getName(), e);
        }
    }

    private String generateRedisKey(ProceedingJoinPoint joinPoint, RedisCache redisCache) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        String expressionValue = spelExpressionParser.parseExpression(redisCache.key()).getValue(context, String.class);
        return String.format(CacheFormat.COMPLEX, redisCache.name(), expressionValue);
    }

    private String serializeData(Object data) {
        return ObjectMapperUtils.serializeToString(objectMapper, data);
    }

    private Object deserializeCachedData(String cachedData, ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        JavaType javaType = ObjectMapperUtils.constructGenericType(objectMapper, methodSignature.getMethod());

        return ObjectMapperUtils.deserialize(objectMapper, cachedData, javaType);
    }

    private boolean shouldCache(RedisCache redisCache, Object result) {
        if (result == null) {
            return false;
        }
        if (result instanceof Collection<?> collection) {
            return !collection.isEmpty() || redisCache.cacheEmptyCollection();
        }
        return true;
    }

    private Duration calculateTtl(RedisCache redisCache) {
        return Duration.of(redisCache.ttl(), redisCache.unit().toChronoUnit());
    }
}
