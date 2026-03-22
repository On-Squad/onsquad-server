package revi1337.onsquad.crew.domain.repository;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.vo.Name;

public interface CrewJpaRepository extends JpaRepository<Crew, Long> {

    boolean existsByName(Name name);

    @Lock(PESSIMISTIC_WRITE)
    @Query("select c from Crew c where c.id = :id")
    Optional<Crew> findByIdForUpdate(Long id);

    List<Crew> findAllByMemberId(Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("delete Crew c where c.id in :ids")
    int deleteByIdIn(List<Long> ids);

    @Modifying
    @Query("update Crew c " +
            "set c.currentSize = c.currentSize - 1, c.version = c.version + 1 " +
            "where c.id = :id and c.currentSize > 0")
    int decrementCountById(Long id);

    @Modifying
    @Query("update Crew c set c.currentSize = c.currentSize - 1 " +
            "where c.id in (select cm.crew.id from CrewMember cm where cm.member.id = :memberId) " +
            "and c.currentSize > 0")
    int decrementCountByMemberId(Long memberId);

}
