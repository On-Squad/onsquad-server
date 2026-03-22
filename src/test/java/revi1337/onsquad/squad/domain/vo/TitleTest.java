package revi1337.onsquad.squad.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import revi1337.onsquad.squad.domain.error.SquadDomainException;

class TitleTest {

    @Test
    @DisplayName("스쿼드 제목은 null 일 수 없다.")
    void titleCannotBeNull() {
        assertThatThrownBy(() -> new Title(null))
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidTitles")
    @DisplayName("스쿼드 제목이 1자 미만이거나 60자를 초과하면 실패한다.")
    void shouldFailWhenTitleLengthIsInvalid(String invalidValue) {
        assertThatThrownBy(() -> new Title(invalidValue))
                .isExactlyInstanceOf(SquadDomainException.InvalidTitleLength.class);
    }

    @ParameterizedTest
    @MethodSource("provideValidTitles")
    @DisplayName("스쿼드 제목이 1자 이상 60자 이하이면 생성에 성공한다.")
    void shouldSuccessWhenTitleLengthIsValid(String validValue) {
        Title title = new Title(validValue);

        assertThat(title.getValue()).isEqualTo(validValue);
    }

    private static Stream<Arguments> provideInvalidTitles() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("a".repeat(61))
        );
    }

    private static Stream<Arguments> provideValidTitles() {
        return Stream.of(
                Arguments.of("a"),
                Arguments.of("스쿼드 제목입니다."),
                Arguments.of("가".repeat(60))
        );
    }
}
