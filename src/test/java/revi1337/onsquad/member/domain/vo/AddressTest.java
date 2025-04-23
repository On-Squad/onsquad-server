package revi1337.onsquad.member.domain.vo;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AddressTest {

    @Test
    @DisplayName("주소 혹은 상세주소가 null 이면 실패합니다.")
    void testAddress1() {
        String address = null;
        String addressDetail = null;

        assertThatThrownBy(() -> new Address(address, addressDetail))
                .isInstanceOf(IllegalArgumentException.class);
    }
}