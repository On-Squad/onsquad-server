package revi1337.onsquad.crew.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_1;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_2;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_3;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_4;
import static revi1337.onsquad.common.fixture.CrewValueFixture.CREW_DETAIL;
import static revi1337.onsquad.common.fixture.CrewValueFixture.CREW_INTRODUCE;
import static revi1337.onsquad.common.fixture.CrewValueFixture.CREW_NAME;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG;
import static revi1337.onsquad.common.fixture.MemberFixture.KWANGWON;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI;
import static revi1337.onsquad.common.fixture.MemberValueFixture.ANDONG_MBTI;
import static revi1337.onsquad.common.fixture.MemberValueFixture.ANDONG_NICKNAME;
import static revi1337.onsquad.common.fixture.MemberValueFixture.KWANGWON_MBTI;
import static revi1337.onsquad.common.fixture.MemberValueFixture.KWANGWON_NICKNAME;
import static revi1337.onsquad.common.fixture.MemberValueFixture.REVI_INTRODUCE_VALUE;
import static revi1337.onsquad.common.fixture.MemberValueFixture.REVI_MBTI;
import static revi1337.onsquad.common.fixture.MemberValueFixture.REVI_NICKNAME;
import static revi1337.onsquad.common.fixture.MemberValueFixture.REVI_NICKNAME_VALUE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import revi1337.onsquad.common.PersistenceLayerTestSupport;
import revi1337.onsquad.crew.domain.dto.CrewDomainDto;
import revi1337.onsquad.crew.domain.dto.EnrolledCrewDomainDto;
import revi1337.onsquad.crew_member.domain.CrewMember;
import revi1337.onsquad.crew_member.domain.CrewMemberJpaRepository;
import revi1337.onsquad.member.domain.Member;
import revi1337.onsquad.member.domain.MemberJpaRepository;
import revi1337.onsquad.member.domain.dto.SimpleMemberDomainDto;
import revi1337.onsquad.member.domain.vo.Introduce;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.member.domain.vo.Nickname;

@Import(CrewQueryDslRepository.class)
class CrewQueryDslRepositoryTest extends PersistenceLayerTestSupport {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private CrewJpaRepository crewJpaRepository;

    @Autowired
    private CrewMemberJpaRepository crewMemberJpaRepository;

    @Autowired
    private CrewQueryDslRepository crewQueryDslRepository;

    @Nested
    @DisplayName("Crew 프로젝션 조회를 테스트한다.")
    class FindCrewById {

        @Test
        @DisplayName("Crew 프로젝션 조회에 성공한다.")
        void success() {
            Member REVI = memberJpaRepository.save(REVI());
            Crew CREW = crewJpaRepository.save(CREW(REVI));

            Optional<CrewDomainDto> OPTIONAL_CREW = crewQueryDslRepository.findCrewById(CREW.getId());

            assertThat(OPTIONAL_CREW).isPresent();
            assertThat(OPTIONAL_CREW.get().getId()).isEqualTo(CREW.getId());
            assertThat(OPTIONAL_CREW.get().getName()).isEqualTo(CREW_NAME);
            assertThat(OPTIONAL_CREW.get().getIntroduce()).isEqualTo(CREW_INTRODUCE);
            assertThat(OPTIONAL_CREW.get().getDetail()).isEqualTo(CREW_DETAIL);
            assertThat(OPTIONAL_CREW.get().getImageUrl()).isNull();
            assertThat(OPTIONAL_CREW.get().getKakaoLink()).isNull();
            assertThat(OPTIONAL_CREW.get().getMemberCnt()).isEqualTo(1);
            assertThat(OPTIONAL_CREW.get().getCrewOwner()).isEqualTo(new SimpleMemberDomainDto(
                    REVI.getId(),
                    new Nickname(REVI_NICKNAME_VALUE),
                    new Introduce(REVI_INTRODUCE_VALUE),
                    Mbti.ISTP
            ));
        }
    }

    @Nested
    @DisplayName("내가 개설한 Crew 프로젝션 조회를 테스트한다.")
    class FetchCrewsByMemberId {

