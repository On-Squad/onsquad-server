package revi1337.onsquad.squad_request.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import revi1337.onsquad.auth.support.Authenticate;
import revi1337.onsquad.auth.support.CurrentMember;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.common.dto.RestResponse;
import revi1337.onsquad.common.presentation.support.AdaptivePageable;
import revi1337.onsquad.squad_request.application.SquadRequestCommandService;
import revi1337.onsquad.squad_request.application.SquadRequestQueryService;
import revi1337.onsquad.squad_request.application.response.MySquadRequestResponse;
import revi1337.onsquad.squad_request.application.response.SquadRequestResponse;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SquadRequestController {

    private final SquadRequestCommandService squadRequestCommandService;
    private final SquadRequestQueryService squadRequestQueryService;

    @PostMapping("/squads/{squadId}/requests")
    public ResponseEntity<RestResponse<Void>> request(
            @PathVariable Long squadId,
            @Authenticate CurrentMember currentMember
    ) {
        squadRequestCommandService.request(currentMember.id(), squadId);

        return ResponseEntity.ok(RestResponse.created());
    }

    @PatchMapping("/squads/{squadId}/requests/{requestId}")
    public ResponseEntity<RestResponse<Void>> acceptRequest(
            @PathVariable Long squadId,
            @PathVariable Long requestId,
            @Authenticate CurrentMember currentMember
    ) {
        squadRequestCommandService.acceptRequest(currentMember.id(), squadId, requestId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @DeleteMapping("/squads/{squadId}/requests/{requestId}")
    public ResponseEntity<RestResponse<Void>> rejectRequest(
            @PathVariable Long squadId,
            @PathVariable Long requestId,
            @Authenticate CurrentMember currentMember
    ) {
        squadRequestCommandService.rejectRequest(currentMember.id(), squadId, requestId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @DeleteMapping("/squads/{squadId}/requests/me")
    public ResponseEntity<RestResponse<Void>> cancelMyRequest(
            @PathVariable Long squadId,
            @Authenticate CurrentMember currentMember
    ) {
        squadRequestCommandService.cancelMyRequest(currentMember.id(), squadId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @GetMapping("/squads/{squadId}/requests")
    public ResponseEntity<RestResponse<PageResponse<SquadRequestResponse>>> fetchAllRequests(
            @PathVariable Long squadId,
            @AdaptivePageable(defaultSort = "requestAt") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<SquadRequestResponse> response = squadRequestQueryService.fetchAllRequests(currentMember.id(), squadId, pageable);

        return ResponseEntity.ok().body(RestResponse.success(response));
    }

    @GetMapping("/members/me/squad-requests")
    public ResponseEntity<RestResponse<PageResponse<MySquadRequestResponse>>> fetchMyRequests(
            @AdaptivePageable(defaultSort = "requestAt") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<MySquadRequestResponse> response = squadRequestQueryService.fetchMyRequests(currentMember.id(), pageable);

        return ResponseEntity.ok().body(RestResponse.success(response));
    }
}
