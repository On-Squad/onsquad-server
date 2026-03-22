package revi1337.onsquad.crew.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_NAME_VALUE;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import revi1337.onsquad.crew.domain.error.CrewDomainException;

class NameTest {

    @Test
    @DisplayName("Crew 이름 생성에 성공한다.")
    void success() {
        Name name = new Name(CREW_NAME_VALUE);

        assertThat(name).isEqualTo(new Name(CREW_NAME_VALUE));
    }

    @Test
    @DisplayName("Crew 이름은 Null 일 수 없다.")
    void fail1() {
        assertThatThrownBy(() -> new Name(null))
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "이름 이름 이름 이름 이름 이"})
    @DisplayName("Crew 이름은 1자 이상 15자 이하여야 한다.")
    void fail2(String name) {
        assertThatThrownBy(() -> new Name(name))
                .isExactlyInstanceOf(CrewDomainException.InvalidNameLength.class);
    }
}
