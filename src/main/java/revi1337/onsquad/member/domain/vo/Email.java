package revi1337.onsquad.member.domain.vo;

import static lombok.AccessLevel.PROTECTED;
import static revi1337.onsquad.member.domain.error.MemberErrorCode.INVALID_EMAIL_FORMAT;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.member.domain.error.MemberDomainException;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@Embeddable
public class Email {

    private static final String EMAIL_REGEX = "^([\\w\\.\\_\\-])*[a-zA-Z0-9]+([\\w\\.\\_\\-])*([a-zA-Z0-9])+([\\w\\.\\_\\-])+@([a-zA-Z0-9]+\\.)+[a-zA-Z0-9]{2,8}$";

    @Column(name = "email", nullable = false)
    private String value;

    public Email(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value == null) {
            throw new NullPointerException("이메일은 null 일 수 없습니다.");
        }

        if (invalidEmailFormat(value)) {
            throw new MemberDomainException.InvalidEmailFormat(INVALID_EMAIL_FORMAT);
        }
    }

    private boolean invalidEmailFormat(String value) {
        return !Pattern.matches(EMAIL_REGEX, value);
    }
}
