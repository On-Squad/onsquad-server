package revi1337.onsquad.member.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.model.SimpleMember;
import revi1337.onsquad.member.domain.vo.Introduce;
import revi1337.onsquad.member.domain.vo.Mbti;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimpleMemberResponse(
        Long id,
        String email,
        String nickname,
        String introduce,
        String mbti
) {

    public static final SimpleMemberResponse DELETED_MEMBER = new SimpleMemberResponse(null, null, "탈퇴한 회원입니다.", null, null);

    public static SimpleMemberResponse from(Member member) {
        return new SimpleMemberResponse(
                member.getId(),
                null,
                member.getNickname().getValue(),
                Introduce.getOrDefault(member.getIntroduce()),
                Mbti.getOrDefault(member.getMbti())
        );
    }

    public static SimpleMemberResponse from(SimpleMember member) {
        return new SimpleMemberResponse(
                member.id(),
                null,
                member.nickname().getValue(),
                Introduce.getOrDefault(member.introduce()),
                Mbti.getOrDefault(member.mbti())
        );
    }
}
