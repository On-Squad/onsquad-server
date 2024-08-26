package revi1337.onsquad.squad.error.exception;

import lombok.Getter;
import revi1337.onsquad.common.error.ErrorCode;

@Getter
public abstract class SquadDomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String errorMessage;

    public SquadDomainException(ErrorCode errorCode, String finalErrorMessage) {
        super(finalErrorMessage);
        this.errorCode = errorCode;
        this.errorMessage = finalErrorMessage;
    }

    public static class InvalidCapacitySize extends SquadDomainException {

        public InvalidCapacitySize(ErrorCode errorCode, Number minSize, Number maxSize) {
            super(errorCode, String.format(errorCode.getDescription(), minSize, maxSize));
        }
    }

    public static class NotEnoughLeft extends SquadDomainException {

        public NotEnoughLeft(ErrorCode errorCode) {
            super(errorCode, String.format(errorCode.getDescription()));
        }
    }

    public static class InvalidCategory extends SquadDomainException {

        public InvalidCategory(ErrorCode errorCode) {
            super(errorCode, String.format(errorCode.getDescription()));
        }
    }
}
