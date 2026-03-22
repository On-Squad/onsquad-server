package revi1337.onsquad.member.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserType {

    GENERAL("일반"),
    KAKAO("카카오"),
    GOOGLE("구글");

    private final String text;

}
