package revi1337.onsquad.common.error;

import java.util.EnumSet;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT_VALUE(400, "C001", "유효성 검증 실패"),
    MISSING_PARAMETER(400, "C002", "파라미터가 필요한 요청"),
    METHOD_NOT_SUPPORT(405, "C003", "지원하지 않는 메서드"),
    PARAMETER_TYPE_MISMATCH(400, "C004", "파라미터 타입 불일치"),
    NOT_FOUND(404, "C005", "존재하지 않는 API 요청"),
    INTERNAL_SERVER_ERROR(500, "C006", "서버에서 처리 불가한 요청"),
    REQUEST_CONFLICT(409, "C007", "중복 요청 발생. %s 후에 재시도해주세요."),
    ALREADY_REQUEST(409, "C008", "이미 처리된 요청입니다. uri : %s");

    private final int status;
    private final String code;
    private final String description;

    public static EnumSet<CommonErrorCode> defaultEnumSet() {
        return EnumSet.allOf(CommonErrorCode.class);
    }

    public static EnumSet<CommonErrorCode> forCommonCase() {
        EnumSet<CommonErrorCode> commonCase = EnumSet.noneOf(CommonErrorCode.class);
        defaultEnumSet().stream()
                .filter(errorCode -> errorCode.getCode().startsWith("C"))
                .forEach(commonCase::add);
        return commonCase;
    }
}