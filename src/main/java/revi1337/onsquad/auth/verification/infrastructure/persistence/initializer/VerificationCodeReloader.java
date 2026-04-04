package revi1337.onsquad.auth.verification.infrastructure.persistence.initializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import revi1337.onsquad.auth.verification.domain.VerificationCode;
import revi1337.onsquad.auth.verification.domain.VerificationCodes;
import revi1337.onsquad.auth.verification.infrastructure.persistence.ExpiringMapVerificationCodeStorage;
import revi1337.onsquad.common.util.ObjectMapperUtils;

@Slf4j
@Profile({"default", "local"})
@Component
public class VerificationCodeReloader {

    private static final String RESTORE_ERROR_LOG = "Error Occur While Restoring Verification Codes";
    private static final String RESTORE_LOG_FORMAT = "Restored Verification Code - path : {}";
    private static final String WRITING_ERROR_LOG = "Error Occur While Writing Verification Codes";
    private static final String WRITING_LOG = "Backup Verification Codes";

    private final String backupPath;
    private final ExpiringMapVerificationCodeStorage verificationCodeStorage;
    private final ObjectMapper objectMapper;

    public VerificationCodeReloader(
            @Value("${spring.mail.verification-code-backup-path}") String backupPath,
            ExpiringMapVerificationCodeStorage verificationCodeStorage,
            ObjectMapper objectMapper
    ) {
        this.backupPath = backupPath;
        this.verificationCodeStorage = verificationCodeStorage;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void restore() {
        Optional<File> optionalFile = getBackupFileIfExists();
        LocalDateTime now = LocalDateTime.now();

        optionalFile.ifPresent(backupFile -> restoreVerificationCodes(backupFile, now));
    }

    @EventListener(ContextClosedEvent.class)
    public void backup(ContextClosedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            Optional<File> optionalFile = getBackupFileIfExists();
            VerificationCodes verificationCodes = verificationCodeStorage.getVerificationCodes();

            optionalFile.ifPresent(backupFile -> backupVerificationCodes(backupFile, verificationCodes));
        }
    }

    private Optional<File> getBackupFileIfExists() {
        File file = Paths.get(backupPath).toFile();
        if (file.exists()) {
            return Optional.of(file);
        }

        return Optional.empty();
    }

    private void restoreVerificationCodes(File backupFile, LocalDateTime now) {
        try {
            VerificationCodes verificationCodes = ObjectMapperUtils.deserializeFromFile(objectMapper, backupFile, VerificationCodes.class);
            List<VerificationCode> availableVerificationCodes = verificationCodes.extractAvailableBefore(now);

            restoreInternal(availableVerificationCodes, now);
        } catch (RuntimeException e) {
            log.error(RESTORE_ERROR_LOG, e);
        }
    }

    private void backupVerificationCodes(File backupFile, VerificationCodes verificationCodes) {
        try {
            ObjectMapperUtils.serializeToFileAsPretty(objectMapper, backupFile, verificationCodes);
            log.info(WRITING_LOG);
        } catch (RuntimeException e) {
            log.error(WRITING_ERROR_LOG, e);
        }
    }

    private void restoreInternal(List<VerificationCode> verificationCodes, LocalDateTime now) {
        for (VerificationCode verificationCode : verificationCodes) {
            Duration leftDuration = Duration.between(now, verificationCode.getExpiredAt());
            if (leftDuration.isNegative() || leftDuration.isZero()) {
                continue;
            }
            verificationCodeStorage.saveVerificationCode(verificationCode.getEmail(), verificationCode.getCode(), verificationCode.getStatus(), leftDuration);
        }
        log.info(RESTORE_LOG_FORMAT, backupPath);
    }
}
