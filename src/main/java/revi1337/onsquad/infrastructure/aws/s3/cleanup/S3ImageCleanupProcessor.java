package revi1337.onsquad.infrastructure.aws.s3.cleanup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import revi1337.onsquad.infrastructure.aws.s3.cleanup.model.FilePath;
import revi1337.onsquad.infrastructure.aws.s3.cleanup.model.FilePaths;
import revi1337.onsquad.infrastructure.aws.s3.client.S3StorageCleaner;
import revi1337.onsquad.infrastructure.aws.s3.client.S3StorageCleaner.DeletedResult;
import revi1337.onsquad.infrastructure.storage.sqlite.DeletedImage;
import revi1337.onsquad.infrastructure.storage.sqlite.ImageRecycleBinRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageCleanupProcessor {

    public static final int MAX_RETRY_COUNT = 5;
    public static final int BATCH_SIZE = 1000;

    private final Executor s3DeletionExecutor;
    private final ImageRecycleBinRepository imageRecyclebinRepository;
    private final S3StorageCleaner s3StorageCleaner;

    public FilePaths findTargets(long lastId, LocalDateTime cutOff, int batchSize) {
        List<FilePath> targets = imageRecyclebinRepository.findByCursor(lastId, cutOff, batchSize).stream()
                .map(this::convert)
                .toList();

        return new FilePaths(targets);
    }

    public CleanupResult executeS3Deletion(FilePaths targets) {
        List<CompletableFuture<DeletedResult>> futures = targets.partition(BATCH_SIZE).stream()
                .map(FilePaths::pathValues)
                .map(this::deleteBatchAsync)
                .toList();

        List<DeletedResult> results = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList()).join();

        return CleanupResult.of(targets, results);
    }

    public FilePaths updateRetryCountAndGetExceeded(FilePaths failedPaths) {
        imageRecyclebinRepository.incrementRetryCount(failedPaths.getFileIds());

        List<FilePath> exceedPaths = failedPaths.stream()
                .filter(failedPath -> failedPath.getRetryCount() + 1 >= MAX_RETRY_COUNT)
                .toList();

        return new FilePaths(exceedPaths);
    }

    public void deleteFromRecycleBin(FilePaths paths) {
        imageRecyclebinRepository.deleteByIdIn(paths.getFileIds());
    }

    private CompletableFuture<DeletedResult> deleteBatchAsync(List<String> pathValues) {
        return CompletableFuture.supplyAsync(() -> s3StorageCleaner.deleteInBatch(pathValues), s3DeletionExecutor)
                .exceptionally(throwable -> {
                    log.error("S3 Batch deletion failed - Total: {}", pathValues.size(), throwable);
                    return new DeletedResult(List.of(), pathValues);
                });
    }

    private FilePath convert(DeletedImage deletedImage) {
        return new FilePath(deletedImage.getId(), deletedImage.getPath(), deletedImage.getRetryCount(), deletedImage.getDeletedAt());
    }

    public record CleanupResult(FilePaths success, FilePaths failure) {

        public static CleanupResult of(FilePaths targets, List<DeletedResult> results) {
            List<String> successPaths = results.stream().flatMap(r -> r.deletedPaths().stream()).toList();
            List<String> failedPaths = results.stream().flatMap(r -> r.failedPaths().stream()).toList();

            return new CleanupResult(targets.filterByPaths(successPaths), targets.filterByPaths(failedPaths));
        }
    }
}
