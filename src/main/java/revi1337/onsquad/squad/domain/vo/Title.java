package revi1337.onsquad.squad.domain.vo;

import static lombok.AccessLevel.PROTECTED;
import static revi1337.onsquad.squad.domain.error.SquadErrorCode.INVALID_TITLE_LENGTH;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.squad.domain.error.SquadDomainException;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@Embeddable
public class Title {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 60;
    private static final int PERSIST_MAX_LENGTH = MAX_LENGTH * 3;

    @Column(name = "title", nullable = false, length = PERSIST_MAX_LENGTH)
    private String value;

    public Title(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        Objects.requireNonNull(value, "스쿼드 제목은 null 일 수 없습니다.");
        if (value.length() > MAX_LENGTH || value.isEmpty()) {
            throw new SquadDomainException.InvalidTitleLength(INVALID_TITLE_LENGTH, MIN_LENGTH, MAX_LENGTH);
        }
    }
}
