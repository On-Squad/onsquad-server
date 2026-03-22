package revi1337.onsquad.squad.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import revi1337.onsquad.auth.support.Authenticate;
import revi1337.onsquad.auth.support.CurrentMember;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.common.dto.RestResponse;
import revi1337.onsquad.common.presentation.support.AdaptivePageable;
import revi1337.onsquad.squad.application.SquadCommandService;
import revi1337.onsquad.squad.application.SquadQueryService;
import revi1337.onsquad.squad.application.response.SquadResponse;
import revi1337.onsquad.squad.application.response.SquadWithLeaderStateResponse;
import revi1337.onsquad.squad.application.response.SquadWithStatesResponse;
import revi1337.onsquad.squad.presentation.request.SquadCreateRequest;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SquadController {

    private final SquadCommandService squadCommandService;
    private final SquadQueryService squadQueryService;

    @PostMapping("/crews/{crewId}/squads")
    public ResponseEntity<RestResponse<Void>> newSquad(
            @PathVariable Long crewId,
            @Valid @RequestBody SquadCreateRequest request,
            @Authenticate CurrentMember currentMember
    ) {
        squadCommandService.newSquad(currentMember.id(), crewId, request.toDomainSpec());

        return ResponseEntity.ok(RestResponse.created());
    }

    @GetMapping("/crews/{crewId}/squads")
    public ResponseEntity<RestResponse<PageResponse<SquadResponse>>> fetchSquads(
            @PathVariable Long crewId,
            @RequestParam(required = false) CategoryType category,
            @AdaptivePageable(defaultSort = "createdAt") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<SquadResponse> response = squadQueryService.fetchSquadsByCrewId(currentMember.id(), crewId, category, pageable);

        return ResponseEntity.ok(RestResponse.success(response));
    }

    @GetMapping("/squads/{squadId}")
    public ResponseEntity<RestResponse<SquadWithStatesResponse>> fetchSquad(
            @PathVariable Long squadId,
            @Authenticate CurrentMember currentMember
    ) {
        SquadWithStatesResponse response = squadQueryService.fetchSquad(currentMember.id(), squadId);

        return ResponseEntity.ok(RestResponse.success(response));
    }

    @DeleteMapping("/squads/{squadId}")
    public ResponseEntity<RestResponse<Void>> deleteSquad(
            @PathVariable Long squadId,
            @Authenticate CurrentMember currentMember
    ) {
        squadCommandService.deleteSquad(currentMember.id(), squadId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @GetMapping("/crews/{crewId}/squads/manage")
    public ResponseEntity<RestResponse<PageResponse<SquadWithLeaderStateResponse>>> fetchManageList(
            @PathVariable Long crewId,
            @AdaptivePageable(defaultSort = "createdAt") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<SquadWithLeaderStateResponse> response = squadQueryService.fetchManageList(currentMember.id(), crewId, pageable);

        return ResponseEntity.ok().body(RestResponse.success(response));
    }
}
