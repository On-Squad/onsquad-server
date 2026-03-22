package revi1337.onsquad.crew.domain.repository;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createAndong;
import static revi1337.onsquad.common.fixture.MemberFixture.createKwangwon;
import static revi1337.onsquad.common.fixture.MemberFixture.createMember;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.common.PersistenceLayerTestSupport;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.model.CrewDetail;
import revi1337.onsquad.crew.domain.model.CrewStatistic;
import revi1337.onsquad.crew.domain.model.CrewWithOwnerState;
import revi1337.onsquad.crew_hashtag.domain.repository.CrewHashtagJdbcRepository;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.entity.CrewMemberFactory;
import revi1337.onsquad.crew_request.domain.entity.CrewRequest;
import revi1337.onsquad.crew_request.domain.repository.CrewRequestJpaRepository;
import revi1337.onsquad.hashtag.domain.entity.Hashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad.domain.model.SquadCreateSpec;
import revi1337.onsquad.squad.domain.repository.SquadJpaRepository;

@Sql({"/h2-hashtag.sql"})
@Import({CrewQueryDslRepository.class, CrewHashtagJdbcRepository.class})
class CrewQueryDslRepositoryTest extends PersistenceLayerTestSupport {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private CrewJpaRepository crewJpaRepository;

    @Autowired
    private CrewRequestJpaRepository crewRequestJpaRepository;

    @Autowired
    private SquadJpaRepository squadJpaRepository;

    @Autowired
    private CrewHashtagJdbcRepository crewHashtagRepository;

    @Autowired
    private CrewQueryDslRepository crewQueryDslRepository;

    @Test
    @DisplayName("크루 ID로 해시태그를 포함한 크루 상세 정보를 조회한다.")
    void fetchCrewWithDetailById() {
        Member member = memberJpaRepository.save(createRevi());
        Crew crew = crewJpaRepository.save(createCrew(member));
        crewHashtagRepository.insertBatch(crew.getId(), Hashtag.fromHashtagTypes(List.of(HashtagType.ACTIVE, HashtagType.FOODIE)));
        clearPersistenceContext();

        Optional<CrewDetail> resultOpt = crewQueryDslRepository.fetchCrewWithDetailById(crew.getId());

        assertSoftly(softly -> {
            softly.assertThat(resultOpt).isPresent();
            CrewDetail result = resultOpt.get();
            softly.assertThat(result.getId()).isEqualTo(crew.getId());
            softly.assertThat(result.getName()).isEqualTo(crew.getName());
            softly.assertThat(result.getIntroduce()).isEqualTo(crew.getIntroduce());
            softly.assertThat(result.getDetail()).isEqualTo(crew.getDetail());
            softly.assertThat(result.getImageUrl()).isEqualTo(crew.getImageUrl());
            softly.assertThat(result.getKakaoLink()).isEqualTo(crew.getKakaoLink());
            softly.assertThat(result.getHashtagTypes()).hasSize(2);
            softly.assertThat(result.getMemberCnt()).isEqualTo(crew.getCurrentSize());
            softly.assertThat(result.getCrewOwner().id()).isEqualTo(member.getId());
        });
    }

    @Test
    @DisplayName("해시태그가 없는 크루를 상세 조회할 경우 빈 리스트를 반환하며 정상 조회된다.")
    void fetchCrewWithDetailById2() {
        Member member = memberJpaRepository.save(createRevi());
        Crew crew = crewJpaRepository.save(createCrew(member));
        clearPersistenceContext();

        Optional<CrewDetail> resultOpt = crewQueryDslRepository.fetchCrewWithDetailById(crew.getId());

        assertSoftly(softly -> {
            softly.assertThat(resultOpt).isPresent();
            CrewDetail result = resultOpt.get();
            softly.assertThat(result.getId()).isEqualTo(crew.getId());
            softly.assertThat(result.getName()).isEqualTo(crew.getName());
            softly.assertThat(result.getIntroduce()).isEqualTo(crew.getIntroduce());
            softly.assertThat(result.getDetail()).isEqualTo(crew.getDetail());
            softly.assertThat(result.getImageUrl()).isEqualTo(crew.getImageUrl());
            softly.assertThat(result.getKakaoLink()).isEqualTo(crew.getKakaoLink());
            softly.assertThat(result.getHashtagTypes()).hasSize(0);
            softly.assertThat(result.getMemberCnt()).isEqualTo(crew.getCurrentSize());
            softly.assertThat(result.getCrewOwner().id()).isEqualTo(member.getId());
        });
    }

