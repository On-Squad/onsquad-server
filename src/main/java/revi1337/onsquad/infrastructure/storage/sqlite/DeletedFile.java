package revi1337.onsquad.infrastructure.storage.sqlite;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class DeletedFile {

    private Long id;
    private String path;
    private int retryCount;
    private LocalDateTime deletedAt;

    public DeletedFile(String path, LocalDateTime deletedAt) {
        this(null, path, 0, deletedAt);
    }

    public DeletedFile(String path, int retryCount, LocalDateTime deletedAt) {
        this(null, path, retryCount, deletedAt);
    }

    public DeletedFile(Long id, String path, int retryCount, LocalDateTime deletedAt) {
        this.id = id;
        this.path = path;
        this.retryCount = retryCount;
        this.deletedAt = deletedAt;
    }
}
