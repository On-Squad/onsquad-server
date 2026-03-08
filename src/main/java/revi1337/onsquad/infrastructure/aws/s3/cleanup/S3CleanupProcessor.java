package revi1337.onsquad.infrastructure.aws.s3.cleanup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import revi1337.onsquad.infrastructure.aws.s3.cleanup.model.FileObject;
import revi1337.onsquad.infrastructure.aws.s3.cleanup.model.FileObjects;
import revi1337.onsquad.infrastructure.aws.s3.client.S3StorageCleaner;
import revi1337.onsquad.infrastructure.aws.s3.client.S3StorageCleaner.DeletedResult;
import revi1337.onsquad.infrastructure.storage.sqlite.DeletedFile;
import revi1337.onsquad.infrastructure.storage.sqlite.FileRecycleBinRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3CleanupProcessor {

    public static final int MAX_RETRY_COUNT = 5;
    public static final int BATCH_SIZE = 1000;

    private final Executor s3DeletionExecutor;
    private final FileRecycleBinRepository fileRecyclebinRepository;
    private final S3StorageCleaner s3StorageCleaner;

    public FileObjects findTargets(long lastId, LocalDateTime cutOff, int batchSize) {
        List<FileObject> targets = fileRecyclebinRepository.findByCursor(lastId, cutOff, batchSize).stream()
                .map(this::convert)
                .toList();

        return new FileObjects(targets);
    }

    public CleanupResult executeS3Deletion(FileObjects targets) {
        List<CompletableFuture<DeletedResult>> futures = targets.partition(BATCH_SIZE).stream()
                .map(FileObjects::pathValues)
                .map(this::deleteBatchAsync)
                .toList();

        List<DeletedResult> results = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList()).join();

        return CleanupResult.of(targets, results);
    }

    public FileObjects updateRetryCountAndGetExceeded(FileObjects failedPaths) {
        fileRecyclebinRepository.incrementRetryCount(failedPaths.getFileIds());

        List<FileObject> exceedPaths = failedPaths.stream()
                .filter(failedPath -> failedPath.getRetryCount() + 1 >= MAX_RETRY_COUNT)
                .toList();

        return new FileObjects(exceedPaths);
    }

    public void deleteFromRecycleBin(FileObjects paths) {
        fileRecyclebinRepository.deleteByIdIn(paths.getFileIds());
    }

    private CompletableFuture<DeletedResult> deleteBatchAsync(List<String> pathValues) {
        return CompletableFuture.supplyAsync(() -> s3StorageCleaner.deleteInBatch(pathValues), s3DeletionExecutor)
                .exceptionally(throwable -> {
                    log.error("S3 Batch deletion failed - Total: {}", pathValues.size(), throwable);
                    return new DeletedResult(List.of(), pathValues);
                });
    }

    private FileObject convert(DeletedFile deletedFile) {
        return new FileObject(deletedFile.getId(), deletedFile.getPath(), deletedFile.getRetryCount(), deletedFile.getDeletedAt());
    }

    public record CleanupResult(FileObjects success, FileObjects failure) {

        public static CleanupResult of(FileObjects targets, List<DeletedResult> results) {
            List<String> successPaths = results.stream().flatMap(r -> r.deletedPaths().stream()).toList();
            List<String> failedPaths = results.stream().flatMap(r -> r.failedPaths().stream()).toList();

            return new CleanupResult(targets.filterByPaths(successPaths), targets.filterByPaths(failedPaths));
        }
    }
}
