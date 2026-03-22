package revi1337.onsquad.auth.verification.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;
import lombok.Getter;
import revi1337.onsquad.common.domain.EmailContent;

@Getter
public class VerificationCode implements EmailContent {

    private final String email;
    private final String code;
    private final VerificationStatus status;
    private final LocalDateTime expiredAt;

    public VerificationCode(String email, String code, VerificationStatus status, long expireMilli) {
        this.email = email;
        this.code = code;
        this.status = status;
        this.expiredAt = Instant.ofEpochMilli(expireMilli)
                .atZone(TimeZone.getDefault().toZoneId())
                .toLocalDateTime();
    }

    public boolean isAvailableAt(LocalDateTime now) {
        return expiredAt.isAfter(now);
    }
}
