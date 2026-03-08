package revi1337.onsquad.infrastructure.aws.s3.cleanup;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import revi1337.onsquad.infrastructure.aws.s3.cleanup.S3ImageCleanupProcessor.CleanupResult;
import revi1337.onsquad.infrastructure.aws.s3.cleanup.model.FilePaths;
import revi1337.onsquad.infrastructure.aws.s3.notification.S3FailNotificationProvider;

@ExtendWith(MockitoExtension.class)
class S3CleanupOrchestratorTest {

    @Mock
    private S3ImageCleanupProcessor cleanupProcessor;

    @Mock
    private S3FailNotificationProvider notificationProvider;

    @InjectMocks
    private S3CleanupOrchestrator orchestrator;

    @Test
    @DisplayName("재시도 임계치를 초과한 실패 건에 대해서만 알림을 발송하고 DB에서 제거한다")
    void execute_withExceedingFailures() {
        // given
        LocalDateTime startAt = LocalDateTime.now();
        FilePaths firstTargets = mock(FilePaths.class);
        FilePaths emptyTargets = mock(FilePaths.class);
        FilePaths success = mock(FilePaths.class);
        FilePaths failure = mock(FilePaths.class);
        FilePaths exceedPaths = mock(FilePaths.class);

        given(firstTargets.isEmpty()).willReturn(false);
        given(firstTargets.size()).willReturn(10);
        given(firstTargets.getLastFileId()).willReturn(100L);
        given(emptyTargets.isEmpty()).willReturn(true);

        given(cleanupProcessor.findTargets(anyLong(), eq(startAt), anyInt()))
                .willReturn(firstTargets)
                .willReturn(emptyTargets);

        given(cleanupProcessor.executeS3Deletion(firstTargets))
                .willReturn(new CleanupResult(success, failure));

        given(success.isNotEmpty()).willReturn(true);
        given(failure.isNotEmpty()).willReturn(true);
        given(cleanupProcessor.updateRetryCountAndGetExceeded(failure)).willReturn(exceedPaths);

        given(exceedPaths.isEmpty()).willReturn(false);
        given(exceedPaths.pathValues()).willReturn(List.of("fail-path"));

        // when
        orchestrator.execute(startAt);

        // then
        verify(cleanupProcessor).deleteFromRecycleBin(success);
        verify(cleanupProcessor).updateRetryCountAndGetExceeded(failure);
        verify(notificationProvider).sendExceedRetryAlert(anyList());
        verify(cleanupProcessor).deleteFromRecycleBin(exceedPaths);
        verify(cleanupProcessor, times(2)).findTargets(anyLong(), eq(startAt), anyInt());
    }
}
