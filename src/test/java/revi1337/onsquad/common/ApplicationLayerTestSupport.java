package revi1337.onsquad.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import revi1337.onsquad.common.aspect.ThrottlingAspect;
import revi1337.onsquad.common.config.ApplicationLayerConfiguration;
import revi1337.onsquad.infrastructure.aws.cloudfront.CloudFrontCacheInvalidator;
import revi1337.onsquad.infrastructure.storage.redis.RedisCacheAspect;

@Sql({"/h2-truncate.sql"})
@Import({ApplicationLayerConfiguration.class})
@RecordApplicationEvents
@Transactional
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public abstract class ApplicationLayerTestSupport {

    @MockBean
    protected ThrottlingAspect throttlingAspect;

    @MockBean
    protected RedisCacheAspect redisCacheAspect;

    @MockBean
    protected CloudFrontCacheInvalidator cloudFrontCacheInvalidator;

    @PersistenceContext
    protected EntityManager entityManager;

    protected void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
