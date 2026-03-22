package revi1337.onsquad.announce.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import revi1337.onsquad.announce.domain.error.AnnounceDomainException;

class TitleTest {

    @Test
    @DisplayName("Crew 공지사항은 null 일 수 없다.")
    void fail1() {
        assertThatThrownBy(() -> new Title(null))
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "공지사항 공지사항 공지사항 공지사항 공지사항 공지사항 1"})
    @DisplayName("Crew 공지사항이 비어있거나 ~ 30 자 초과면 실패한다.")
    void fail2(String value) {
        assertThatThrownBy(() -> new Title(value))
                .isExactlyInstanceOf(AnnounceDomainException.InvalidLength.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"공", "공지사항 공지사항 공지사항 공지사항 공지사항 공지사항 "})
    @DisplayName("Crew 공지사항은 생성에 성공한다.")
    void success(String value) {
        Title title = new Title(value);

        assertThat(title).isEqualTo(new Title(value));
    }
}
