package revi1337.onsquad.crew_member.infrastructure.discord;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import revi1337.onsquad.common.config.system.properties.OnsquadProperties;
import revi1337.onsquad.infrastructure.network.discord.DiscordMessage;
import revi1337.onsquad.infrastructure.network.discord.DiscordNotificationClient;

@ExtendWith(MockitoExtension.class)
class LeaderboardRefreshFailNotificationProviderTest {

    @Mock
    private DiscordNotificationClient notificationClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OnsquadProperties onsquadProperties;

    @InjectMocks
    private LeaderboardRefreshFailNotificationProvider provider;

    @Test
    @DisplayName("리더보드 갱신 실패 시 스냅샷 키 목록을 JSON 파일로 첨부하여 알림을 전송한다")
    void sendLeaderboardUpdateFailAlert_WithFile() throws JsonProcessingException {
        List<String> snapshotKeys = List.of("snapshot:1", "snapshot:2");
        byte[] expectedBytes = "{\"failedKeys\":[\"snapshot:1\",\"snapshot:2\"]}".getBytes();
        when(onsquadProperties.getIdentifier()).thenReturn("prod-env");
        when(objectMapper.writeValueAsBytes(any())).thenReturn(expectedBytes);
        ArgumentCaptor<DiscordMessage> messageCaptor = ArgumentCaptor.forClass(DiscordMessage.class);
        ArgumentCaptor<byte[]> fileCaptor = ArgumentCaptor.forClass(byte[].class);

        provider.sendLeaderboardUpdateFailAlert(snapshotKeys);

        assertSoftly(softly -> {
            verify(notificationClient, times(1))
                    .sendNotification(messageCaptor.capture(), eq("failed_snapshot_keys.json"), fileCaptor.capture());

            DiscordMessage sentMessage = messageCaptor.getValue();
            softly.assertThat(sentMessage.getUsername()).isEqualTo("OnSquad Crew Leaderboard Update Scheduler");
            softly.assertThat(sentMessage.getContent()).contains("Leaderboard Update: Process Failed");
            softly.assertThat(sentMessage.getContent()).contains("Total Failed Snapshots");
            softly.assertThat(sentMessage.getContent()).contains("`2` keys");
            softly.assertThat(sentMessage.getContent()).contains("Environment Identifier");
            softly.assertThat(sentMessage.getContent()).contains("`prod-env`");

            softly.assertThat(sentMessage.getEmbeds().get(0).getTitle()).isEqualTo("Critical: Leaderboard Refresh Aborted");
            softly.assertThat(fileCaptor.getValue()).isEqualTo(expectedBytes);
        });
    }

    @Test
    @DisplayName("JSON 직렬화 중 예외가 발생하면 파일 첨부 없이 메시지만 전송한다")
    void sendLeaderboardUpdateFailAlert_WithoutFile_WhenJsonExceptionOccurs() throws JsonProcessingException {
        List<String> snapshotKeys = List.of("snapshot:1");
        when(onsquadProperties.getIdentifier()).thenReturn("prod-env");
        when(objectMapper.writeValueAsBytes(any())).thenThrow(new JsonProcessingException("Serialization Error") {
        });

        provider.sendLeaderboardUpdateFailAlert(snapshotKeys);

        verify(notificationClient, times(1)).sendNotification(any(DiscordMessage.class));
        verify(notificationClient, never()).sendNotification(any(), anyString(), any());
    }
}
