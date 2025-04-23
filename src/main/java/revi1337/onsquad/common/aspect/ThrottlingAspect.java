package revi1337.onsquad.common.aspect;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import revi1337.onsquad.common.constant.CacheConst.CacheFormat;
import revi1337.onsquad.common.constant.Sign;
import revi1337.onsquad.common.error.CommonErrorCode;
import revi1337.onsquad.common.error.exception.CommonBusinessException;

@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@Aspect
public class ThrottlingAspect {

    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
    private final RequestCacheHandler requestCacheHandler;

    @Before("@annotation(throttling)")
    public void checkInitialRequest(JoinPoint joinPoint, Throttling throttling) {
        String redisKey = generateRedisKey(joinPoint, throttling);
        boolean firstRequest = requestCacheHandler
                .isFirstRequest(redisKey, LocalDateTime.now().toString(), throttling.during(), throttling.unit());
        if (!firstRequest) {
            throw new CommonBusinessException.RequestConflict(
                    CommonErrorCode.REQUEST_CONFLICT, getCycleAsDuration(throttling)
            );
        }
    }

    private String generateRedisKey(JoinPoint joinPoint, Throttling throttling) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        String expressionValue = spelExpressionParser.parseExpression(throttling.key()).getValue(context, String.class);
        return String.format(CacheFormat.COMPLEX, throttling.name(), expressionValue);
    }

    private String getCycleAsDuration(Throttling throttling) {
        ChronoUnit chronoUnit = throttling.unit().toChronoUnit();
        return throttling.during() + Sign.WHITESPACE + chronoUnit;
    }
}
