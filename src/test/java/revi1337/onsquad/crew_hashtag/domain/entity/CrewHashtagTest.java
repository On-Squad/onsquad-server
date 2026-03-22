package revi1337.onsquad.crew_hashtag.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.HashtagFixture.createHashtag;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.hashtag.domain.entity.Hashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;

class CrewHashtagTest {

    @Test
    @DisplayName("크루와 해시태그 정보를 바탕으로 연관 관계 엔티티를 생성한다")
    void constructor() {
        Crew crew = createCrew(2L, createRevi(1L));
        Hashtag hashtag = createHashtag(HashtagType.ACTIVE);

        CrewHashtag crewHashtag = new CrewHashtag(crew, hashtag);

        assertThat(crewHashtag.getCrew().getId()).isEqualTo(crew.getId());
        assertThat(crewHashtag.getHashtag().getId()).isEqualTo(hashtag.getId());
    }
}
