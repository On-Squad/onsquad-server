package revi1337.onsquad.member.domain.vo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.member.domain.error.MemberDomainException;

class IntroduceTest {

    @Test
    @DisplayName("자기소개가 null 이면 실패한다.")
    void fail1() {
        String value = null;

        assertThatThrownBy(() -> new Introduce(value)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("자기소개의 길이가 1 보다 짧으면 실패한다.")
    void fail2() {
        String value = "";

        assertThatThrownBy(() -> new Introduce(value)).isInstanceOf(MemberDomainException.InvalidIntroduceLength.class);
    }

    @Test
    @DisplayName("자기소개의 길이가 200 보다 길면 실패한다.")
    void fail3() {
        String value = "a".repeat(201);

        assertSoftly(softly -> {
            softly.assertThat(value.length()).isEqualTo(201);
            softly.assertThatThrownBy(() -> new Introduce(value)).isInstanceOf(MemberDomainException.InvalidIntroduceLength.class);
        });
    }
}
