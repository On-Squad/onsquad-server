package revi1337.onsquad.infrastructure.aws.s3.cleanup;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import revi1337.onsquad.infrastructure.storage.redis.RedisLockExecutor;

@Component
@RequiredArgsConstructor
public class S3BatchDeletionScheduler {

    private static final String LOCK_KEY = "batch-del-sch-lock";

    private final RedisLockExecutor redisLockExecutor;
    private final S3CleanupOrchestrator s3CleanupOrchestrator;

    @Scheduled(cron = "${onsquad.aws.s3.delete-batch-cron}")
    public void deleteInBatch() {
        LocalDateTime startAt = LocalDate.now().atStartOfDay();
        redisLockExecutor.executeIfAcquired(LOCK_KEY, Duration.ofMinutes(3), () -> s3CleanupOrchestrator.execute(startAt));
    }
}
