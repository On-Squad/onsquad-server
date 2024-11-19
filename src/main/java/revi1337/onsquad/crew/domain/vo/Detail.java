package revi1337.onsquad.crew.domain.vo;

import static revi1337.onsquad.crew.error.CrewErrorCode.INVALID_DETAIL_LENGTH;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.crew.error.exception.CrewDomainException;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class Detail {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 150;
    private static final int PERSIST_MAX_LENGTH = MAX_LENGTH * 3;

    @Column(name = "detail", nullable = false, length = PERSIST_MAX_LENGTH)
    private String value;

    public Detail(String value) {
        validate(value);
        this.value = value;
    }

    public void validate(String value) {
        if (value == null) {
            throw new NullPointerException("크루 상세정보는 null 일 수 없습니다.");
        }

        if (value.length() > MAX_LENGTH || value.isEmpty()) {
            throw new CrewDomainException.InvalidDetailLength(INVALID_DETAIL_LENGTH, MIN_LENGTH, MAX_LENGTH);
        }
    }

    public Detail updateDetail(String detail) {
        return new Detail(detail);
    }
}
