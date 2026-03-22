package revi1337.onsquad.crew.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import revi1337.onsquad.crew_hashtag.domain.CrewHashtags;
import revi1337.onsquad.hashtag.domain.HashtagType;

public class CrewDetails {

    private final List<CrewDetail> results;

    public CrewDetails(List<CrewDetail> results) {
        this.results = Collections.unmodifiableList(results);
    }

    public List<Long> getIds() {
        return results.stream()
                .map(CrewDetail::getId)
                .toList();
    }

    public List<CrewDetail> values() {
        return results;
    }

    public boolean isNotEmpty() {
        return !results.isEmpty();
    }

    public void linkHashtags(CrewHashtags hashtags) {
        Map<Long, List<HashtagType>> hashtagMap = hashtags.groupByCrewId();
        results.forEach(result -> {
            List<HashtagType> hashtagTypes = hashtagMap.get(result.getId());
            if (hashtagTypes != null) {
                result.addHashtagTypes(hashtagTypes);
            }
        });
    }

    public <U> List<U> map(Function<CrewDetail, U> converter) {
        return results.stream()
                .map(converter)
                .collect(Collectors.toList());
    }
}
