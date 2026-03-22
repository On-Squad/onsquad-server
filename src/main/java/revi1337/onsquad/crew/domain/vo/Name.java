package revi1337.onsquad.crew.domain.vo;

import static lombok.AccessLevel.PROTECTED;
import static revi1337.onsquad.crew.domain.error.CrewErrorCode.INVALID_NAME_LENGTH;

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
public class Name {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 15;
    private static final int PERSIST_MAX_LENGTH = MAX_LENGTH * 3;

    @Column(name = "name", nullable = false, length = PERSIST_MAX_LENGTH)
    private String value;

    public Name(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        Objects.requireNonNull(value, "크루명은 null 일 수 없습니다.");
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new CrewDomainException.InvalidNameLength(INVALID_NAME_LENGTH, MIN_LENGTH, MAX_LENGTH);
        }
    }
}
