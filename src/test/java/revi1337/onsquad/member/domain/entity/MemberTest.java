package revi1337.onsquad.member.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_ADDRESS_DETAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_ADDRESS_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_INTRODUCE_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_KAKAO_LINK;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_MBTI_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_NICKNAME_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_ADDRESS_DETAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_ADDRESS_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_EMAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_ENCRYPTED_PASSWORD_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_NICKNAME_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_OAUTH_PROFILE_IMAGE_LINK;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_PASSWORD_VALUE;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.member.domain.model.ProfileSpec;
import revi1337.onsquad.member.domain.vo.Address;
import revi1337.onsquad.member.domain.vo.Email;
import revi1337.onsquad.member.domain.vo.Introduce;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.member.domain.vo.Nickname;
import revi1337.onsquad.member.domain.vo.Password;
import revi1337.onsquad.member.domain.vo.PasswordPolicy;
import revi1337.onsquad.member.domain.vo.UserType;

class MemberTest {

    @Test
    @DisplayName("일반 사용자 생성에 성공한다.")
    void success1() {
        Member revi = Member.general(
                REVI_EMAIL_VALUE,
                REVI_PASSWORD_VALUE,
                REVI_NICKNAME_VALUE,
                REVI_ADDRESS_VALUE,
                REVI_ADDRESS_DETAIL_VALUE
        );

        assertSoftly(softly -> {
            softly.assertThat(revi.getEmail()).isEqualTo(new Email(REVI_EMAIL_VALUE));
            softly.assertThat(revi.getPassword()).isEqualTo(Password.of(REVI_PASSWORD_VALUE, PasswordPolicy.RAW));
            softly.assertThat(revi.getNickname()).isEqualTo(new Nickname(REVI_NICKNAME_VALUE));
            softly.assertThat(revi.getIntroduce()).isNull();
            softly.assertThat(revi.getAddress()).isEqualTo(new Address(REVI_ADDRESS_VALUE, REVI_ADDRESS_DETAIL_VALUE));

            softly.assertThat(revi.getProfileImage()).isNull();
            softly.assertThat(revi.getKakaoLink()).isNull();
            softly.assertThat(revi.getUserType()).isSameAs(UserType.GENERAL);
            softly.assertThat(revi.getMbti()).isNull();
        });
    }

    @Test
    @DisplayName("OAuth2 사용자 생성에 성공한다.")
    void success2() {
        Member revi = Member.oauth2(
                REVI_EMAIL_VALUE,
                REVI_ENCRYPTED_PASSWORD_VALUE,
                REVI_NICKNAME_VALUE,
                REVI_OAUTH_PROFILE_IMAGE_LINK,
                UserType.KAKAO
        );

        assertSoftly(softly -> {
            softly.assertThat(revi.getEmail()).isEqualTo(new Email(REVI_EMAIL_VALUE));
            softly.assertThat(revi.getPassword()).isEqualTo(Password.of(REVI_ENCRYPTED_PASSWORD_VALUE, PasswordPolicy.BCRYPT));
            softly.assertThat(revi.getNickname()).isEqualTo(new Nickname(REVI_NICKNAME_VALUE));
            softly.assertThat(revi.getProfileImage()).isEqualTo(REVI_OAUTH_PROFILE_IMAGE_LINK);
            softly.assertThat(revi.getUserType()).isSameAs(UserType.KAKAO);

            softly.assertThat(revi.getAddress()).isNull();
            softly.assertThat(revi.getIntroduce()).isNull();
            softly.assertThat(revi.getKakaoLink()).isNull();
            softly.assertThat(revi.getMbti()).isNull();
        });
    }

    @Test
    @DisplayName("회원정보 프로필 업데이트에 성공한다.")
    void success3() {
        Member revi = Member.general(REVI_EMAIL_VALUE, REVI_PASSWORD_VALUE, REVI_NICKNAME_VALUE, REVI_ADDRESS_VALUE, REVI_ADDRESS_DETAIL_VALUE);
        ProfileSpec profileSpec = new ProfileSpec(
                ANDONG_NICKNAME_VALUE,
                ANDONG_INTRODUCE_VALUE,
                ANDONG_MBTI_VALUE,
                ANDONG_ADDRESS_VALUE,
                ANDONG_ADDRESS_DETAIL_VALUE,
                ANDONG_KAKAO_LINK
        );

        revi.updateProfile(profileSpec);

        assertSoftly(softly -> {
            softly.assertThat(revi.getNickname()).isEqualTo(new Nickname(ANDONG_NICKNAME_VALUE));
            softly.assertThat(revi.getIntroduce()).isEqualTo(new Introduce(ANDONG_INTRODUCE_VALUE));
            softly.assertThat(revi.getMbti()).isSameAs(Mbti.ISFP);
            softly.assertThat(revi.getAddress()).isEqualTo(new Address(ANDONG_ADDRESS_VALUE, ANDONG_ADDRESS_DETAIL_VALUE));
            softly.assertThat(revi.getKakaoLink()).isEqualTo(ANDONG_KAKAO_LINK);

            softly.assertThat(revi.getEmail()).isEqualTo(new Email(REVI_EMAIL_VALUE));
            softly.assertThat(revi.getPassword()).isEqualTo(Password.of(REVI_PASSWORD_VALUE, PasswordPolicy.RAW));
        });
    }

    @Test
    @DisplayName("프로필 이미지 정보를 업데이트에 성공한다.")
    void success4() {
        Member revi = Member.general(
                REVI_EMAIL_VALUE,
                REVI_PASSWORD_VALUE,
                REVI_NICKNAME_VALUE,
                REVI_ADDRESS_VALUE,
                REVI_ADDRESS_DETAIL_VALUE
        );
        String changedImage = "https://changeed_img.com";

        revi.updateImage(changedImage);

        assertThat(revi.getProfileImage()).isEqualTo(changedImage);
    }

    @Test
    @DisplayName("기본 프로필 이미지 변경에 성공한다.")
    void success5() {
        Member revi = Member.general(
                REVI_EMAIL_VALUE,
                REVI_PASSWORD_VALUE,
                REVI_NICKNAME_VALUE,
                REVI_ADDRESS_VALUE,
                REVI_ADDRESS_DETAIL_VALUE
        );
        String changedImage = "https://changeed_img.com";

        revi.updateImage(changedImage);

        assertSoftly(softly -> {
            softly.assertThat(revi.hasImage()).isTrue();
            softly.assertThat(revi.getProfileImage()).isEqualTo(changedImage);
        });
    }

    @Test
    @DisplayName("비밀번호 변경에 성공한다.")
    void success8() {
        Member revi = Member.general(
                REVI_EMAIL_VALUE,
                REVI_PASSWORD_VALUE,
                REVI_NICKNAME_VALUE,
                REVI_ADDRESS_VALUE,
                REVI_ADDRESS_DETAIL_VALUE
        );
        String changedPassword = "12345!@asb";

        revi.updatePassword(changedPassword, PasswordPolicy.RAW);

        assertThat(revi.getPassword()).isEqualTo(Password.of(changedPassword, PasswordPolicy.RAW));
    }
}
