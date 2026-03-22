package revi1337.onsquad.crew_member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrewRole {

    OWNER("크루장"),
    MANAGER("매니저"),
    GENERAL("일반");

    private final String text;

}
