package revi1337.onsquad.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileErrorCode implements ErrorCode {

    FAIL_PROCESS(500, "F001", "파일 처리에 실패했습니다."),
    FAIL_UPLOAD(500, "F002", "파일 업로드에 실패하였습니다."),
    FAIL_DELETE(500, "F003", "파일 삭제에 실패하였습니다.");

    private final int status;
    private final String code;
    private final String description;

}
