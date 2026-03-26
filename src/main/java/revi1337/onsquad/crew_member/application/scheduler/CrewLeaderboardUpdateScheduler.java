package revi1337.onsquad.crew_member.application.scheduler;

import io.lettuce.core.RedisException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardKeyMapper;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardManager;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardSnapshotManager;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardUpdateService;
import revi1337.onsquad.crew_member.domain.model.CrewLeaderboards;
import revi1337.onsquad.crew_member.infrastructure.discord.LeaderboardRefreshFailNotificationProvider;
import revi1337.onsquad.infrastructure.storage.redis.RedisLockExecutor;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrewLeaderboardUpdateScheduler {

    private static final String LOCK_KEY = "leaderboard-sch-lock";

    private final RedisLockExecutor redisLockExecutor;
    private final CrewLeaderboardSnapshotManager leaderboardSnapshotManager;
    private final CrewLeaderboardUpdateService leaderboardUpdateService;
    private final LeaderboardRefreshFailNotificationProvider notificationProvider;

    @Scheduled(cron = "${onsquad.api.crew-leaderboard.schedule.expression}")
    public void updateLeaderboards() {
        redisLockExecutor.executeIfAcquired(LOCK_KEY, Duration.ofMinutes(5), () -> {
            log.info("[Leaderboard-Scheduler] Job initiated. Fetching candidates from Redis...");
            List<String> snapshotKeys = captureSnapshotsViaRename();
            if (snapshotKeys.isEmpty()) {
                return;
            }
            processSnapshots(snapshotKeys);
        });
    }

    private List<String> captureSnapshotsViaRename() {
        try {
            return leaderboardSnapshotManager.captureSnapshots();
        } catch (RedisException exception) {
            log.error("[Leaderboard-Scheduler] Critical: Snapshot capture failed. Job aborted to prevent data corruption.", exception);
            notificationProvider.sendSnapshotCaptureFailAlert(CrewLeaderboardKeyMapper.getLeaderboardPattern());
            return Collections.emptyList();
        }
    }

    private void processSnapshots(List<String> snapshotKeys) {
        List<Long> relatedCrewIds = CrewLeaderboardKeyMapper.parseCrewIdFromKeys(snapshotKeys);
        try {
            CrewLeaderboards snapshots = leaderboardSnapshotManager.getSnapshots(relatedCrewIds, CrewLeaderboardManager.RANKING_OVER_FETCH_SIZE);
            leaderboardUpdateService.updateLeaderboards(snapshots);
            leaderboardSnapshotManager.clearSnapshots(snapshotKeys);
            log.info("[Leaderboard-Scheduler] Job completed successfully. Snapshot cleared.");
        } catch (Exception e) {
            log.error("[Leaderboard-Scheduler] Job failed during RDB update. Target Crew IDs: {}. Snapshots are preserved.", relatedCrewIds, e);
            notificationProvider.sendLeaderboardUpdateFailAlert(snapshotKeys);
        }
    }
}
