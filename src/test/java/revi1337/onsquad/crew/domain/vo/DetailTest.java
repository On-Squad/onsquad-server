package revi1337.onsquad.crew.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_DETAIL_VALUE;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.crew.domain.error.CrewDomainException;

class DetailTest {

    @Test
    @DisplayName("Crew 상세 정보 생성에 성공한다.")
    void success() {
        Detail detail = new Detail(CREW_DETAIL_VALUE);

        assertThat(detail).isEqualTo(new Detail(CREW_DETAIL_VALUE));
    }

    @Test
    @DisplayName("Crew 상세 정보는 null 일 수 없다.")
    void fail1() {
        assertThatThrownBy(() -> new Detail(null))
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Crew 상세 정보는 1자 이상 150자 이하여야 한다. (1)")
    void fail2() {
        String value = "";

        assertThatThrownBy(() -> new Detail(value))
                .isExactlyInstanceOf(CrewDomainException.InvalidDetailLength.class);
    }

    @Test
    @DisplayName("Crew 상세 정보는 1자 이상 150자 이하여야 한다. (2)")
    void fail3() {
        String value = "상세 정보 ".repeat(30) + "A";

        assertThatThrownBy(() -> new Detail(value))
                .isExactlyInstanceOf(CrewDomainException.InvalidDetailLength.class);
    }
}
