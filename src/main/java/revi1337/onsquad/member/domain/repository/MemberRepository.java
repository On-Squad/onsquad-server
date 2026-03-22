package revi1337.onsquad.member.domain.repository;

import java.util.List;
import java.util.Optional;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.vo.Email;
import revi1337.onsquad.member.domain.vo.Nickname;

public interface MemberRepository {

    Member save(Member member);

    Member saveAndFlush(Member member);

    Member getReferenceById(Long id);

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(Email email);

    List<Member> findByIdIn(List<Long> memberIds);

    boolean existsByNickname(Nickname nickname);

    boolean existsByEmail(Email email);

    void deleteById(Long id);

}
