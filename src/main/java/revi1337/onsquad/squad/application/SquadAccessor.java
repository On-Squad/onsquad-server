package revi1337.onsquad.squad.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad.domain.error.SquadBusinessException;
import revi1337.onsquad.squad.domain.error.SquadErrorCode;
import revi1337.onsquad.squad.domain.model.SimpleSquad;
import revi1337.onsquad.squad.domain.model.SquadDetail;
import revi1337.onsquad.squad.domain.repository.SquadRepository;

@Component
@RequiredArgsConstructor
public class SquadAccessor {

    private final SquadRepository squadRepository;

    public Squad getById(Long squadId) {
        return squadRepository.findById(squadId)
                .orElseThrow(() -> new SquadBusinessException.NotFound(SquadErrorCode.NOT_FOUND));
    }

    public Squad getByIdForUpdate(Long squadId) {
        return squadRepository.findByIdForUpdate(squadId)
                .orElseThrow(() -> new SquadBusinessException.NotFound(SquadErrorCode.NOT_FOUND));
    }

    public Squad getDetailById(Long squadId) {
        return squadRepository.fetchSquadDetailById(squadId)
                .orElseThrow(() -> new SquadBusinessException.NotFound(SquadErrorCode.NOT_FOUND));
    }

    public Squad getReferenceById(Long squadId) {
        return squadRepository.getReferenceById(squadId);
    }

    public Page<SimpleSquad> fetchSquadsByCrewId(Long crewId, Pageable pageable) {
        return squadRepository.fetchSquadsByCrewId(crewId, pageable);
    }

    public Page<SquadDetail> fetchSquadsWithDetailByCrewIdAndCategory(Long crewId, CategoryType categoryType, Pageable pageable) {
        return squadRepository.fetchSquadsWithDetailByCrewIdAndCategory(crewId, categoryType, pageable);
    }
}
