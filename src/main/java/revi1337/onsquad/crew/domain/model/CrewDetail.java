package revi1337.onsquad.crew.domain.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import revi1337.onsquad.crew.domain.vo.Detail;
import revi1337.onsquad.crew.domain.vo.Introduce;
import revi1337.onsquad.crew.domain.vo.Name;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.domain.model.SimpleMember;

@Getter
@AllArgsConstructor
public class CrewDetail {

    private Long id;
    private Name name;
    private Introduce introduce;
    private Detail detail;
    private String imageUrl;
    private String kakaoLink;
    private Collection<HashtagType> hashtagTypes;
    private Long memberCnt;
    private SimpleMember crewOwner;

    public CrewDetail(
            Long id,
            Name name,
            Introduce introduce,
            String imageUrl,
            String kakaoLink,
            Collection<HashtagType> hashtagTypes,
            Long memberCnt,
            SimpleMember crewOwner
    ) {
        this(id, name, introduce, null, imageUrl, kakaoLink, hashtagTypes, memberCnt, crewOwner);
    }

    public CrewDetail(
            Long id,
            Name name,
            Introduce introduce,
            String imageUrl,
            String kakaoLink,
            Long memberCnt,
            SimpleMember crewOwner
    ) {
        this(id, name, introduce, null, imageUrl, kakaoLink, new ArrayList<>(), memberCnt, crewOwner);
    }

    public void addHashtagTypes(List<HashtagType> hashtagTypes) {
        this.hashtagTypes.addAll(hashtagTypes);
    }
}
