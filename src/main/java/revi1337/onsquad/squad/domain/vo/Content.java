package revi1337.onsquad.squad.domain.vo;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.squad.domain.error.SquadDomainException;
import revi1337.onsquad.squad.domain.error.SquadErrorCode;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@Embeddable
public class Content {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 10000;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String value;

    public Content(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        Objects.requireNonNull(value, "스쿼드 본문은 null 일 수 없습니다.");
        if (value.length() > MAX_LENGTH || value.isEmpty()) {
            throw new SquadDomainException.InvalidContentLength(SquadErrorCode.INVALID_CONTENT_LENGTH, MIN_LENGTH, MAX_LENGTH);
        }
    }
}
