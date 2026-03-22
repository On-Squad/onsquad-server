package revi1337.onsquad.common.presentation.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.data.domain.Sort.Direction;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AdaptivePageable {

    int page() default 0;

    int size() default 10;

    boolean allowWebSort() default false;

    String[] defaultSort() default {};

    Direction direction() default Direction.DESC;

}
