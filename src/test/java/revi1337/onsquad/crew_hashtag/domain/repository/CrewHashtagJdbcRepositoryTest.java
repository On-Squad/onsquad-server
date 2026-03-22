package revi1337.onsquad.crew_hashtag.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.HashtagFixture.createHashtags;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.common.PersistenceLayerTestSupport;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.hashtag.domain.entity.Hashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;

@Sql({"/h2-hashtag.sql"})
@Import(CrewHashtagJdbcRepository.class)
class CrewHashtagJdbcRepositoryTest extends PersistenceLayerTestSupport {

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private CrewJpaRepository crewRepository;

    @Autowired
    private CrewHashtagJpaRepository crewHashtagJpaRepository;

    @Autowired
    private CrewHashtagJdbcRepository crewHashtagJdbcRepository;

    @Test
    @DisplayName("JdbcTemplate의 batchUpdate를 이용해 복수 개의 해시태그 연관 관계를 일괄 삽입한다")
    void insertBatch() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        List<Hashtag> hashtags = createHashtags(HashtagType.ACTIVE, HashtagType.PASSIONATE);
        clearPersistenceContext();

        crewHashtagJdbcRepository.insertBatch(crew.getId(), hashtags);

        clearPersistenceContext();
        assertThat(crewHashtagJpaRepository.findAll()).hasSize(hashtags.size());
    }
}
