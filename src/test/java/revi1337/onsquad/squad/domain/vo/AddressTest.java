package revi1337.onsquad.squad.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AddressTest {

    @ParameterizedTest
    @DisplayName("기본 주소가 없으면 상세 주소가 있어도 모두 null로 초기화된다.")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void shouldBeNullWhenValueIsInvalid(String invalidValue) {
        Address address = new Address(invalidValue, "상세주소");

        assertThat(address.getValue()).isNull();
        assertThat(address.getDetail()).isNull();
    }

    @ParameterizedTest(name = "주소: {0}, 상세: {1}")
    @CsvSource({
            "서울시 강남구, 테헤란로 123",
            "경기도 성남시, ",
            "부산시 해운대구, ''"
    })
    @DisplayName("유효한 기본 주소가 있으면 입력된 값이 그대로 저장된다.")
    void shouldSaveAddressWhenValueIsValid(String value, String detail) {
        Address address = new Address(value, detail);

        assertThat(address.getValue()).isEqualTo(value);
        assertThat(address.getDetail()).isEqualTo(detail);
    }

    @ParameterizedTest
    @MethodSource("provideAddressAndExpectedValue")
    @DisplayName("getValueOrDefault는 주소나 값이 null일 때 기본값을 반환한다.")
    void getValueOrDefaultTest(Address address, String expected) {
        assertThat(Address.getValueOrDefault(address)).isEqualTo(expected);
    }

    private static Stream<Arguments> provideAddressAndExpectedValue() {
        return Stream.of(
                Arguments.of(null, ""),
                Arguments.of(new Address(null, null), ""),
                Arguments.of(new Address("강남구", "101호"), "강남구")
        );
    }

    @ParameterizedTest
    @MethodSource("provideAddressAndExpectedDetail")
    @DisplayName("getDetailOrDefault는 주소나 상세값이 null일 때 기본값을 반환한다.")
    void getDetailOrDefaultTest(Address address, String expected) {
        assertThat(Address.getDetailOrDefault(address)).isEqualTo(expected);
    }

    private static Stream<Arguments> provideAddressAndExpectedDetail() {
        return Stream.of(
                Arguments.of(null, ""),
                Arguments.of(new Address(null, null), ""),
                Arguments.of(new Address("강남구", "101호"), "101호"),
                Arguments.of(new Address("강남구", null), "")
        );
    }

    @Test
    @DisplayName("값과 상세주소가 모두 같으면 동일한 객체로 판단한다. (EqualsAndHashCode)")
    void equalsAndHashCodeTest() {
        Address address1 = new Address("서울", "강남");
        Address address2 = new Address("서울", "강남");

        assertThat(address1).isEqualTo(address2);
        assertThat(address1.hashCode()).isEqualTo(address2.hashCode());
    }
}
