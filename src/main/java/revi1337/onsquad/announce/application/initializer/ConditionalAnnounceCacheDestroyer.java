package revi1337.onsquad.announce.application.initializer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import revi1337.onsquad.common.config.ConditionalFactory.UseCustomRedisAspectOrUseRedisCacheManager;
import revi1337.onsquad.common.constant.CacheConst;
import revi1337.onsquad.common.constant.CacheConst.CacheFormat;
import revi1337.onsquad.common.constant.Sign;

@Profile({"local", "default"})
@Conditional(UseCustomRedisAspectOrUseRedisCacheManager.class)
@Configuration
@Slf4j
@RequiredArgsConstructor
public class ConditionalAnnounceCacheDestroyer {

    private final StringRedisTemplate redisTemplate;
    private static final String DESTROY_KEY_PATTERN =
            String.format(CacheFormat.SIMPLE, CacheConst.CREW_ANNOUNCE + Sign.ASTERISK);

    @EventListener(ContextClosedEvent.class)
    private void cleanAnnounceCaches() {
        log.info("[Try to Destroy Cached Crew Announces]");
        Cursor<byte[]> scanCursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(ScanOptions.scanOptions()
                        .match(DESTROY_KEY_PATTERN)
                        .build()
                );

        List<String> keys = new ArrayList<>();
        while (scanCursor.hasNext()) {
            keys.add(new String(scanCursor.next(), StandardCharsets.UTF_8));
        }

        if (!keys.isEmpty()) {
            log.debug("[Destroy Cached Crew Announces] keys : {}", keys);
            redisTemplate.delete(keys);
        }
    }
}
