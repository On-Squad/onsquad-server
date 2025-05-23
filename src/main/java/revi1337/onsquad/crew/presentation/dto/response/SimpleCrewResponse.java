package revi1337.onsquad.crew.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import revi1337.onsquad.crew.application.dto.SimpleCrewDto;
import revi1337.onsquad.member.presentation.dto.response.SimpleMemberInfoResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimpleCrewResponse(
        Long id,
        String name,
        String introduce,
        String kakaoLink,
        String imageUrl,
        SimpleMemberInfoResponse owner
) {
    public static SimpleCrewResponse from(SimpleCrewDto simpleCrewDto) {
        return new SimpleCrewResponse(
                simpleCrewDto.id(),
                simpleCrewDto.name(),
                simpleCrewDto.introduce(),
                simpleCrewDto.kakaoLink(),
                simpleCrewDto.imageUrl(),
                SimpleMemberInfoResponse.from(simpleCrewDto.owner())
        );
    }
}
