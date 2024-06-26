package revi1337.onsquad.common.config;

import com.blazebit.persistence.querydsl.JPQLNextOps;
import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.sql.SQLOps;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaQueryFactoryConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
//        return new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager);
        return new JPAQueryFactory(new CustomJPQLTemplates(), entityManager);
    }

    static class CustomJPQLTemplates extends JPQLTemplates {

        public CustomJPQLTemplates() {
            add(SQLOps.ROWNUMBER, "row_number()");
        }

//        public CustomJPQLTemplates() {
//            add(JPQLNextOps.ROW_NUMBER, "row_number()");
//            add(JPQLNextOps.WINDOW_ORDER_BY, "ORDER BY {0}");
//            add(JPQLNextOps.WINDOW_PARTITION_BY, "PARTITION BY {0}");
//            add(JPQLNextOps.WINDOW_DEFINITION_1, "{0}");
//            add(JPQLNextOps.WINDOW_DEFINITION_2, "{0} {1}");
//            add(JPQLNextOps.WINDOW_DEFINITION_3, "{0} {1} {2}");
//            add(JPQLNextOps.WINDOW_DEFINITION_4, "{0} {1} {2} {3}");
//        }
    }

}
