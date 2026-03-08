package revi1337.onsquad.infrastructure.aws.s3.cleanup.model;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class FilePath {

    private Long id;
    private String path;
    private int retryCount;
    private LocalDateTime deletedAt;

    public FilePath(Long id, String path, int retryCount, LocalDateTime deletedAt) {
        this.id = id;
        this.path = path;
        this.retryCount = retryCount;
        this.deletedAt = deletedAt;
    }
}
