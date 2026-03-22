package revi1337.onsquad.crew.application;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createAndong;
import static revi1337.onsquad.common.fixture.MemberFixture.createKwangwon;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.announce.domain.entity.Announce;
import revi1337.onsquad.announce.domain.repository.AnnounceJpaRepository;
import revi1337.onsquad.common.ApplicationLayerTestSupport;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.crew_hashtag.domain.repository.CrewHashtagJdbcRepository;
import revi1337.onsquad.crew_hashtag.domain.repository.CrewHashtagJpaRepository;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.entity.CrewMemberFactory;
import revi1337.onsquad.crew_member.domain.repository.CrewMemberJpaRepository;
import revi1337.onsquad.crew_request.domain.entity.CrewRequest;
import revi1337.onsquad.crew_request.domain.repository.CrewRequestJpaRepository;
import revi1337.onsquad.hashtag.domain.entity.Hashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;

@Sql({"/h2-hashtag.sql"})
class CrewContextDisposerTest extends ApplicationLayerTestSupport {

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private CrewJpaRepository crewRepository;

    @Autowired
    private CrewHashtagJdbcRepository crewHashtagJdbcRepository;

    @Autowired
    private CrewHashtagJpaRepository crewHashtagRepository;

    @Autowired
    private CrewRequestJpaRepository crewRequestRepository;

    @Autowired
    private CrewMemberJpaRepository crewMemberRepository;

    @Autowired
    private AnnounceJpaRepository announceRepository;

    @Autowired
    private CrewContextDisposer contextDisposer;

    @Test
    @DisplayName("특정 크루 ID를 전달하면 해당 크루와 관련된 모든 연관 데이터가 삭제된다")
    void disposeContext() {
        // given
        Member revi = createRevi();
        Member andong = createAndong();
        Member kwangwon = createKwangwon();

        memberRepository.saveAll(List.of(revi, andong, kwangwon));
        Crew crew = crewRepository.save(createCrew(revi));
        crewHashtagJdbcRepository.insertBatch(crew.getId(), createHashtags(HashtagType.ACTIVE, HashtagType.PASSIONATE));
        crewRequestRepository.saveAll(List.of(createCrewRequest(crew, andong), createCrewRequest(crew, kwangwon)));
        announceRepository.save(createCrewAnnounce(crew, revi));

        // when
        contextDisposer.disposeContext(crew.getId());

        // then
        assertSoftly(softly -> {
            clearPersistenceContext();
            softly.assertThat(crewHashtagRepository.findAll().size()).isZero();
            softly.assertThat(crewRequestRepository.findAll().size()).isZero();
            softly.assertThat(crewMemberRepository.findAll().size()).isZero();
            softly.assertThat(announceRepository.findAll().size()).isZero();
            softly.assertThat(crewRepository.findAll().size()).isZero();
        });
    }

    @Test
    @DisplayName("여러 개의 크루 ID 리스트를 전달하면 해당 크루들의 모든 데이터가 일괄 삭제된다")
    void disposeContexts() {
        // given
        Member revi = createRevi();
        Member andong = createAndong();
        Member kwangwon = createKwangwon();
        memberRepository.saveAll(List.of(revi, andong, kwangwon));

        Crew crew1 = crewRepository.save(createCrew(revi));
        crewHashtagJdbcRepository.insertBatch(crew1.getId(), createHashtags(HashtagType.ACTIVE, HashtagType.PASSIONATE));
        crewRequestRepository.saveAll(List.of(createCrewRequest(crew1, andong), createCrewRequest(crew1, kwangwon)));
        announceRepository.save(createCrewAnnounce(crew1, revi));

        Crew crew2 = crewRepository.save(createCrew(andong));
        crewHashtagJdbcRepository.insertBatch(crew2.getId(), createHashtags(HashtagType.CAFE_LOVER, HashtagType.GAME_LOVER_FEMALE));
        crewRequestRepository.saveAll(List.of(createCrewRequest(crew2, revi), createCrewRequest(crew2, kwangwon)));
        announceRepository.save(createCrewAnnounce(crew2, andong));

        // when
        contextDisposer.disposeContexts(List.of(crew1.getId(), crew2.getId()));

        // then
        assertSoftly(softly -> {
            clearPersistenceContext();
            softly.assertThat(crewHashtagRepository.findAll().size()).isZero();
            softly.assertThat(crewRequestRepository.findAll().size()).isZero();
            softly.assertThat(crewMemberRepository.findAll().size()).isZero();
            softly.assertThat(announceRepository.findAll().size()).isZero();
            softly.assertThat(crewRepository.findAll().size()).isZero();
        });
    }

    private List<Hashtag> createHashtags(HashtagType... hashtagTypes) {
        return Hashtag.fromHashtagTypes(Arrays.asList(hashtagTypes));
    }

    private CrewRequest createCrewRequest(Crew crew, Member andong) {
        return CrewRequest.of(crew, andong, LocalDateTime.now());
    }

    private CrewMember createManagerCrewMember(Crew crew, Member member) {
        return CrewMemberFactory.manager(crew, member, LocalDateTime.now());
    }

    private Announce createCrewAnnounce(Crew crew, Member revi) {
        String uuid = UUID.randomUUID().toString().substring(0, 10);
        return new Announce(uuid, uuid, crew, revi);
    }
}
