package revi1337.onsquad.member.error.exception;

import lombok.Getter;
import revi1337.onsquad.common.error.ErrorCode;

@Getter
public abstract class MemberDomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String errorMessage;

    public MemberDomainException(ErrorCode errorCode, String finalErrorMessage) {
        super(finalErrorMessage);
        this.errorCode = errorCode;
        this.errorMessage = finalErrorMessage;
    }

    public static class InvalidEmailFormat extends MemberBusinessException {

        public InvalidEmailFormat(ErrorCode errorCode) {
            super(errorCode, String.format(errorCode.getDescription()));
        }
    }

    public static class InvalidPasswordFormat extends MemberBusinessException {

        public InvalidPasswordFormat(ErrorCode errorCode) {
            super(errorCode, String.format(errorCode.getDescription()));
        }
    }

    public static class InvalidNicknameLength extends MemberBusinessException {

        public InvalidNicknameLength(ErrorCode errorCode, Number begin, Number end) {
            super(errorCode, String.format(errorCode.getDescription(), begin, end));
        }
    }

    public static class InvalidIntroduceLength extends MemberBusinessException {

        public InvalidIntroduceLength(ErrorCode errorCode, Number begin, Number end) {
            super(errorCode, String.format(errorCode.getDescription(), begin, end));
        }
    }
}
