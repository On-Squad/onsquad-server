package revi1337.onsquad.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.common.util.ObjectMapperUtils;
import revi1337.onsquad.notification.application.response.NotificationResponse;
import revi1337.onsquad.notification.domain.repository.NotificationRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public PageResponse<NotificationResponse> fetchNotifications(Long memberId, Pageable pageable) {
        Page<NotificationResponse> response = notificationRepository.findAllByReceiverId(memberId, pageable)
                .map(entity -> NotificationResponse.from(entity, ObjectMapperUtils.deserializeAsTreeOrEmpty(objectMapper, entity.getJson())));

        return PageResponse.from(response);
    }
}
