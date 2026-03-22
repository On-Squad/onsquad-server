package revi1337.onsquad.crew.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import javax.annotation.Nullable;
import revi1337.onsquad.crew.domain.model.CrewDetail;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CrewWithParticipantStateResponse(
        CrewStates states,
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

    public static CrewWithParticipantStateResponse from(@Nullable Boolean alreadyRequest, @Nullable Boolean alreadyParticipant, CrewDetail result) {
        return new CrewWithParticipantStateResponse(
                new CrewStates(alreadyRequest, alreadyParticipant),
                result.getId(),
                result.getName().getValue(),
                result.getIntroduce().getValue(),
                result.getDetail() != null ? result.getDetail().getValue() : null,
                result.getImageUrl() != null ? result.getImageUrl() : "",
                result.getKakaoLink(),
                result.getHashtagTypes().stream()
                        .map(HashtagType::getText)
                        .toList(),
                result.getMemberCnt(),
                SimpleMemberResponse.from(result.getCrewOwner())
        );
    }
}
