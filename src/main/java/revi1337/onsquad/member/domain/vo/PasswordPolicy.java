package revi1337.onsquad.member.domain.vo;

import static revi1337.onsquad.member.domain.error.MemberErrorCode.INVALID_PASSWORD_FORMAT;

import java.util.Arrays;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import revi1337.onsquad.member.domain.error.MemberDomainException;

@RequiredArgsConstructor
public enum PasswordPolicy {

    RAW("^(?=.*[a-zA-Z])(?=.*\\d)(?=.*\\W).{8,20}$") { // 최소 8자 ~ 20자 (영문 + 특문 + 숫자)

        @Override
        public void validate(String value) {
            if (!matches(value)) {
                throw new MemberDomainException.InvalidPasswordFormat(INVALID_PASSWORD_FORMAT);
            }
        }
    },

    BCRYPT("^\\{bcrypt\\}[$]2[abxy]?[$](?:0[4-9]|[12][0-9]|3[01])[$][./0-9a-zA-Z]{53}$") { // BCrypt

        @Override
        public void validate(String value) {
            if (!matches(value)) {
                throw new IllegalArgumentException("암호화된 비밀번호는 bcrypt 포맷이어야 합니다.");
            }
        }
    };

    private final String regex;

    public static boolean isValidAny(String value) {
        return Arrays.stream(values())
                .anyMatch(policy -> policy.matches(value));
    }

    abstract void validate(String value);

    public boolean matches(String value) {
        return value != null && Pattern.matches(regex, value);
    }
}
