package revi1337.onsquad.crew_member.application.scheduler;

import java.time.Duration;
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
            List<String> snapshotKeys;
            try {
                snapshotKeys = leaderboardSnapshotManager.captureSnapshots();
            } catch (Exception e) {
                log.error("[Leaderboard-Scheduler] Snapshot capture failed. Job aborted.", e);
                return;
            }
            List<Long> relatedCrewIds = CrewLeaderboardKeyMapper.parseCrewIdFromKeys(snapshotKeys);
            try {
                CrewLeaderboards snapshots = leaderboardSnapshotManager.getSnapshots(relatedCrewIds, CrewLeaderboardManager.RANKING_OVER_FETCH_SIZE);
                leaderboardUpdateService.updateLeaderboards(snapshots);
                leaderboardSnapshotManager.clearSnapshots(snapshotKeys);
                log.info("[Leaderboard-Scheduler] Job completed. Leaderboard has been refreshed and Redis cleared.");
            } catch (Exception e) {
                log.error("[Leaderboard-Scheduler] Job failed. Target Crew IDs: {}. Snapshots are preserved in Redis.", relatedCrewIds, e);
                notificationProvider.sendLeaderboardUpdateFailAlert(snapshotKeys);
            }
        });
    }
}
