package revi1337.onsquad.infrastructure.storage.sqlite;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class DeletedImage {

    private Long id;
    private String path;
    private int retryCount;
    private LocalDateTime deletedAt;

    public DeletedImage(String path, LocalDateTime deletedAt) {
        this(null, path, 0, deletedAt);
    }

    public DeletedImage(String path, int retryCount, LocalDateTime deletedAt) {
        this(null, path, retryCount, deletedAt);
    }

    public DeletedImage(Long id, String path, int retryCount, LocalDateTime deletedAt) {
        this.id = id;
        this.path = path;
        this.retryCount = retryCount;
        this.deletedAt = deletedAt;
    }
}
