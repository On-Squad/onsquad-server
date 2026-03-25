package revi1337.onsquad.crew_member.domain.model;

import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.member.domain.vo.Nickname;

public class RankerProfile {

    private final Nickname nickname;
    private final Mbti mbti;

    public RankerProfile(Nickname nickname, Mbti mbti) {
        this.nickname = nickname;
        this.mbti = mbti;
    }

    public RankerProfile(Nickname nickname, String mbti) {
        this.nickname = nickname;
        this.mbti = mbti != null ? Mbti.parse(mbti) : null;
    }

    public String getNickname() {
        return nickname.getValue();
    }

    public String getMbtiOrDefault() {
        return mbti == null ? "" : mbti.name();
    }
}
