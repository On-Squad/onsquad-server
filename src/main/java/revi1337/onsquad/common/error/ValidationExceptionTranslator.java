package revi1337.onsquad.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindException;
import revi1337.onsquad.common.dto.ProblemDetail;

@RequiredArgsConstructor
public class ValidationExceptionTranslator {

    private List<String> errors;

    public ProblemDetail translate(CommonErrorCode commonErrorCode, Exception exception) {
        if (exception instanceof ConstraintViolationException constraintViolationException) {
            errors = extractFieldError(constraintViolationException);
            return ProblemDetail.of(commonErrorCode, errors);
        }

        if (exception instanceof BindException bindException) {
            errors = extractFieldError(bindException);
            return ProblemDetail.of(commonErrorCode, errors);
        }

        throw new IllegalArgumentException("unsupport exception");
    }

    private List<String> extractFieldError(ConstraintViolationException constraintViolationException) {
        return constraintViolationException.getConstraintViolations().stream()
                .map(constraintViolation -> Arrays.stream(constraintViolation.getPropertyPath().toString().split("\\."))
                        .reduce((first, second) -> second).orElse(null))
                .toList();
    }

    private List<String> extractFieldError(BindException bindException) {
        return new ArrayList<>() {
            {
                bindException.getBindingResult()
                        .getFieldErrors()
                        .forEach(fieldError -> add(fieldError.getField()));

                bindException.getGlobalErrors()
                        .forEach(objectError -> add(objectError.getCode()));
            }
        };
    }
}
