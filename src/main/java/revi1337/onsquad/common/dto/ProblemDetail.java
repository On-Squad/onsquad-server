package revi1337.onsquad.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import revi1337.onsquad.common.error.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        String code,
        String message,
        List<String> parameters,
        Map<String, String> details
) {
    private ProblemDetail(ErrorCode code, Map<String, String> details) {
        this(code.getCode(), code.getDescription(), null, details);
    }

    private ProblemDetail(ErrorCode code, List<String> parameters) {
        this(code.getCode(), code.getDescription(), parameters, null);
    }

    private ProblemDetail(ErrorCode code) {
        this(code.getCode(), code.getDescription(), null, null);
    }

    public static ProblemDetail of(ErrorCode code) {
        return new ProblemDetail(code.getCode(), code.getDescription(), null, null);
    }

    public static ProblemDetail of(ErrorCode code, List<String> parameters) {
        return new ProblemDetail(code.getCode(), code.getDescription(), parameters, null);
    }

    public static ProblemDetail of(ErrorCode code, Map<String, String> details) {
        return new ProblemDetail(code.getCode(), code.getDescription(), null, details);
    }

    public static ProblemDetail of(ErrorCode code, String errorMessage) {
        return new ProblemDetail(code.getCode(), errorMessage, null, null);
    }

    public static ProblemDetail withFormat(ErrorCode code, String errorMessage) {
        return new ProblemDetail(code.getCode(), String.format(code.getDescription(), errorMessage), null, null);
    }
}
