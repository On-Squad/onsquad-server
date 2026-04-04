package revi1337.onsquad.notification.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import revi1337.onsquad.common.util.ObjectMapperUtils;
import revi1337.onsquad.notification.domain.model.NotificationMessage;
import revi1337.onsquad.notification.infrastructure.sse.SseEmitterManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNotificationMessageSubscriber implements MessageListener {

    private static final String CHANNEL_PREFIX = "channel:";
    private static final String EMPTY_SIGN = "";

    private final ObjectMapper defaultObjectMapper;
    private final SseEmitterManager sseEmitterManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String receiverId = new String(message.getChannel()).replaceFirst(CHANNEL_PREFIX, EMPTY_SIGN);
        NotificationMessage notificationMsg = ObjectMapperUtils.deserialize(defaultObjectMapper, new String(message.getBody()), NotificationMessage.class);
        sseEmitterManager.send(receiverId, notificationMsg);
    }
}
