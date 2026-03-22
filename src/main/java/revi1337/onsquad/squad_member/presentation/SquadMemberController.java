package revi1337.onsquad.squad_member.presentation;

import java.util.List;
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
import revi1337.onsquad.squad_member.application.SquadMemberCommandService;
import revi1337.onsquad.squad_member.application.SquadMemberQueryService;
import revi1337.onsquad.squad_member.application.response.MyParticipantSquadResponse;
import revi1337.onsquad.squad_member.application.response.SquadMemberResponse;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SquadMemberController {

    private final SquadMemberCommandService squadMemberCommandService;
    private final SquadMemberQueryService squadMemberQueryService;

    @GetMapping("/squads/{squadId}/members")
    public ResponseEntity<RestResponse<PageResponse<SquadMemberResponse>>> fetchParticipants(
            @PathVariable Long squadId,
            @AdaptivePageable(defaultSort = "participateAt") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<SquadMemberResponse> response = squadMemberQueryService.fetchParticipants(currentMember.id(), squadId, pageable);

        return ResponseEntity.ok().body(RestResponse.success(response));
    }

    @PatchMapping("/squads/{squadId}/members/{targetMemberId}/leader")
    public ResponseEntity<RestResponse<Void>> delegateLeader(
            @PathVariable Long squadId,
            @PathVariable Long targetMemberId,
            @Authenticate CurrentMember currentMember
    ) {
        squadMemberCommandService.delegateLeader(currentMember.id(), squadId, targetMemberId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @DeleteMapping("/squads/{squadId}/members/me")
    public ResponseEntity<RestResponse<Void>> leaveSquad(
            @PathVariable Long squadId,
            @Authenticate CurrentMember currentMember
    ) {
        squadMemberCommandService.leaveSquad(currentMember.id(), squadId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @DeleteMapping("/squads/{squadId}/members/{targetMemberId}")
    public ResponseEntity<RestResponse<Void>> kickOutMember(
            @PathVariable Long squadId,
            @PathVariable Long targetMemberId,
            @Authenticate CurrentMember currentMember
    ) {
        squadMemberCommandService.kickOutMember(currentMember.id(), squadId, targetMemberId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @GetMapping("/members/me/squad-participants")
    public ResponseEntity<RestResponse<List<MyParticipantSquadResponse>>> fetchMyParticipatingSquads(
            @Authenticate CurrentMember currentMember
    ) {
        List<MyParticipantSquadResponse> response = squadMemberQueryService.fetchMyParticipatingSquads(currentMember.id());

        return ResponseEntity.ok().body(RestResponse.success(response));
    }
}
