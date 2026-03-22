package revi1337.onsquad.member.domain.vo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.member.domain.error.MemberDomainException;

class NicknameTest {

    @Test
    @DisplayName("nickname 이 null 이면 실패한다.")
    void fail1() {
        String value = null;

        assertThatThrownBy(() -> new Nickname(value)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("nickname 의 길이가 2보다 짧으면 실패한다.")
    void fail2() {
        String value = "a";

        assertThatThrownBy(() -> new Nickname(value)).isInstanceOf(MemberDomainException.InvalidNicknameLength.class);
    }

    @Test
    @DisplayName("nickname 의 길이가 8보다 길면 실패한다.")
    void fail3() {
        String value = "aaaaaaaaa";

        assertThatThrownBy(() -> new Nickname(value)).isInstanceOf(MemberDomainException.InvalidNicknameLength.class);
    }
}
