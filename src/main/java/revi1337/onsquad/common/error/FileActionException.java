package revi1337.onsquad.common.error;

import lombok.Getter;

@Getter
public abstract class FileActionException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String errorMessage;

    public FileActionException(ErrorCode errorCode, String finalErrorMessage, Throwable cause) {
        super(finalErrorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = finalErrorMessage;
    }

    public static class UploadFail extends FileActionException {

        public UploadFail(ErrorCode errorCode, Throwable cause) {
            super(errorCode, String.format(errorCode.getDescription()), cause);
        }
    }

    public static class DeleteFail extends FileActionException {

        public DeleteFail(ErrorCode errorCode, Throwable cause) {
            super(errorCode, String.format(errorCode.getDescription()), cause);
        }
    }

    public static class ProcessFail extends FileActionException {

        public ProcessFail(ErrorCode errorCode, Throwable cause) {
            super(errorCode, String.format(errorCode.getDescription()), cause);
        }
    }
}
