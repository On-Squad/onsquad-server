package revi1337.onsquad.crew_participant.presentation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import revi1337.onsquad.auth.application.CurrentMember;
import revi1337.onsquad.auth.config.Authenticate;
import revi1337.onsquad.common.dto.RestResponse;
import revi1337.onsquad.crew_participant.application.CrewParticipantCommandService;
import revi1337.onsquad.crew_participant.application.CrewParticipantQueryService;
import revi1337.onsquad.crew_participant.presentation.dto.response.CrewRequestWithCrewResponse;
import revi1337.onsquad.crew_participant.presentation.dto.response.CrewRequestWithMemberResponse;

@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class CrewParticipantController {

    private final CrewParticipantCommandService crewParticipantCommandService;
    private final CrewParticipantQueryService crewParticipantQueryService;

    @PostMapping("/crews/{crewId}/requests")
    public ResponseEntity<RestResponse<String>> request(
            @PathVariable Long crewId,
            @Authenticate CurrentMember currentMember
    ) {
        crewParticipantCommandService.request(currentMember.id(), crewId);

        return ResponseEntity.ok().body(RestResponse.created());
    }

    @PatchMapping("/crews/{crewId}/requests/{requestId}")
    public ResponseEntity<RestResponse<String>> acceptRequest(
            @PathVariable Long crewId,
            @PathVariable Long requestId,
            @Authenticate CurrentMember currentMember
    ) {
        crewParticipantCommandService.acceptRequest(currentMember.id(), crewId, requestId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @DeleteMapping("/crews/{crewId}/requests/{requestId}")
    public ResponseEntity<RestResponse<String>> rejectRequest(
            @PathVariable Long crewId,
            @PathVariable Long requestId,
            @Authenticate CurrentMember currentMember
    ) {
        crewParticipantCommandService.rejectRequest(currentMember.id(), crewId, requestId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @GetMapping("/crews/{crewId}/manage/requests")
    public ResponseEntity<RestResponse<List<CrewRequestWithMemberResponse>>> fetchAllRequests(
            @PathVariable Long crewId,
            @PageableDefault Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        List<CrewRequestWithMemberResponse> requestResponses = crewParticipantQueryService
                .fetchAllRequests(currentMember.id(), crewId, pageable).stream()
                .map(CrewRequestWithMemberResponse::from)
                .toList();

        return ResponseEntity.ok().body(RestResponse.success(requestResponses));
    }

    @DeleteMapping("/crews/{crewId}/requests/me")
    public ResponseEntity<RestResponse<String>> cancelMyRequest(
            @PathVariable Long crewId,
            @Authenticate CurrentMember currentMember
    ) {
        crewParticipantCommandService.cancelMyRequest(currentMember.id(), crewId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @GetMapping("/crew-requests/me")
    public ResponseEntity<RestResponse<List<CrewRequestWithCrewResponse>>> fetchAllCrewRequests(
            @Authenticate CurrentMember currentMember
    ) {
        List<CrewRequestWithCrewResponse> crewRequestWithCrewResponse = crewParticipantQueryService
                .fetchAllCrewRequests(currentMember.id()).stream()
                .map(CrewRequestWithCrewResponse::from)
                .toList();

        return ResponseEntity.ok().body(RestResponse.success(crewRequestWithCrewResponse));
    }
}
