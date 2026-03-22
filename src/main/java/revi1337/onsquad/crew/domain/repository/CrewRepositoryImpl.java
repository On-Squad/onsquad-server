package revi1337.onsquad.crew.domain.repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.vo.Name;
import revi1337.onsquad.crew.domain.model.CrewDetail;
import revi1337.onsquad.crew.domain.model.CrewStatistic;
import revi1337.onsquad.crew.domain.model.CrewWithOwnerState;

@Repository
@RequiredArgsConstructor
public class CrewRepositoryImpl implements CrewRepository {

    private final CrewJpaRepository crewJpaRepository;
    private final CrewQueryDslRepository crewQueryDslRepository;

    @Override
    public Crew save(Crew crew) {
        return crewJpaRepository.save(crew);
    }

    @Override
    public Crew saveAndFlush(Crew crew) {
        return crewJpaRepository.saveAndFlush(crew);
    }

    @Override
    public Crew getReferenceById(Long id) {
        return crewJpaRepository.getReferenceById(id);
    }

    @Override
    public boolean existsByName(Name name) {
        return crewJpaRepository.existsByName(name);
    }

    @Override
    public Optional<Crew> findById(Long id) {
        return crewJpaRepository.findById(id);
    }

    @Override
    public Optional<Crew> findByIdForUpdate(Long id) {
        return crewJpaRepository.findByIdForUpdate(id);
    }

    @Override
    public Optional<CrewDetail> fetchCrewWithDetailById(Long id) {
        return crewQueryDslRepository.fetchCrewWithDetailById(id);
    }

    @Override
    public Page<CrewDetail> fetchCrewsWithDetailByName(String name, Pageable pageable) {
        return crewQueryDslRepository.fetchCrewsWithDetailByName(name, pageable);
    }

    @Override
    public List<CrewWithOwnerState> fetchCrewsWithStateByIdIn(List<Long> ids, Long currentMemberId) {
        return crewQueryDslRepository.fetchCrewsWithStateByIdIn(ids, currentMemberId);
    }

    @Override
    public CrewStatistic getStatisticById(Long crewId) {
        return crewQueryDslRepository.getStatisticById(crewId);
    }

    @Override
    public List<Crew> findAllByMemberId(Long memberId) {
        return crewJpaRepository.findAllByMemberId(memberId);
    }

    @Override
    public int deleteByIdIn(List<Long> ids) {
        return crewJpaRepository.deleteByIdIn(ids);
    }

    @Override
    public int decrementCountById(Long id) {
        return crewJpaRepository.decrementCountById(id);
    }

    @Override
    public int decrementCountByMemberId(Long memberId) {
        int deleted = crewJpaRepository.decrementCountByMemberId(memberId);
        if (deleted == 0) {
            throw new ObjectOptimisticLockingFailureException(
                    Crew.class,
                    "The crew current size was not updated. It might have been modified by another transaction or does not exist."
            );
        }
        return deleted;
    }
}
