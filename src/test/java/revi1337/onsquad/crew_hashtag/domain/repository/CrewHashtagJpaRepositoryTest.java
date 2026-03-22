package revi1337.onsquad.crew_hashtag.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.CrewHashtagFixture.createCrewHashtag;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.common.PersistenceLayerTestSupport;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.crew_hashtag.domain.entity.CrewHashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;

@Sql({"/h2-hashtag.sql"})
class CrewHashtagJpaRepositoryTest extends PersistenceLayerTestSupport {

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private CrewJpaRepository crewRepository;

    @Autowired
    private CrewHashtagJpaRepository crewHashtagJpaRepository;

    @Test
    @DisplayName("복수 크루 ID에 해당하는 해시태그 목록을 패치 조인으로 조회한다")
    void fetchHashtagsByCrewIdIn() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        List<CrewHashtag> crewHashtags = List.of(createCrewHashtag(crew, HashtagType.ACTIVE), createCrewHashtag(crew, HashtagType.PASSIONATE));
        crewHashtagJpaRepository.saveAll(crewHashtags);
        clearPersistenceContext();

        List<CrewHashtag> fetchedHashtags = crewHashtagJpaRepository.fetchHashtagsByCrewIdIn(List.of(crew.getId()));

        clearPersistenceContext();
        assertThat(fetchedHashtags).hasSize(crewHashtags.size());
    }

    @Test
    @DisplayName("복수 크루 ID에 속한 해시태그 연관 관계 데이터를 벌크 삭제한다")
    void deleteByCrewIdIn() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        List<CrewHashtag> crewHashtags = List.of(createCrewHashtag(crew, HashtagType.ACTIVE), createCrewHashtag(crew, HashtagType.PASSIONATE));
        crewHashtagJpaRepository.saveAll(crewHashtags);
        clearPersistenceContext();

        int deleted = crewHashtagJpaRepository.deleteByCrewIdIn(List.of(crew.getId()));

        clearPersistenceContext();
        assertThat(deleted).isEqualTo(crewHashtags.size());
    }
}
