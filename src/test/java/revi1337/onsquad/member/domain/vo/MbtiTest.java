package revi1337.onsquad.member.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.member.domain.error.MemberDomainException;

class MbtiTest {

    @Test
    @DisplayName("mbti 로 파싱 성공한다.")
    void success() {
        String mbtiText = "ISTP";

        Mbti mbti = Mbti.parse(mbtiText);

        assertThat(mbti.name()).isEqualTo(mbtiText);
    }

    @Test
    @DisplayName("소문자 mbti 로 파싱에 성공한다.")
    void success2() {
        String mbtiText = "istp";

        Mbti mbti = Mbti.parse(mbtiText);

        assertThat(mbti.name()).isEqualToIgnoringCase(mbtiText);
    }

    @Test
    @DisplayName("잘못된 MBTI 값을 파싱하면 실패한다.")
    void fail() {
        String invalidMbtiValue = "xxxx";

        assertThatThrownBy(() -> Mbti.parse(invalidMbtiValue)).isExactlyInstanceOf(MemberDomainException.InvalidMbti.class);
    }
}
