package revi1337.onsquad.auth.token.infrastructure.persistence.initializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import revi1337.onsquad.auth.token.domain.model.RefreshToken;
import revi1337.onsquad.auth.token.domain.model.RefreshTokens;
import revi1337.onsquad.auth.token.infrastructure.persistence.ExpiringMapRefreshTokenStorage;
import revi1337.onsquad.common.util.ObjectMapperUtils;

@Slf4j
@Profile({"default", "local"})
@Component
public class RefreshTokenReloader {

    private static final String RESTORE_ERROR_LOG = "Error Occur While Restoring Refresh Tokens";
    private static final String RESTORE_LOG_FORMAT = "Restored Refresh Token - path : {}";
    private static final String WRITING_ERROR_LOG = "Error Occur While Writing Refresh Tokens";
    private static final String WRITING_LOG = "Backup Refresh Tokens";

    private final String backupPath;
    private final ExpiringMapRefreshTokenStorage refreshTokenStorage;
    private final ObjectMapper objectMapper;

    public RefreshTokenReloader(
            @Value("${onsquad.token.refresh-token.backup-path}") String backupPath,
            ExpiringMapRefreshTokenStorage refreshTokenStorage,
            ObjectMapper objectMapper
    ) {
        this.backupPath = backupPath;
        this.refreshTokenStorage = refreshTokenStorage;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void restore() {
        Optional<File> optionalFile = getBackupFileIfExists();
        Instant now = Instant.now();

        optionalFile.ifPresent(backupFile -> restoreRefreshTokens(backupFile, now));
    }

    @EventListener(ContextClosedEvent.class)
    public void backup(ContextClosedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            RefreshTokens refreshTokens = refreshTokenStorage.getTokens();
            if (refreshTokens.isEmpty()) {
                return;
            }
            File backupFile = ensureParentDirectoryExists(Paths.get(backupPath).toFile());
            backupRefreshTokens(backupFile, refreshTokens);
        }
    }

    private void restoreRefreshTokens(File backupFile, Instant now) {
        try {
            RefreshTokens refreshTokens = ObjectMapperUtils.deserializeFromFile(objectMapper, backupFile, RefreshTokens.class);
            List<RefreshToken> availableRefreshTokens = refreshTokens.extractAvailableBefore(now);

            restoreInternal(availableRefreshTokens, now);
        } catch (RuntimeException exception) {
            log.error(RESTORE_ERROR_LOG, exception);
        }
    }

    private void restoreInternal(List<RefreshToken> refreshTokens, Instant now) {
        for (RefreshToken refreshToken : refreshTokens) {
            Duration leftDuration = Duration.between(now, refreshToken.expiredAt().toInstant());
            if (leftDuration.isNegative() || leftDuration.isZero()) {
                continue;
            }
            refreshTokenStorage.saveToken(refreshToken.identifier(), refreshToken, leftDuration);
        }
        log.info(RESTORE_LOG_FORMAT, backupPath);
    }

    private void backupRefreshTokens(File backupFile, RefreshTokens refreshTokens) {
        try {
            ObjectMapperUtils.serializeToFileAsPretty(objectMapper, backupFile, refreshTokens);
            log.info(WRITING_LOG);
        } catch (RuntimeException e) {
            log.error(WRITING_ERROR_LOG, e);
        }
    }

    private Optional<File> getBackupFileIfExists() {
        File file = Paths.get(backupPath).toFile();
        if (file.exists()) {
            return Optional.of(file);
        }

        return Optional.empty();
    }

    private File ensureParentDirectoryExists(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (parentDir.mkdirs()) {
                log.debug("Created backup directory: {}", parentDir.getAbsolutePath());
            }
        }

        return file;
    }
}
