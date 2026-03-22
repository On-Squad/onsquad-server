package revi1337.onsquad.squad.domain.vo;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@Embeddable
public class Address {

    private static final String DEFAULT_VALUE = "";
    private static final String DEFAULT_DETAIL = "";

    @Column(name = "address")
    private String value;

    @Column(name = "address_detail")
    private String detail;

    public static String getValueOrDefault(Address address) {
        if (address == null) {
            return DEFAULT_VALUE;
        }
        return address.getValue() != null ? address.getValue() : DEFAULT_VALUE;
    }

    public static String getDetailOrDefault(Address address) {
        if (address == null) {
            return DEFAULT_DETAIL;
        }
        return address.getDetail() != null ? address.getDetail() : DEFAULT_DETAIL;
    }

    public Address(String value, String detail) {
        if (value == null || value.isBlank()) {
            this.value = null;
            this.detail = null;
            return;
        }

        this.value = value;
        this.detail = detail;
    }
}
