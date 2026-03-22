package revi1337.onsquad.member.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_ADDRESS_DETAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_ADDRESS_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_EMAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_NICKNAME_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_PASSWORD_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_ADDRESS_DETAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_ADDRESS_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_EMAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_NICKNAME_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_PASSWORD_VALUE;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import revi1337.onsquad.common.PersistenceLayerTestSupport;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.vo.Email;
import revi1337.onsquad.member.domain.vo.Nickname;

class MemberJpaRepositoryTest extends PersistenceLayerTestSupport {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Test
    void existsByNickname() {
        memberJpaRepository.save(Member.general(
                REVI_EMAIL_VALUE,
                REVI_PASSWORD_VALUE,
                REVI_NICKNAME_VALUE,
                REVI_ADDRESS_VALUE,
                REVI_ADDRESS_DETAIL_VALUE
        ));

        assertThat(memberJpaRepository.existsByNickname(new Nickname(REVI_NICKNAME_VALUE))).isTrue();
    }

    @Test
    void existsByEmail() {
        memberJpaRepository.save(Member.general(
                REVI_EMAIL_VALUE,
                REVI_PASSWORD_VALUE,
                REVI_NICKNAME_VALUE,
                REVI_ADDRESS_VALUE,
                REVI_ADDRESS_DETAIL_VALUE
        ));

        assertThat(memberJpaRepository.existsByEmail(new Email(REVI_EMAIL_VALUE))).isTrue();
    }

    @Test
    void findByEmail() {
        memberJpaRepository.save(Member.general(
                REVI_EMAIL_VALUE,
                REVI_PASSWORD_VALUE,
                REVI_NICKNAME_VALUE,
                REVI_ADDRESS_VALUE,
                REVI_ADDRESS_DETAIL_VALUE
        ));

        assertThat(memberJpaRepository.findByEmail(new Email(REVI_EMAIL_VALUE))).isPresent();
    }

    @Test
    void findByIdIn() {
        Member member1 = memberJpaRepository.save(Member.general(
                REVI_EMAIL_VALUE,
                REVI_PASSWORD_VALUE,
                REVI_NICKNAME_VALUE,
                REVI_ADDRESS_VALUE,
                REVI_ADDRESS_DETAIL_VALUE
        ));
        Member member2 = memberJpaRepository.save(Member.general(
                ANDONG_EMAIL_VALUE,
                ANDONG_PASSWORD_VALUE,
                ANDONG_NICKNAME_VALUE,
                ANDONG_ADDRESS_VALUE,
                ANDONG_ADDRESS_DETAIL_VALUE
        ));

        assertThat(memberJpaRepository.findByIdIn(List.of(member1.getId(), member2.getId()))).hasSize(2);
    }

    @Test
    void deleteById() {
        Member member = memberJpaRepository.save(Member.general(
                REVI_EMAIL_VALUE,
                REVI_PASSWORD_VALUE,
                REVI_NICKNAME_VALUE,
                REVI_ADDRESS_VALUE,
                REVI_ADDRESS_DETAIL_VALUE
        ));

        memberJpaRepository.deleteById(member.getId());
        clearPersistenceContext();

        assertThat(memberJpaRepository.findById(member.getId())).isEmpty();
    }
}
