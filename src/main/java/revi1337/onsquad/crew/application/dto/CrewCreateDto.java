package revi1337.onsquad.crew.application.dto;

import java.util.List;
import revi1337.onsquad.crew.domain.model.CrewCreateSpec;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.domain.entity.Member;

public record CrewCreateDto(
        String name,
        String introduce,
        String detail,
        List<HashtagType> hashtags,
        String kakaoLink
) {

    public CrewCreateSpec toSpec(Member owner, String imageUrl) {
        return new CrewCreateSpec(
                owner,
                name,
                introduce,
                detail,
                hashtags,
                kakaoLink,
                imageUrl
        );
    }
}
