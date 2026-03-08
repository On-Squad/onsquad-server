package revi1337.onsquad.infrastructure.aws.s3.event;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;
import revi1337.onsquad.common.util.UrlUtils;
import revi1337.onsquad.infrastructure.aws.cloudfront.CloudFrontProperties;
import revi1337.onsquad.infrastructure.storage.sqlite.DeletedImage;
import revi1337.onsquad.infrastructure.storage.sqlite.ImageRecycleBinRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileDeleteEventListener {

    private final ImageRecycleBinRepository imageRecyclebinRepository;
    private final CloudFrontProperties cloudFrontProperties;

    @Async("fileDeletionRecorder")
    @TransactionalEventListener(value = FileDeleteEvent.class, fallbackExecution = true)
    public void recordFileDeletion(FileDeleteEvent event) {
        if (CollectionUtils.isEmpty(event.getFileUrls())) {
            return;
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        List<DeletedImage> deletedImages = event.getFileUrls().stream()
                .map(this::extractPath)
                .map(filePath -> new DeletedImage(filePath, deletedAt))
                .toList();

        imageRecyclebinRepository.insertBatch(deletedImages);
        log.debug("{} file paths have been stored in SQLite for batch deletion", deletedImages.size());
    }

    private String extractPath(String imageUrl) {
        return UrlUtils.stripPrefixAndLeadingSlash(cloudFrontProperties.baseDomain(), imageUrl);
    }
}
