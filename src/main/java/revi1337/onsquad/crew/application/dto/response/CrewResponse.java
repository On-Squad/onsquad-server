package revi1337.onsquad.crew.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.model.CrewDetail;
import revi1337.onsquad.crew_hashtag.domain.entity.CrewHashtag;
import revi1337.onsquad.hashtag.domain.entity.Hashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CrewResponse(
        Long id,
        String name,
        String introduce,
        String detail,
        String imageUrl,
        String kakaoLink,
        List<String> hashtags,
        Long memberCount,
        SimpleMemberResponse owner
) {

    public static CrewResponse from(CrewDetail crewDetail) {
        return new CrewResponse(
                crewDetail.getId(),
                crewDetail.getName().getValue(),
                crewDetail.getIntroduce().getValue(),
                crewDetail.getDetail() != null ? crewDetail.getDetail().getValue() : null,
                crewDetail.getImageUrl() != null ? crewDetail.getImageUrl() : "",
                crewDetail.getKakaoLink(),
                crewDetail.getHashtagTypes().stream()
                        .map(HashtagType::getText)
                        .toList(),
                crewDetail.getMemberCnt(),
                SimpleMemberResponse.from(crewDetail.getCrewOwner())
        );
    }

    public static CrewResponse from(Crew crew) {
        return new CrewResponse(
                crew.getId(),
                crew.getName().getValue(),
                crew.getIntroduce().getValue(),
                crew.getDetail() != null ? crew.getDetail().getValue() : null,
                crew.getImageUrl() != null ? crew.getImageUrl() : "",
                crew.getKakaoLink(),
                crew.getHashtags().stream()
                        .map(CrewHashtag::getHashtag)
                        .map(Hashtag::getHashtagType)
                        .map(HashtagType::getText)
                        .toList(),
                crew.getCurrentSize(),
                SimpleMemberResponse.from(crew.getMember())
        );
    }
}
