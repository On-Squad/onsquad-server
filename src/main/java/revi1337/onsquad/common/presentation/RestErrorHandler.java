package revi1337.onsquad.common.presentation;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import revi1337.onsquad.common.dto.ProblemDetail;
import revi1337.onsquad.common.dto.RestResponse;
import revi1337.onsquad.common.error.CommonErrorCode;
import revi1337.onsquad.common.error.ErrorCode;
import revi1337.onsquad.common.error.RequestDispatcherResolver;

@RestController
public class RestErrorHandler implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<RestResponse<ProblemDetail>> handleError(HttpServletRequest httpServletRequest) {
        HttpStatus httpStatus = new RequestDispatcherResolver(httpServletRequest).resolveHttpStatus();
        return switch (httpStatus) {
            case BAD_REQUEST -> {
                ErrorCode errorCode = CommonErrorCode.INVALID_INPUT_VALUE;
                yield ResponseEntity.ok()
                        .body(RestResponse.fail(errorCode, ProblemDetail.of(errorCode)));
            }

            case NOT_FOUND -> {
                ErrorCode errorCode = CommonErrorCode.NOT_FOUND;
                yield ResponseEntity.ok()
                        .body(RestResponse.fail(errorCode, ProblemDetail.of(errorCode)));
            }

            default -> {
                ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
                yield ResponseEntity.ok()
                        .body(RestResponse.fail(errorCode, ProblemDetail.of(errorCode)));
            }
        };
    }
}
