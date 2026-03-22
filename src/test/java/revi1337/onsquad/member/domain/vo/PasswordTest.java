package revi1337.onsquad.member.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import revi1337.onsquad.member.domain.error.MemberDomainException;

class PasswordTest {

    @Test
    @DisplayName("평문 비밀번호를 변경에 성공한다.")
    void success1() {
        Password rawPassword = Password.of("@@@@1112aa", PasswordPolicy.RAW);

        Password updatedPassword = rawPassword.update("@@@@1112aa", PasswordPolicy.RAW);

        assertThat(updatedPassword).isNotSameAs(rawPassword);
    }

    @Test
    @DisplayName("Bcrypt 비밀번호를 변경에 성공한다.")
    void success2() {
        Password rawPassword = Password.of("{bcrypt}$2a$10$9qzyli03H7Z2dsPn3e56Su5CoOolFJApvRS86AA7d8cSvuBwnqLG.", PasswordPolicy.BCRYPT);

        Password updatedPassword = rawPassword.update("{bcrypt}$2a$10$9qzyli03H7Z2dsPn3e56Su5CoOolFJApvRS86AA7d8cSvuBwnqLG.", PasswordPolicy.BCRYPT);

        assertThat(updatedPassword).isNotSameAs(rawPassword);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11111111a", // 영문 X & 특수문자 X
            "@@@@@@@@a", // 특수문자 X & 숫자 X
            "aaaaaaaaa", // 숫자 X & 특수문자 X
            "aaaaaa@@", // 숫자 X
            "aaaaaa11", // 특수문자 X
            "1111111@", // 영문 X
            "aa1133@", // 7 자
            "aa1133@@@@aa1133@@@@@" // 21 자
    })
    @DisplayName("평문 비밀번호가 '최소 8자 ~ 20자 (영문+특문+숫자)' 를 만족하지 못하면 실패한다.")
    void rawPasswordTest(String password) {
        assertThatThrownBy(() -> Password.of(password, PasswordPolicy.RAW)).isExactlyInstanceOf(MemberDomainException.InvalidPasswordFormat.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "$2a$10$9qzyli03H7Z2dsPn3e56Su5CoOolFJApvRS86AA7d8cSvuBwnqLG.",
            "{bcrypt}$2a$10$9qzyli03H7Z2dsPn3e56Su5CoOolFJApvRS86AA7d8cSvuBwnqL",
            "{bcrypt}$2a$10$9qzyli03H7Z2dsPn3e56Su5CoOolFJApvRS86AA7d8cSvuBwnqLG.aaa",
    })
    @DisplayName("Bcrypt 비밀번호가 포맷에 맞지 않으면 실패한다.")
    void encryptedPasswordTest(String password) {
        assertThatThrownBy(() -> Password.of(password, PasswordPolicy.BCRYPT)).isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
