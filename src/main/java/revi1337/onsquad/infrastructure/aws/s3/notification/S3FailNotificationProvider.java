package revi1337.onsquad.infrastructure.aws.s3.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import revi1337.onsquad.common.config.system.properties.OnsquadProperties;
import revi1337.onsquad.common.util.ObjectMapperUtils;
import revi1337.onsquad.infrastructure.aws.s3.cleanup.S3CleanupProcessor;
import revi1337.onsquad.infrastructure.network.discord.DiscordMessage;
import revi1337.onsquad.infrastructure.network.discord.DiscordMessage.Embed;
import revi1337.onsquad.infrastructure.network.discord.DiscordMessage.Embed.Footer;
import revi1337.onsquad.infrastructure.network.discord.DiscordMessage.Embed.Thumbnail;
import revi1337.onsquad.infrastructure.network.discord.DiscordNotificationClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3FailNotificationProvider {

    private static final String NOTIFICATION_PROVIDER_NAME = "Amazon Web Service";
    private static final String NOTIFICATION_AVATAR_URL = "https://moneyneversleeps.co.kr/news/photo/202503/104407_14308_4535.png";
    private static final String S3_SERVICE_NAME = "S3 Lifecycle Management Service";
    private static final String S3_ICON_URL = "https://www.dmuth.org/wp-content/uploads/2019/09/aws-s3-icon.png";

    private final DiscordNotificationClient s3DiscordNotificationClient;
    private final ObjectMapper defaultObjectMapper;
    private final OnsquadProperties onsquadProperties;

    public void sendExceedRetryAlert(List<String> paths) {
        DiscordMessage message = createDiscordMessage(paths);
        try {
            byte[] fileBytes = ObjectMapperUtils.serializeToBytes(defaultObjectMapper, new RetryExceedJson(paths));
            s3DiscordNotificationClient.sendNotification(message, "exceed_file_paths.json", fileBytes);
        } catch (RuntimeException e) {
            log.error("Failed to serialize S3 exceed retry paths. Object count: {}, Error: {}", paths.size(), e.getMessage(), e);
            s3DiscordNotificationClient.sendNotification(message);
        }
    }

    private DiscordMessage createDiscordMessage(List<String> paths) {
        String content = MessageFormat.format("""
                ⚠️ **S3 Batch Purge: Max Retries Exceeded**
                
                **Cause:** Objects failed to be deleted after `{0}` batch attempts.
                **Manual deletion is required.** Please refer to the attached JSON file for the specific file paths.
                
                **S3 Batch Cleanup: Exceed Report:** `{1} Objects Exceeded`
                """, S3CleanupProcessor.MAX_RETRY_COUNT, paths.size()).translateEscapes();

        return DiscordMessage.builder()
                .username(NOTIFICATION_PROVIDER_NAME)
                .avatarUrl(NOTIFICATION_AVATAR_URL)
                .threadName(String.format("[%s] S3 Cleanup Report (Instance: %s)", LocalDate.now(), onsquadProperties.getIdentifier()))
                .content(content)
                .embeds(buildEmbeds())
                .build();
    }

    private List<Embed> buildEmbeds() {
        String title = "S3 Deletion Process: Max Retries Reached";
        String description = MessageFormat.format("""
                Files scheduled for deletion are processed in a daily midnight batch.
                Any object that fails the deletion process **{0} consecutive times** is classified as 'Exceed' and excluded from the automated system.
                """, S3CleanupProcessor.MAX_RETRY_COUNT).translateEscapes();

        return List.of(Embed.builder()
                .color(Embed.COLOR_RED)
                .title(title)
                .description(description)
                .thumbnail(new Thumbnail(S3_ICON_URL))
                .footer(new Footer(S3_SERVICE_NAME, S3_ICON_URL))
                .build());
    }

    public record RetryExceedJson(List<String> exceedPaths) {

    }
}
