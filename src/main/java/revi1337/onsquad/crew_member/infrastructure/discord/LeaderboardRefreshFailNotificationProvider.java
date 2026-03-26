package revi1337.onsquad.crew_member.infrastructure.discord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import revi1337.onsquad.common.config.system.properties.OnsquadProperties;
import revi1337.onsquad.infrastructure.network.discord.DiscordMessage;
import revi1337.onsquad.infrastructure.network.discord.DiscordMessage.Embed;
import revi1337.onsquad.infrastructure.network.discord.DiscordMessage.Embed.Footer;
import revi1337.onsquad.infrastructure.network.discord.DiscordMessage.Embed.Thumbnail;
import revi1337.onsquad.infrastructure.network.discord.DiscordNotificationClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardRefreshFailNotificationProvider {

    private static final String NOTIFICATION_PROVIDER_NAME = "OnSquad Crew Leaderboard Update Scheduler";
    private static final String NOTIFICATION_AVATAR_URL = "https://res.cloudinary.com/eightcruz/image/upload/c_lfill,h_120,w_120/perbhzmfdr5mecxo8w3y";
    private static final String SERVICE_NAME = "OnSquad Leaderboard Update Service";
    private static final String SERVICE_ICON_URL = "https://img.icons8.com/color/512/redis.png";

    private final DiscordNotificationClient leaderboardDiscordNotificationClient;
    private final ObjectMapper defaultObjectMapper;
    private final OnsquadProperties onsquadProperties;

    public void sendSnapshotCaptureFailAlert(String keyPattern) {
        DiscordMessage message = createCaptureFailDiscordMessage(keyPattern);
        leaderboardDiscordNotificationClient.sendNotification(message);
    }

    public void sendLeaderboardUpdateFailAlert(List<String> snapshotKeys) {
        DiscordMessage message = createDiscordMessage(snapshotKeys);
        try {
            byte[] fileBytes = defaultObjectMapper.writeValueAsBytes(new FailedSnapshotKeysJson(snapshotKeys));
            leaderboardDiscordNotificationClient.sendNotification(message, "failed_snapshot_keys.json", fileBytes);
        } catch (JsonProcessingException e) {
            log.error("[Leaderboard-Notification] Failed to serialize snapshot keys. Key count: {}, Error: {}", snapshotKeys.size(), e.getMessage(), e);
            leaderboardDiscordNotificationClient.sendNotification(message);
        }
    }

    private DiscordMessage createCaptureFailDiscordMessage(String pattern) {
        String content = MessageFormat.format("""
                ⚠️ **Leaderboard Update: Snapshot Capture Failed**
                
                **Cause:** Critical failure during Redis Lua script execution (Capture phase).
                **Status:** Potential data fragmentation or timeout. **Leaderboard RENAME might be incomplete.**
                
                **Target Key Pattern:** `{0}`
                **Action Required:** Check Redis for partial `:snapshot` keys and verify system load.
                **Environment Identifier:** `{1}`
                """, pattern, onsquadProperties.getIdentifier()).translateEscapes();

        return DiscordMessage.builder()
                .username(NOTIFICATION_PROVIDER_NAME)
                .avatarUrl(NOTIFICATION_AVATAR_URL)
                .threadName(String.format("[%s] Snapshot Capture Critical Failure (Instance: %s)", LocalDate.now(), onsquadProperties.getIdentifier()))
                .content(content)
                .embeds(buildCaptureFailEmbeds(pattern))
                .build();
    }

    private List<Embed> buildCaptureFailEmbeds(String pattern) {
        String title = "Critical: Redis Snapshot Process Aborted";
        String description = MessageFormat.format("""
                The system failed to rename current leaderboards to snapshots.
                **Some leaderboards might still be in the previous week's state.**
                
                Please use `SCAN 0 MATCH {0}*` in Redis to identify the current state.
                Check logs for `RedisException` or `TimeoutException`.
                """, pattern).translateEscapes();

        return List.of(Embed.builder()
                .color(Embed.COLOR_RED)
                .title(title)
                .description(description)
                .thumbnail(new Thumbnail(SERVICE_ICON_URL))
                .footer(new Footer(SERVICE_NAME, SERVICE_ICON_URL))
                .build());
    }

    private DiscordMessage createDiscordMessage(List<String> snapshotKeys) {
        String content = MessageFormat.format("""
                ⚠️ **Leaderboard Update: Process Failed**
                
                **Cause:** An unexpected error occurred during the scheduled leaderboard update.
                **Action Required:** Manual ranking synchronization or system status check (Redis/DB) is required.
                
                **Total Failed Snapshots:** `{0}` keys preserved in Redis.
                **Environment Identifier:** `{1}`
                """, snapshotKeys.size(), onsquadProperties.getIdentifier()).translateEscapes();

        return DiscordMessage.builder()
                .username(NOTIFICATION_PROVIDER_NAME)
                .avatarUrl(NOTIFICATION_AVATAR_URL)
                .threadName(String.format("[%s] Leaderboard Update Failure (Instance: %s)", LocalDate.now(), onsquadProperties.getIdentifier()))
                .content(content)
                .embeds(buildEmbeds(snapshotKeys.size()))
                .build();
    }

    private List<Embed> buildEmbeds(int failedCount) {
        String title = "Critical: Leaderboard Refresh Aborted";
        String description = MessageFormat.format("""
                The scheduled task failed to apply activity scores to the global leaderboard.
                To prevent data loss, the **{0}** snapshots remain in Redis.
                Please check the application logs for the full stack trace and manually trigger the refresh if necessary.
                """, failedCount).translateEscapes();

        return List.of(Embed.builder()
                .color(Embed.COLOR_RED)
                .title(title)
                .description(description)
                .thumbnail(new Thumbnail(SERVICE_ICON_URL))
                .footer(new Footer(SERVICE_NAME, SERVICE_ICON_URL))
                .build());
    }

    private record FailedSnapshotKeysJson(List<String> failedKeys) {

    }
}
