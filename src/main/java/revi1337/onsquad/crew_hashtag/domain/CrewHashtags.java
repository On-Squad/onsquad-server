package revi1337.onsquad.crew_hashtag.domain;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import revi1337.onsquad.crew_hashtag.domain.entity.CrewHashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;

public final class CrewHashtags {

    private final List<CrewHashtag> hashtags;

    public CrewHashtags(List<CrewHashtag> hashtags) {
        this.hashtags = Collections.unmodifiableList(hashtags);
    }

    public Map<Long, List<HashtagType>> groupByCrewId() {
        return hashtags.stream()
                .collect(Collectors.groupingBy(
                        ch -> ch.getCrew().getId(),
                        Collectors.mapping(
                                ch -> ch.getHashtag().getHashtagType(),
                                Collectors.toList()
                        )
                ));
    }
}
