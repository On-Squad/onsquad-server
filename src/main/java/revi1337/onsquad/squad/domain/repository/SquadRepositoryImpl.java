package revi1337.onsquad.squad.domain.repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad.domain.model.SimpleSquad;
import revi1337.onsquad.squad.domain.model.SquadDetail;

@Repository
@RequiredArgsConstructor
public class SquadRepositoryImpl implements SquadRepository {

    private final SquadJpaRepository squadJpaRepository;
    private final SquadQueryDslRepository squadQueryDslRepository;

    @Override
    public Squad getReferenceById(Long id) {
        return squadJpaRepository.getReferenceById(id);
    }

    @Override
    public Squad save(Squad squad) {
        return squadJpaRepository.save(squad);
    }

    @Override
    public Squad saveAndFlush(Squad squad) {
        return squadJpaRepository.saveAndFlush(squad);
    }

    @Override
    public Optional<Squad> findById(Long id) {
        return squadJpaRepository.findById(id);
    }

    @Override
    public Optional<Squad> findByIdForUpdate(Long id) {
        return squadJpaRepository.findByIdForUpdate(id);
    }

    @Override
    public Optional<Squad> findWithCrewById(Long id) {
        return squadJpaRepository.findWithCrewById(id);
    }

    @Override
    public Optional<Squad> fetchSquadDetailById(Long id) {
        return squadQueryDslRepository.fetchSquadDetailById(id);
    }

    @Override
    public Page<SquadDetail> fetchSquadsWithDetailByCrewIdAndCategory(Long crewId, CategoryType categoryType, Pageable pageable) {
        return squadQueryDslRepository.fetchSquadsWithDetailByCrewIdAndCategory(crewId, categoryType, pageable);
    }

    @Override
    public Page<SimpleSquad> fetchSquadsByCrewId(Long crewId, Pageable pageable) {
        return squadQueryDslRepository.fetchSquadsByCrewId(crewId, pageable);
    }

    @Override
    public List<Long> findIdsByMemberId(Long memberId) {
        return squadJpaRepository.findIdsByMemberId(memberId);
    }

    @Override
    public List<Long> findIdsByCrewIdIn(List<Long> crewIds) {
        return squadJpaRepository.findIdsByCrewIdIn(crewIds);
    }

    @Override
    public int deleteByIdIn(List<Long> ids) {
        return squadJpaRepository.deleteByIdIn(ids);
    }

    @Override
    public int deleteByCrewIdIn(List<Long> crewIds) {
        return squadJpaRepository.deleteByCrewIdIn(crewIds);
    }

    @Override
    public int decrementCountByMemberId(Long memberId) {
        return squadJpaRepository.decrementCountByMemberId(memberId);
    }
}
