package revi1337.onsquad.member.domain.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.vo.Email;
import revi1337.onsquad.member.domain.vo.Nickname;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    boolean existsByNickname(Nickname nickname);

    boolean existsByEmail(Email email);

    Optional<Member> findByEmail(Email email);

    List<Member> findByIdIn(List<Long> memberIds);

    @Modifying
    @Query("delete Member m where m.id = :id")
    void deleteById(Long id);

}
