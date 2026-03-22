package revi1337.onsquad.common.fixture;

import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew_hashtag.domain.entity.CrewHashtag;
import revi1337.onsquad.hashtag.domain.entity.Hashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;

public class CrewHashtagFixture {

    public static CrewHashtag createCrewHashtag(Crew crew, HashtagType hashtagType) {
        return new CrewHashtag(crew, Hashtag.fromHashtagType(hashtagType));
    }
}
