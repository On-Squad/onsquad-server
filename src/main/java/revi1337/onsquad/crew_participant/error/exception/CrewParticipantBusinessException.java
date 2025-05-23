package revi1337.onsquad.crew_participant.error.exception;

import lombok.Getter;
import revi1337.onsquad.common.error.ErrorCode;

@Getter
public abstract class CrewParticipantBusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String errorMessage;

    public CrewParticipantBusinessException(ErrorCode errorCode, String finalErrorMessage) {
        super(finalErrorMessage);
        this.errorCode = errorCode;
        this.errorMessage = finalErrorMessage;
    }

    public static class NeverRequested extends CrewParticipantBusinessException {

        public NeverRequested(ErrorCode errorCode) {
            super(errorCode, errorCode.getDescription());
        }
    }

    public static class InvalidReference extends CrewParticipantBusinessException {

        public InvalidReference(ErrorCode errorCode) {
            super(errorCode, errorCode.getDescription());
        }
    }

    public static class CantSeeParticipant extends CrewParticipantBusinessException {

        public CantSeeParticipant(ErrorCode errorCode) {
            super(errorCode, errorCode.getDescription());
        }
    }
}
