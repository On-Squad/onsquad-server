package revi1337.onsquad.auth.verification.domain;

import java.time.LocalDateTime;
import java.util.List;

public record VerificationCodes(List<VerificationCode> verificationCodes) {

    public List<VerificationCode> extractAvailableBefore(LocalDateTime now) {
        return verificationCodes.stream()
                .filter(verification -> verification.isAvailableAt(now))
                .toList();
    }
}
