package revi1337.onsquad.crew_member.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;

@Getter
@RequiredArgsConstructor
public enum JoinStatus {

    PENDING("보류"),
    ACCEPT("수락"),
    REJECT("거절");

    private final String text;

    public static boolean checkEquivalence(JoinStatus status, String constant) {
        return status.getText().equals(constant);
    }

    public static EnumSet<JoinStatus> defaultEnumSet() {
        return EnumSet.allOf(JoinStatus.class);
    }
}
