package revi1337.onsquad.member.domain.vo;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@Embeddable
public class Password {

    @Column(name = "password", nullable = false)
    private String value;

    public static Password of(String value, PasswordPolicy policy) {
        Objects.requireNonNull(value, "password cannot be null");
        policy.validate(value);
        return new Password(value);
    }

    private Password(String value) {
        this.value = value;
    }

    public Password update(String value, PasswordPolicy policy) {
        return of(value, policy);
    }
}
