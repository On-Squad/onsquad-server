package revi1337.onsquad.notification.presentation;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import revi1337.onsquad.auth.support.Authenticate;
import revi1337.onsquad.auth.support.CurrentMember;
import revi1337.onsquad.auth.token.application.JsonWebTokenManager;
import revi1337.onsquad.auth.token.domain.model.ClaimsParser;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.common.dto.RestResponse;
import revi1337.onsquad.common.presentation.support.AdaptivePageable;
import revi1337.onsquad.notification.application.NotificationCommandService;
import revi1337.onsquad.notification.application.NotificationQueryService;
import revi1337.onsquad.notification.application.NotificationService;
import revi1337.onsquad.notification.application.response.NotificationResponse;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {

    private static final String LAST_EVENT_ID = "Last-Event-ID";

    private final NotificationService notificationService;
    private final NotificationCommandService notificationCommandService;
    private final NotificationQueryService notificationQueryService;
    private final JsonWebTokenManager jsonWebTokenManager;

    @GetMapping(value = "/notifications/sse", produces = TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @RequestParam String accessToken,
            @RequestHeader(value = LAST_EVENT_ID, required = false) Long lastEventId
    ) {
        ClaimsParser claimsParser = jsonWebTokenManager.verifyAccessToken(accessToken);
        return notificationService.connect(claimsParser.parseIdentity(), lastEventId);
    }

    @GetMapping("/members/me/notifications")
    public ResponseEntity<RestResponse<PageResponse<NotificationResponse>>> fetchNotifications(
            @AdaptivePageable(defaultSort = "id") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<NotificationResponse> response = notificationQueryService.fetchNotifications(currentMember.id(), pageable);

        return ResponseEntity.ok(RestResponse.success(response));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<RestResponse<RestResponse<Void>>> readNotification(
            @PathVariable Long notificationId,
            @Authenticate CurrentMember currentMember
    ) {
        notificationCommandService.read(currentMember.id(), notificationId);

        return ResponseEntity.ok(RestResponse.noContent());
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<RestResponse<RestResponse<Void>>> readNotifications(
            @Authenticate CurrentMember currentMember
    ) {
        notificationCommandService.readAll(currentMember.id());

        return ResponseEntity.ok(RestResponse.noContent());
    }
}
