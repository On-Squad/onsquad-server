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

class ContentTest {

    @Test
    @DisplayName("본문 내용은 null 일 수 없다.")
    void contentCannotBeNull() {
        assertThatThrownBy(() -> new Content(null))
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidContents")
    @DisplayName("본문 길이가 1자 미만이거나 10,000자를 초과하면 예외가 발생한다.")
    void shouldFailWhenContentLengthIsInvalid(String invalidValue) {
        assertThatThrownBy(() -> new Content(invalidValue))
                .isExactlyInstanceOf(SquadDomainException.InvalidContentLength.class);
    }

    @ParameterizedTest
    @MethodSource("provideValidContents")
    @DisplayName("본문 길이가 1자 이상 10,000자 이하이면 생성에 성공한다.")
    void shouldCreateContentWhenLengthIsValid(String validValue) {
        Content content = new Content(validValue);

        assertThat(content.getValue()).isEqualTo(validValue);
        assertThat(content).isEqualTo(new Content(validValue));
    }

    private static Stream<Arguments> provideInvalidContents() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("a".repeat(10001))
        );
    }

    private static Stream<Arguments> provideValidContents() {
        return Stream.of(
                Arguments.of("본"),
                Arguments.of("일반적인 본문 내용입니다."),
                Arguments.of("한".repeat(10000))
        );
    }
}
