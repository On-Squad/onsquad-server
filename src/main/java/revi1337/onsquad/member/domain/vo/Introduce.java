package revi1337.onsquad.member.domain.vo;

import static lombok.AccessLevel.PROTECTED;
import static revi1337.onsquad.member.domain.error.MemberErrorCode.INVALID_INTRODUCE_LENGTH;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.member.domain.error.MemberDomainException;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@Embeddable
public class Introduce {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 200;
    private static final int PERSIST_MAX_LENGTH = MAX_LENGTH * 3;

    @Column(name = "introduce", length = PERSIST_MAX_LENGTH)
    private String value;

    public Introduce(String value) {
        validateNull(value);
        validateSize(value);
        this.value = value;
    }

    private void validateNull(String value) {
        if (value == null) {
            throw new NullPointerException("자기소개는 null 일 수 없습니다");
        }
    }

    private void validateSize(String value) {
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new MemberDomainException.InvalidIntroduceLength(INVALID_INTRODUCE_LENGTH);
        }
    }

    public static String getOrDefault(Introduce introduce) {
        if (introduce == null) {
            return "";
        }

        return introduce.getValue();
    }
}
