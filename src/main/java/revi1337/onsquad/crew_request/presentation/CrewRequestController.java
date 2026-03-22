package revi1337.onsquad.crew_request.presentation;

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
import revi1337.onsquad.crew_request.application.CrewRequestCommandService;
import revi1337.onsquad.crew_request.application.CrewRequestQueryService;
import revi1337.onsquad.crew_request.application.response.CrewRequestResponse;
import revi1337.onsquad.crew_request.application.response.CrewRequestWithCrewResponse;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CrewRequestController {

    private final CrewRequestCommandService crewRequestCommandService;
    private final CrewRequestQueryService crewRequestQueryService;

    @PostMapping("/crews/{crewId}/requests")
    public ResponseEntity<RestResponse<Void>> request(
            @PathVariable Long crewId,
            @Authenticate CurrentMember currentMember
    ) {
        crewRequestCommandService.request(currentMember.id(), crewId);

        return ResponseEntity.ok().body(RestResponse.created());
    }

    @PatchMapping("/crews/{crewId}/requests/{requestId}")
    public ResponseEntity<RestResponse<Void>> acceptRequest(
            @PathVariable Long crewId,
            @PathVariable Long requestId,
            @Authenticate CurrentMember currentMember
    ) {
        crewRequestCommandService.acceptRequest(currentMember.id(), crewId, requestId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @DeleteMapping("/crews/{crewId}/requests/{requestId}")
    public ResponseEntity<RestResponse<Void>> rejectRequest(
            @PathVariable Long crewId,
            @PathVariable Long requestId,
            @Authenticate CurrentMember currentMember
    ) {
        crewRequestCommandService.rejectRequest(currentMember.id(), crewId, requestId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @GetMapping("/crews/{crewId}/requests")
    public ResponseEntity<RestResponse<PageResponse<CrewRequestResponse>>> fetchAllRequests(
            @PathVariable Long crewId,
            @AdaptivePageable(defaultSort = "requestAt") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<CrewRequestResponse> response = crewRequestQueryService.fetchAllRequests(currentMember.id(), crewId, pageable);

        return ResponseEntity.ok().body(RestResponse.success(response));
    }

    @DeleteMapping("/crews/{crewId}/requests/me")
    public ResponseEntity<RestResponse<Void>> cancelMyRequest(
            @PathVariable Long crewId,
            @Authenticate CurrentMember currentMember
    ) {
        crewRequestCommandService.cancelMyRequest(currentMember.id(), crewId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @GetMapping("/members/me/crew-requests")
    public ResponseEntity<RestResponse<PageResponse<CrewRequestWithCrewResponse>>> fetchAllCrewRequests(
            @AdaptivePageable(defaultSort = "requestAt") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<CrewRequestWithCrewResponse> response = crewRequestQueryService.fetchAllCrewRequests(currentMember.id(), pageable);

        return ResponseEntity.ok().body(RestResponse.success(response));
    }
}
