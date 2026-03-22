package revi1337.onsquad.crew_hashtag.domain;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.CrewHashtagFixture.createCrewHashtag;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.crew_hashtag.domain.entity.CrewHashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;

class CrewHashtagsTest {

    @Test
    @DisplayName("복수 크루의 해시태그 목록을 크루 ID 기준으로 그룹화하여 타입별로 분류한다")
    void groupByCrewId() {
        CrewHashtag ht1 = createCrewHashtag(createCrew(1L, createRevi(1L)), HashtagType.ACTIVE);
        CrewHashtag ht2 = createCrewHashtag(createCrew(1L, createRevi(1L)), HashtagType.TRENDY);
        CrewHashtag ht3 = createCrewHashtag(createCrew(1L, createRevi(1L)), HashtagType.INTROVERTED);
        CrewHashtag ht4 = createCrewHashtag(createCrew(2L, createRevi(1L)), HashtagType.EXTROVERTED);
        CrewHashtag ht5 = createCrewHashtag(createCrew(2L, createRevi(1L)), HashtagType.QUIET);
        CrewHashtag ht6 = createCrewHashtag(createCrew(2L, createRevi(1L)), HashtagType.GAME_LOVER_MALE);
        CrewHashtag ht7 = createCrewHashtag(createCrew(3L, createRevi(1L)), HashtagType.GAME_LOVER_FEMALE);
        CrewHashtag ht8 = createCrewHashtag(createCrew(3L, createRevi(1L)), HashtagType.DRINK_LOVER);
        CrewHashtag ht9 = createCrewHashtag(createCrew(3L, createRevi(1L)), HashtagType.THRILLING);
        CrewHashtags hashtags = new CrewHashtags(List.of(ht1, ht2, ht3, ht4, ht5, ht6, ht7, ht8, ht9));

        Map<Long, List<HashtagType>> groupedByCrew = hashtags.groupByCrewId();

        assertSoftly(softly -> {
            softly.assertThat(groupedByCrew).hasSize(3);
            softly.assertThat(groupedByCrew.keySet()).containsExactlyInAnyOrder(1L, 2L, 3L);
            softly.assertThat(groupedByCrew.get(1L)).hasSize(3);
            softly.assertThat(groupedByCrew.get(2L)).hasSize(3);
            softly.assertThat(groupedByCrew.get(3L)).hasSize(3);
        });
    }
}
