package revi1337.onsquad.crew.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import revi1337.onsquad.announce.application.AnnounceCacheService;
import revi1337.onsquad.announce.application.dto.response.AnnounceResponse;
import revi1337.onsquad.crew.application.dto.response.CrewMainResponse;
import revi1337.onsquad.crew.application.dto.response.CrewManageResponse;
import revi1337.onsquad.crew.domain.CrewPolicy;
import revi1337.onsquad.crew.domain.model.CrewDetail;
import revi1337.onsquad.crew.domain.model.CrewStatistic;
import revi1337.onsquad.crew_member.application.CrewMemberAccessor;
import revi1337.onsquad.crew_member.application.leaderboard.CrewRankerCacheService;
import revi1337.onsquad.crew_member.application.response.CrewRankerResponse;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.squad.application.SquadAccessor;
import revi1337.onsquad.squad.domain.model.SquadDetail;
import revi1337.onsquad.squad.domain.model.SquadLinkableGroup;
import revi1337.onsquad.squad_category.application.SquadCategoryAccessor;
import revi1337.onsquad.squad_category.domain.model.SquadCategories;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrewMainService {

    private final CrewAccessor crewAccessor;
    private final CrewMemberAccessor crewMemberAccessor;
    private final CrewRankerCacheService crewRankerCacheService;
    private final AnnounceCacheService announceCacheService;
    private final SquadAccessor squadAccessor;
    private final SquadCategoryAccessor squadCategoryAccessor;

    public CrewMainResponse fetchMain(Long memberId, Long crewId, Pageable pageable) {
        List<AnnounceResponse> announces = announceCacheService.getDefaultAnnounces(crewId);
        List<CrewRankerResponse> rankers = crewRankerCacheService.findAllByCrewId(crewId);

        CrewMember me = crewMemberAccessor.getByMemberIdAndCrewId(memberId, crewId);
        CrewDetail crew = crewAccessor.getCrewWithDetailById(crewId);
        SquadLinkableGroup<SquadDetail> squadGroup = getSquads(crewId, pageable);

        return CrewMainResponse.from(CrewPolicy.canManage(me), crew, announces, rankers, squadGroup.results());
    }

    public CrewManageResponse fetchManageInfo(Long memberId, Long crewId) {
        CrewMember me = crewMemberAccessor.getByMemberIdAndCrewId(memberId, crewId);
        CrewPolicy.ensureManageable(me);

        CrewStatistic statistic = crewAccessor.getStatisticById(crewId);
        boolean canModify = CrewPolicy.canModify(me);
        boolean canDelete = CrewPolicy.canDelete(me);

        return CrewManageResponse.from(canModify, canDelete, statistic);
    }

    private SquadLinkableGroup<SquadDetail> getSquads(Long crewId, Pageable pageable) {
        Page<SquadDetail> squads = squadAccessor.fetchSquadsWithDetailByCrewIdAndCategory(crewId, null, pageable);
        SquadLinkableGroup<SquadDetail> squadGroup = new SquadLinkableGroup<>(squads.getContent());
        SquadCategories squadCategories = squadCategoryAccessor.fetchCategoriesBySquadIdIn(squadGroup.getSquadIds());
        squadGroup.linkCategories(squadCategories);

        return squadGroup;
    }
}
