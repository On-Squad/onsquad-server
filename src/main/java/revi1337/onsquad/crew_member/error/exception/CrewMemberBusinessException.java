package revi1337.onsquad.crew_member.error.exception;

import lombok.Getter;
import revi1337.onsquad.common.error.ErrorCode;

@Getter
public abstract class CrewMemberBusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String errorMessage;

    public CrewMemberBusinessException(ErrorCode errorCode, String finalErrorMessage) {
        super(finalErrorMessage);
        this.errorCode = errorCode;
        this.errorMessage = finalErrorMessage;
    }

    public static class NeverRequested extends CrewMemberBusinessException {

        public NeverRequested(ErrorCode errorCode, String crewName) {
            super(errorCode, String.format(errorCode.getDescription(), crewName));
        }
    }
}
