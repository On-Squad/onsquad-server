package revi1337.onsquad.infrastructure.storage.redis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RedisCache {

    String name() default "";

    String key();

    long ttl() default 1;

    TimeUnit unit() default TimeUnit.MINUTES;

    boolean cacheEmptyCollection() default false;

}
