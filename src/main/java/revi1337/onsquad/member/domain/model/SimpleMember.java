package revi1337.onsquad.member.domain.model;

import revi1337.onsquad.member.domain.vo.Email;
import revi1337.onsquad.member.domain.vo.Introduce;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.member.domain.vo.Nickname;

public record SimpleMember(
        Long id,
        Email email,
        Nickname nickname,
        Introduce introduce,
        Mbti mbti
) {

    public SimpleMember(Long id, Nickname nickname, Introduce introduce, Mbti mbti) {
        this(id, null, nickname, introduce, mbti);
    }
}