    @Test
    @DisplayName("크루 이름 검색 시 페이징이 적용된 크루 목록을 최신순으로 반환한다.")
    void fetchCrewsWithDetailByName() {
        Member member = memberJpaRepository.save(createRevi());
        crewJpaRepository.saveAll(List.of(
                createCrew("crew-1", member),
                createCrew("crew-2", member),
                createCrew("crew-3", member),
                createCrew("crew-4", member)
        ));
        PageRequest pageRequest = PageRequest.of(0, 2);

        Page<CrewDetail> results = crewQueryDslRepository.fetchCrewsWithDetailByName("crew", pageRequest);

        assertSoftly(softly -> {
            softly.assertThat(results).hasSize(2);
            List<CrewDetail> content = results.getContent();
            softly.assertThat(content.get(0).getName().getValue()).isEqualTo("crew-4");
            softly.assertThat(content.get(1).getName().getValue()).isEqualTo("crew-3");
        });
    }

    @Test
    @DisplayName("입력받은 크루 ID 리스트에 대해 현재 접속자의 방장 여부(Owner State)를 포함하여 조회한다.")
    void fetchCrewsWithStateByIdIn() {
        Member andong = memberJpaRepository.save(createAndong());
        Member revi = memberJpaRepository.save(createRevi());
        Crew crew1 = crewJpaRepository.save(createCrew("crew-2", andong));
        Crew crew2 = crewJpaRepository.save(createCrew("crew-1", revi));

        List<CrewWithOwnerState> results = crewQueryDslRepository.fetchCrewsWithStateByIdIn(List.of(crew1.getId(), crew2.getId()), revi.getId());

        assertSoftly(softly -> {
            softly.assertThat(results).hasSize(2);
            softly.assertThat(results.get(0).crew().id()).isEqualTo(crew2.getId());
            softly.assertThat(results.get(0).isOwner()).isTrue();
            softly.assertThat(results.get(1).crew().id()).isEqualTo(crew1.getId());
            softly.assertThat(results.get(1).isOwner()).isFalse();
        });
    }

    @Test
    @DisplayName("특정 크루의 가입 신청 수, 활성화된 스쿼드 수, 현재 인원 수 통계를 조회한다.")
    void getStatisticById() {
        Member revi = memberJpaRepository.save(createRevi());
        Member andong = memberJpaRepository.save(createAndong());
        Member kwangwon = memberJpaRepository.save(createKwangwon());
        Member member = memberJpaRepository.save(createMember(1));
        Crew crew = createCrew(revi);
        crew.addCrewMember(createManagerCrewMember(crew, andong));
        crew.addCrewMember(createManagerCrewMember(crew, kwangwon));
        crewJpaRepository.save(crew);
        crewRequestJpaRepository.save(createCrewRequest(crew, member));
        squadJpaRepository.saveAll(List.of(createSquad(crew, andong), createSquad(crew, kwangwon)));
        clearPersistenceContext();

        CrewStatistic statistic = crewQueryDslRepository.getStatisticById(crew.getId());

        assertSoftly(softly -> {
            softly.assertThat(statistic.requestCnt()).isEqualTo(1);
            softly.assertThat(statistic.squadCnt()).isEqualTo(2);
            softly.assertThat(statistic.memberCnt()).isEqualTo(3);
        });
    }

    private CrewRequest createCrewRequest(Crew crew, Member andong) {
        return CrewRequest.of(crew, andong, LocalDateTime.now());
    }

    private CrewMember createManagerCrewMember(Crew crew, Member member) {
        return CrewMemberFactory.manager(crew, member, LocalDateTime.now());
    }

    private Squad createSquad(Crew crew, Member member) {
        return Squad.create(
                new SquadCreateSpec(
                        "title",
                        "content",
                        20,
                        "어딘가",
                        "상세-어딘가",
                        List.of(),
                        "kakao-link",
                        "discord-link"
                ),
                crew,
                member,
                LocalDateTime.now()
        );
    }
}
