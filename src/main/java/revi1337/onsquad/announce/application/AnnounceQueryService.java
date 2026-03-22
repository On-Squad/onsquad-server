package revi1337.onsquad.announce.application;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import revi1337.onsquad.announce.application.dto.response.AnnounceResponse;
import revi1337.onsquad.announce.application.dto.response.AnnounceWithPinAndModifyStateResponse;
import revi1337.onsquad.announce.application.dto.response.AnnouncesWithWriteStateResponse;
import revi1337.onsquad.announce.domain.AnnouncePolicy;
import revi1337.onsquad.announce.domain.model.Announces;
import revi1337.onsquad.announce.domain.repository.AnnounceRepository;
import revi1337.onsquad.crew_member.application.CrewMemberAccessor;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.CrewRole;
import revi1337.onsquad.crew_member.domain.model.CrewMembers;

@Service
@RequiredArgsConstructor
public class AnnounceQueryService {

    private final CrewMemberAccessor crewMemberAccessor;
    private final AnnounceCacheService announceCacheService;
    private final AnnounceRepository announceRepository;

    public AnnounceWithPinAndModifyStateResponse findAnnounce(Long memberId, Long crewId, Long announceId) {
        CrewMember me = crewMemberAccessor.getByMemberIdAndCrewId(memberId, crewId);
        AnnounceResponse announce = announceCacheService.getAnnounce(crewId, announceId);

        boolean canPin = AnnouncePolicy.canPin(me);
        boolean canModify = AnnouncePolicy.canModify(me, announce.writer().id());

        return AnnounceWithPinAndModifyStateResponse.from(canPin, canModify, announce);
    }

    @Transactional(readOnly = true)
    public AnnouncesWithWriteStateResponse findAnnounces(Long memberId, Long crewId) {
        CrewMember me = crewMemberAccessor.getByMemberIdAndCrewId(memberId, crewId);
        Announces announces = announceRepository.fetchAllByCrewId(crewId);
        CrewMembers crewMembers = crewMemberAccessor.findAllByCrewIdAndMemberIdIn(crewId, announces.getWriterIds());
        Map<Long, CrewRole> memberRoleMap = crewMembers.splitRolesByMemberId();

        boolean canWrite = AnnouncePolicy.canWrite(me);
        List<AnnounceResponse> response = announces.values().stream()
                .map(announce -> AnnounceResponse.from(memberRoleMap.get(announce.getMember().getId()), announce))
                .toList();

        return AnnouncesWithWriteStateResponse.from(canWrite, response);
    }
}
