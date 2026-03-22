package revi1337.onsquad.crew.domain.vo;

import static lombok.AccessLevel.PROTECTED;
import static revi1337.onsquad.crew.domain.error.CrewErrorCode.INVALID_INTRODUCE_LENGTH;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.crew.domain.error.CrewDomainException;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@Embeddable
public class Introduce {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 200;
    private static final int PERSIST_MAX_LENGTH = MAX_LENGTH * 3;

    @Column(name = "introduce", nullable = false, length = PERSIST_MAX_LENGTH)
    private String value;

    public Introduce(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        Objects.requireNonNull(value, "크루 소개는 null 일 수 없습니다.");
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new CrewDomainException.InvalidIntroduceLength(INVALID_INTRODUCE_LENGTH, MIN_LENGTH, MAX_LENGTH);
        }
    }
}
