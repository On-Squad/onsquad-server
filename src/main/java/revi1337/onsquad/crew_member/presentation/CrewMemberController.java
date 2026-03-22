package revi1337.onsquad.crew_member.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import revi1337.onsquad.auth.support.Authenticate;
import revi1337.onsquad.auth.support.CurrentMember;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.common.dto.RestResponse;
import revi1337.onsquad.common.presentation.support.AdaptivePageable;
import revi1337.onsquad.crew_member.application.CrewMemberCommandService;
import revi1337.onsquad.crew_member.application.CrewMemberQueryService;
import revi1337.onsquad.crew_member.application.response.CrewMemberResponse;
import revi1337.onsquad.crew_member.application.response.MyParticipantCrewResponse;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CrewMemberController {

    private final CrewMemberCommandService crewMemberCommandService;
    private final CrewMemberQueryService crewMemberQueryService;

    @GetMapping("/crews/{crewId}/members")
    public ResponseEntity<RestResponse<PageResponse<CrewMemberResponse>>> fetchParticipants(
            @PathVariable Long crewId,
            @AdaptivePageable(defaultSort = "participateAt") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<CrewMemberResponse> response = crewMemberQueryService.fetchParticipants(currentMember.id(), crewId, pageable);

        return ResponseEntity.ok().body(RestResponse.success(response));
    }

    @PatchMapping("/crews/{crewId}/members/{targetMemberId}/owner")
    public ResponseEntity<RestResponse<Void>> delegateOwner(
            @PathVariable Long crewId,
            @PathVariable Long targetMemberId,
            @Authenticate CurrentMember currentMember
    ) {
        crewMemberCommandService.delegateOwner(currentMember.id(), crewId, targetMemberId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @DeleteMapping("/crews/{crewId}/members/me")
    public ResponseEntity<RestResponse<Void>> leaveCrew(
            @PathVariable Long crewId,
            @Authenticate CurrentMember currentMember
    ) {
        crewMemberCommandService.leaveCrew(currentMember.id(), crewId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @DeleteMapping("/crews/{crewId}/members/{targetMemberId}")
    public ResponseEntity<RestResponse<Void>> kickOutMember(
            @PathVariable Long crewId,
            @PathVariable Long targetMemberId,
            @Authenticate CurrentMember currentMember
    ) {
        crewMemberCommandService.kickOutMember(currentMember.id(), crewId, targetMemberId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @GetMapping("/members/me/crew-participants")
    public ResponseEntity<RestResponse<PageResponse<MyParticipantCrewResponse>>> fetchMyParticipatingCrews(
            @AdaptivePageable Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<MyParticipantCrewResponse> response = crewMemberQueryService.fetchMyParticipatingCrews(currentMember.id(), pageable);

        return ResponseEntity.ok().body(RestResponse.success(response));
    }
}
