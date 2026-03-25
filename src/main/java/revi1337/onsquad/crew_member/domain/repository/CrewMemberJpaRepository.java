package revi1337.onsquad.crew_member.domain.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;

public interface CrewMemberJpaRepository extends JpaRepository<CrewMember, Long>, CrewMemberQueryRepository {

    boolean existsByMemberIdAndCrewId(Long memberId, Long crewId);

    @Query("select cm from CrewMember cm where cm.crew.id = :crewId and cm.member.id = :memberId")
    Optional<CrewMember> findByCrewIdAndMemberId(Long crewId, Long memberId);

    List<CrewMember> findAllByCrewIdAndMemberIdIn(Long crewId, List<Long> writerIds);

    @Modifying
    @Query("delete CrewMember cm where cm.member.id = :memberId")
    int deleteByMemberId(Long memberId);

    @Modifying
    @Query("delete CrewMember cm where cm.crew.id in :crewIds")
    int deleteByCrewIdIn(List<Long> crewIds);

    @Transactional
    @Modifying
    @Query("delete CrewMember cm where cm.crew.id = :crewId and cm.member.id = :memberId")
    int deleteByCrewIdAndMemberId(Long crewId, Long memberId);
    
}
