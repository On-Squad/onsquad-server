package revi1337.onsquad.infrastructure.aws.s3.cleanup;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import revi1337.onsquad.infrastructure.aws.s3.cleanup.S3CleanupProcessor.CleanupResult;
import revi1337.onsquad.infrastructure.aws.s3.cleanup.model.FileObjects;
import revi1337.onsquad.infrastructure.aws.s3.notification.S3FailNotificationProvider;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3CleanupOrchestrator {

    private static final int BATCH_FETCH_SIZE = 5000;

    private final S3CleanupProcessor cleanupProcessor;
    private final S3FailNotificationProvider notificationProvider;

    public void execute(LocalDateTime startAt) {
        long lastId = 0L;
        log.info("Starting S3 Cleanup Task - CutOff: {}", startAt);

        while (true) {
            FileObjects targets = cleanupProcessor.findTargets(lastId, startAt, BATCH_FETCH_SIZE);
            if (targets.isEmpty()) {
                break;
            }
            log.debug("Processing S3 Cleanup Batch - LastID: {}, Size: {}", lastId, targets.size());
            CleanupResult result = cleanupProcessor.executeS3Deletion(targets);
            handleSuccess(result.success());
            handleFailure(result.failure());
            lastId = targets.getLastFileId();
        }
    }

    private void handleSuccess(FileObjects success) {
        if (success.isNotEmpty()) {
            log.debug("Success Cleanup S3 Objects - Total: {}", success.size());
            cleanupProcessor.deleteFromRecycleBin(success);
        }
    }

    private void handleFailure(FileObjects failure) {
        if (failure.isNotEmpty()) {
            log.debug("Fail to Cleanup S3 Objects - Total: {}", failure.size());
            FileObjects exceedPaths = cleanupProcessor.updateRetryCountAndGetExceeded(failure);
            if (exceedPaths.isEmpty()) {
                return;
            }
            log.warn("S3 Objects Exceeded Max Retries - Notifying Total: {}", exceedPaths.size());
            notificationProvider.sendExceedRetryAlert(exceedPaths.pathValues());
            cleanupProcessor.deleteFromRecycleBin(exceedPaths);
        }
    }
}