        @Test
        @DisplayName("내가 개설한 Crew 프로젝션 조회에 성공한다.")
        void success() {
            Member ANDONG = memberJpaRepository.save(ANDONG());
            Member KWANGWON = memberJpaRepository.save(KWANGWON());
            crewJpaRepository.save(CREW_1(ANDONG));
            crewJpaRepository.save(CREW_2(ANDONG));
            Crew CREW3 = crewJpaRepository.save(CREW_3(KWANGWON));
            Crew CREW4 = crewJpaRepository.save(CREW_4(KWANGWON));
            PageRequest PAGE_REQUEST = PageRequest.of(0, 2);

            Page<CrewDomainDto> DTOS = crewQueryDslRepository.fetchCrewsByMemberId(KWANGWON.getId(), PAGE_REQUEST);

            assertThat(DTOS).hasSize(2);
            assertThat(DTOS.getContent().get(0).getId()).isEqualTo(CREW4.getId());
            assertThat(DTOS.getContent().get(0).getCrewOwner().id()).isEqualTo(KWANGWON.getId());
            assertThat(DTOS.getContent().get(1).getId()).isEqualTo(CREW3.getId());
            assertThat(DTOS.getContent().get(1).getCrewOwner().id()).isEqualTo(KWANGWON.getId());
        }
    }

    @Nested
    @DisplayName("내가 참여한 Crew 프로젝션 조회를 테스트한다.")
    class FetchAllJoinedCrewsByMemberId {

        @Test
        @DisplayName("내가 참여한 Crew 프로젝션 조회를 성공한다.")
        void success() {
            Member ANDONG = memberJpaRepository.save(ANDONG());
            Member KWANGWON = memberJpaRepository.save(KWANGWON());
            Member REVI = memberJpaRepository.save(REVI());
            Crew CREW1 = crewJpaRepository.save(CREW_1(ANDONG));
            Crew CREW2 = crewJpaRepository.save(CREW_2(KWANGWON));
            Crew CREW3 = crewJpaRepository.save(CREW_3(REVI));
            LocalDateTime NOW = LocalDateTime.now();
            crewMemberJpaRepository.save(CrewMember.forGeneral(CREW1, REVI, NOW));
            crewMemberJpaRepository.save(CrewMember.forGeneral(CREW2, REVI, NOW.plusMinutes(1)));
            crewMemberJpaRepository.save(CrewMember.forGeneral(CREW2, ANDONG, NOW.plusMinutes(1)));

            List<EnrolledCrewDomainDto> DTOS = crewQueryDslRepository.fetchEnrolledCrewsByMemberId(REVI.getId());

            assertThat(DTOS).hasSize(3);

            assertThat(DTOS.get(0).id()).isEqualTo(CREW2.getId());
            assertThat(DTOS.get(0).name()).isEqualTo(CREW2.getName());
            assertThat(DTOS.get(0).imageUrl()).isNull();
            assertThat(DTOS.get(0).isOwner()).isFalse();
            assertThat(DTOS.get(0).owner().id()).isEqualTo(KWANGWON.getId());
            assertThat(DTOS.get(0).owner().nickname()).isEqualTo(KWANGWON_NICKNAME);
            assertThat(DTOS.get(0).owner().mbti()).isSameAs(KWANGWON_MBTI);

            assertThat(DTOS.get(1).id()).isEqualTo(CREW1.getId());
            assertThat(DTOS.get(1).name()).isEqualTo(CREW1.getName());
            assertThat(DTOS.get(1).imageUrl()).isNull();
            assertThat(DTOS.get(1).isOwner()).isFalse();
            assertThat(DTOS.get(1).owner().id()).isEqualTo(ANDONG.getId());
            assertThat(DTOS.get(1).owner().nickname()).isEqualTo(ANDONG_NICKNAME);
            assertThat(DTOS.get(1).owner().mbti()).isSameAs(ANDONG_MBTI);

            assertThat(DTOS.get(2).id()).isEqualTo(CREW3.getId());
            assertThat(DTOS.get(2).name()).isEqualTo(CREW3.getName());
            assertThat(DTOS.get(2).imageUrl()).isNull();
            assertThat(DTOS.get(2).isOwner()).isTrue();
            assertThat(DTOS.get(2).owner().id()).isEqualTo(REVI.getId());
            assertThat(DTOS.get(2).owner().nickname()).isEqualTo(REVI_NICKNAME);
            assertThat(DTOS.get(2).owner().mbti()).isSameAs(REVI_MBTI);
        }
    }
}