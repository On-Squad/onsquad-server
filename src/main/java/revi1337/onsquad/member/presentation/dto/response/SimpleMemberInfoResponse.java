package revi1337.onsquad.member.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import revi1337.onsquad.member.application.dto.SimpleMemberInfoDto;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimpleMemberInfoResponse(
        Long id,
        String email,
        String nickname,
        String mbti
) {
    public static SimpleMemberInfoResponse from(SimpleMemberInfoDto simpleMemberInfoDto) {
        return new SimpleMemberInfoResponse(
                simpleMemberInfoDto.id(),
                simpleMemberInfoDto.email(),
                simpleMemberInfoDto.nickname(),
                simpleMemberInfoDto.mbti()
        );
    }
}
