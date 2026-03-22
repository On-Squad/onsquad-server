package revi1337.onsquad.common.fixture;

import java.util.Arrays;
import java.util.List;
import revi1337.onsquad.hashtag.domain.entity.Hashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;

public class HashtagFixture {

    public static Hashtag createHashtag(HashtagType hashtagType) {
        return Hashtag.fromHashtagType(hashtagType);
    }

    public static List<Hashtag> createHashtags(HashtagType... hashtagTypes) {
        return Hashtag.fromHashtagTypes(Arrays.asList(hashtagTypes));
    }
}
