package revi1337.onsquad.crew.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.vo.Name;
import revi1337.onsquad.crew.domain.error.CrewBusinessException;
import revi1337.onsquad.crew.domain.error.CrewErrorCode;
import revi1337.onsquad.crew.domain.model.CrewDetail;
import revi1337.onsquad.crew.domain.model.CrewStatistic;
import revi1337.onsquad.crew.domain.model.CrewWithOwnerState;
import revi1337.onsquad.crew.domain.repository.CrewRepository;

@Component
@RequiredArgsConstructor
public class CrewAccessor {

    private final CrewRepository crewRepository;

    public Crew getById(Long crewId) {
        return crewRepository.findById(crewId)
                .orElseThrow(() -> new CrewBusinessException.NotFound(CrewErrorCode.NOT_FOUND));
    }

    public Crew getByIdForUpdate(Long crewId) {
        return crewRepository.findByIdForUpdate(crewId)
                .orElseThrow(() -> new CrewBusinessException.NotFound(CrewErrorCode.NOT_FOUND));
    }

    public Crew getReferenceById(Long crewId) {
        return crewRepository.getReferenceById(crewId);
    }

    public CrewDetail getCrewWithDetailById(Long crewId) {
        return crewRepository.fetchCrewWithDetailById(crewId)
                .orElseThrow(() -> new CrewBusinessException.NotFound(CrewErrorCode.NOT_FOUND));
    }

    public CrewStatistic getStatisticById(Long crewId) {
        return crewRepository.getStatisticById(crewId);
    }

    public CrewDetail fetchCrewWithDetailById(Long crewId) {
        return crewRepository.fetchCrewWithDetailById(crewId)
                .orElseThrow(() -> new CrewBusinessException.NotFound(CrewErrorCode.NOT_FOUND));
    }

    public Page<CrewDetail> fetchCrewsWithDetailByName(String crewName, Pageable pageable) {
        return crewRepository.fetchCrewsWithDetailByName(crewName, pageable);
    }

    public List<CrewWithOwnerState> fetchCrewWithStateByIdsIn(List<Long> crewIds, Long currentMemberId) {
        return crewRepository.fetchCrewsWithStateByIdIn(crewIds, currentMemberId);
    }

    public boolean checkCrewNameExists(String name) {
        return crewRepository.existsByName(new Name(name));
    }

    public void validateCrewNameIsDuplicate(String name) {
        if (crewRepository.existsByName(new Name(name))) {
            throw new CrewBusinessException.AlreadyExists(CrewErrorCode.ALREADY_EXISTS);
        }
    }
}
