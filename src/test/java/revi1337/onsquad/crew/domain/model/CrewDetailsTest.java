package revi1337.onsquad.crew.domain.model;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.crew.domain.vo.Introduce;
import revi1337.onsquad.crew.domain.vo.Name;
import revi1337.onsquad.crew_hashtag.domain.CrewHashtags;
import revi1337.onsquad.crew_hashtag.domain.entity.CrewHashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.hashtag.domain.entity.Hashtag;
import revi1337.onsquad.member.domain.model.SimpleMember;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.member.domain.vo.Nickname;

class CrewDetailsTest {

    @Test
    @DisplayName("크루 상세 목록에서 각 크루의 ID 리스트만 지성 있게 추출한다.")
    void getIds() {
        CrewDetail detail1 = createCrewDetail(1L);
        CrewDetail detail2 = createCrewDetail(2L);
        CrewDetails crewDetails = new CrewDetails(List.of(detail1, detail2));

        List<Long> ids = crewDetails.getIds();

        assertThat(ids).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("외부에서 전달된 해시태그 뭉치를 각 크루 ID에 맞게 매핑한다.")
    void linkHashtags() {
        CrewDetail detail1 = createCrewDetail(1L);
        CrewDetail detail2 = createCrewDetail(2L);
        CrewDetails crewDetails = new CrewDetails(List.of(detail1, detail2));
        CrewHashtags hashtags = new CrewHashtags(List.of(
                createCrewHashtag(1L, HashtagType.THRILLING),
                createCrewHashtag(1L, HashtagType.QUIET),
                createCrewHashtag(2L, HashtagType.TRAVEL),
                createCrewHashtag(2L, HashtagType.ACTIVE),
                createCrewHashtag(2L, HashtagType.EXTROVERTED)
        ));

        crewDetails.linkHashtags(hashtags);

        assertSoftly(softly -> {
            List<CrewDetail> values = crewDetails.values();
            softly.assertThat(values).hasSize(2);
            softly.assertThat(values.get(0).getHashtagTypes()).hasSize(2);
            softly.assertThat(values.get(1).getHashtagTypes()).hasSize(3);
        });

    }

    @Test
    @DisplayName("제공된 컨버터를 사용하여 CrewDetail 리스트를 다른 객체 리스트로 변환한다.")
    void map() {
        CrewDetails crewDetails = new CrewDetails(List.of(createCrewDetail(1L), createCrewDetail(2L)));

        List<String> names = crewDetails.map(detail -> "Crew-" + detail.getId());

        assertThat(names).containsExactly("Crew-1", "Crew-2");
    }

    private CrewDetail createCrewDetail(Long id) {
        return new CrewDetail(
                id,
                new Name("크루-" + id),
                new Introduce("소개"),
                "image-url",
                "kakao-link",
                20L,
                new SimpleMember(1L, new Nickname("방장"), new revi1337.onsquad.member.domain.vo.Introduce("안녕"), Mbti.ENTJ)
        );
    }

    public CrewHashtag createCrewHashtag(Long crewId, HashtagType hashtagType) {
        return new CrewHashtag(createCrew(crewId, createRevi()), Hashtag.fromHashtagType(hashtagType));
    }
}
