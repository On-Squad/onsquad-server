package revi1337.onsquad.announce.domain.vo;

import static lombok.AccessLevel.PROTECTED;
import static revi1337.onsquad.announce.domain.error.AnnounceErrorCode.INVALID_LENGTH;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.announce.domain.error.AnnounceDomainException;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@Embeddable
public class Title {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 30;
    private static final int PERSIST_MAX_LENGTH = MAX_LENGTH * 3;

    @Column(name = "title", nullable = false, length = PERSIST_MAX_LENGTH)
    private String value;

    public Title(String value) {
        validate(value);
        this.value = value;
    }

    public void validate(String value) {
        if (value == null) {
            throw new NullPointerException("제목은 null 일 수 없습니다.");
        }

        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new AnnounceDomainException.InvalidLength(INVALID_LENGTH);
        }
    }

    public Title update(String value) {
        return new Title(value);
    }
}
