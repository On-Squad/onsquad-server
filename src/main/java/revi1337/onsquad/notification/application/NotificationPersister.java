package revi1337.onsquad.notification.application;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import revi1337.onsquad.common.util.ObjectMapperUtils;
import revi1337.onsquad.notification.domain.Notification;
import revi1337.onsquad.notification.domain.entity.NotificationEntity;
import revi1337.onsquad.notification.domain.repository.NotificationRepository;

@Component
@RequiredArgsConstructor
public class NotificationPersister {

    private final ObjectMapper defaultObjectMapper;
    private final NotificationRepository notificationRepository;

    @Transactional(propagation = REQUIRES_NEW)
    public NotificationEntity persist(Notification notification) {
        return notificationRepository.save(NotificationEntity.builder()
                .publisherId(notification.getPublisherId())
                .receiverId(notification.getReceiverId())
                .topic(notification.getTopic())
                .detail(notification.getDetail())
                .json(ObjectMapperUtils.serializeToString(defaultObjectMapper, notification.getPayload()))
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
