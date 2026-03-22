package revi1337.onsquad.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_ADDRESS_DETAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_ADDRESS_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_INTRODUCE_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_KAKAO_LINK;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_MBTI_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_NICKNAME_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_PASSWORD_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.KWANGWON_PASSWORD_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_ADDRESS_DETAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_ADDRESS_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_EMAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_ENCRYPTED_PASSWORD_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_NICKNAME_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_PASSWORD_VALUE;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.event.ApplicationEvents;
import revi1337.onsquad.auth.verification.application.EmailVerificationValidator;
import revi1337.onsquad.common.ApplicationLayerTestSupport;
import revi1337.onsquad.common.fixture.MemberFixture;
import revi1337.onsquad.infrastructure.aws.s3.event.FileDeleteEvent;
import revi1337.onsquad.member.application.dto.MemberCreateDto;
import revi1337.onsquad.member.application.dto.MemberPasswordUpdateDto;
import revi1337.onsquad.member.application.dto.MemberUpdateDto;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.error.MemberBusinessException;
import revi1337.onsquad.member.domain.error.MemberErrorCode;
import revi1337.onsquad.member.domain.repository.MemberRepository;
import revi1337.onsquad.member.domain.vo.Email;

class MemberCommandServiceTest extends ApplicationLayerTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @MockBean
    private EmailVerificationValidator emailVerificationValidator;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private ApplicationEvents events;

    @Nested
    @DisplayName("신규 회원 가입")
    class newMember {

        @Test
        @DisplayName("가입 정보가 유효하고 이메일 인증이 완료되면 가입에 성공한다")
        void test1() {
            MemberCreateDto createDto = new MemberCreateDto(
                    REVI_EMAIL_VALUE,
                    REVI_PASSWORD_VALUE,
                    REVI_PASSWORD_VALUE,
                    REVI_NICKNAME_VALUE,
                    REVI_ADDRESS_VALUE,
                    REVI_ADDRESS_DETAIL_VALUE
            );
            clearPersistenceContext();
            doNothing().when(emailVerificationValidator).ensureEmailVerified(REVI_EMAIL_VALUE);

            memberCommandService.newMember(createDto);

            assertSoftly(softly -> {
                Member savedMember = memberRepository.findByEmail(new Email(REVI_EMAIL_VALUE)).get();
                softly.assertThat(savedMember.getEmail().getValue()).isEqualTo(REVI_EMAIL_VALUE);
                softly.assertThat(savedMember.getNickname().getValue()).isEqualTo(REVI_NICKNAME_VALUE);
                softly.assertThat(savedMember.getPassword().getValue()).isNotEqualTo(REVI_PASSWORD_VALUE);
            });
        }

        @Test
        @DisplayName("이메일 인증이 완료되지 않은 상태로 가입을 시도하면 실패한다")
        void test2() {
            MemberCreateDto createDto = new MemberCreateDto(
                    REVI_EMAIL_VALUE,
                    REVI_PASSWORD_VALUE,
                    REVI_PASSWORD_VALUE,
                    REVI_NICKNAME_VALUE,
                    REVI_ADDRESS_VALUE,
                    REVI_ADDRESS_DETAIL_VALUE
            );
            clearPersistenceContext();
            willThrow(new MemberBusinessException.NonAuthenticateEmail(MemberErrorCode.NON_AUTHENTICATE_EMAIL))
                    .given(emailVerificationValidator)
                    .ensureEmailVerified(REVI_EMAIL_VALUE);

            assertThatThrownBy(() -> memberCommandService.newMember(createDto))
                    .isInstanceOf(MemberBusinessException.NonAuthenticateEmail.class);
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임으로 가입을 시도하면 실패한다")
        void test3() {
            memberRepository.save(MemberFixture.createRevi());
            MemberCreateDto createDto = new MemberCreateDto(
                    REVI_EMAIL_VALUE,
                    REVI_PASSWORD_VALUE,
                    REVI_PASSWORD_VALUE,
                    REVI_NICKNAME_VALUE,
                    REVI_ADDRESS_VALUE,
                    REVI_ADDRESS_DETAIL_VALUE
            );
            clearPersistenceContext();
            doNothing().when(emailVerificationValidator).ensureEmailVerified(REVI_EMAIL_VALUE);

            assertThatThrownBy(() -> memberCommandService.newMember(createDto))
                    .isInstanceOf(MemberBusinessException.DuplicateNickname.class);
        }

        @Test
        @DisplayName("이미 가입된 이메일로 가입을 시도하면 실패한다")
        void test4() {
            memberRepository.save(MemberFixture.createRevi());
            MemberCreateDto createDto = new MemberCreateDto(
                    REVI_EMAIL_VALUE,
                    ANDONG_PASSWORD_VALUE,
                    ANDONG_PASSWORD_VALUE,
                    ANDONG_NICKNAME_VALUE,
                    ANDONG_ADDRESS_VALUE,
                    ANDONG_ADDRESS_DETAIL_VALUE
            );
            clearPersistenceContext();
            doNothing().when(emailVerificationValidator).ensureEmailVerified(REVI_EMAIL_VALUE);

            assertThatThrownBy(() -> memberCommandService.newMember(createDto))
                    .isInstanceOf(MemberBusinessException.DuplicateEmail.class);
        }
    }

    @Nested
    @DisplayName("회원 프로필 변경")
    class updateProfile {

        @Test
        @DisplayName("회원 프로필 정보 변경에 성공한다")
        void test1() {
            Member revi = memberRepository.save(MemberFixture.createRevi());
            MemberUpdateDto updateDto = new MemberUpdateDto(
                    ANDONG_NICKNAME_VALUE,
                    ANDONG_INTRODUCE_VALUE,
                    ANDONG_MBTI_VALUE,
                    ANDONG_KAKAO_LINK,
                    ANDONG_ADDRESS_VALUE,
                    ANDONG_ADDRESS_DETAIL_VALUE
            );
            clearPersistenceContext();

            memberCommandService.updateProfile(revi.getId(), updateDto);

            assertSoftly(softly -> {
                clearPersistenceContext();
                Member updatedRevi = memberRepository.findById(revi.getId()).get();
                softly.assertThat(updatedRevi.getNickname().getValue()).isEqualTo(ANDONG_NICKNAME_VALUE);
                softly.assertThat(updatedRevi.getIntroduce().getValue()).isEqualTo(ANDONG_INTRODUCE_VALUE);
                softly.assertThat(updatedRevi.getMbti().name()).isEqualTo(ANDONG_MBTI_VALUE);
                softly.assertThat(updatedRevi.getKakaoLink()).isEqualTo(ANDONG_KAKAO_LINK);
                softly.assertThat(updatedRevi.getAddress().getValue()).isEqualTo(ANDONG_ADDRESS_VALUE);
                softly.assertThat(updatedRevi.getAddress().getDetail()).isEqualTo(ANDONG_ADDRESS_DETAIL_VALUE);
            });
        }
    }

    @Nested
    @DisplayName("회원 비밀번호 변경")
    class updatePassword {

        @Test
        @DisplayName("현재 비밀번호와 일치하면 비밀번호 변경에 성공한다")
        void test1() {
            Member revi = memberRepository.save(MemberFixture.createRevi());
            MemberPasswordUpdateDto dto = new MemberPasswordUpdateDto(
                    REVI_PASSWORD_VALUE,
                    ANDONG_PASSWORD_VALUE,
                    ANDONG_PASSWORD_VALUE
            );
            clearPersistenceContext();

            memberCommandService.updatePassword(revi.getId(), dto);

            clearPersistenceContext();
            Member updatedRevi = memberRepository.findById(revi.getId()).get();
            assertThat(updatedRevi.getPassword().getValue()).isNotEqualTo(REVI_ENCRYPTED_PASSWORD_VALUE);
        }

        @Test
        @DisplayName("현재 비밀번호와 다르면 비밀번호 변경에 실패한다")
        void test2() {
            Member revi = memberRepository.save(MemberFixture.createRevi());
            MemberPasswordUpdateDto dto = new MemberPasswordUpdateDto(
                    KWANGWON_PASSWORD_VALUE,
                    ANDONG_PASSWORD_VALUE,
                    ANDONG_PASSWORD_VALUE
            );
            clearPersistenceContext();

            assertThatThrownBy(() -> memberCommandService.updatePassword(revi.getId(), dto))
                    .isExactlyInstanceOf(MemberBusinessException.WrongPassword.class);
        }
    }

    @Nested
    @DisplayName("회원 프로필 이미지 변경")
    class updateImage {

        @Test
        @DisplayName("회원 프로필 이미지변경에 성공한다 (1)")
        void test1() {
            Member revi = memberRepository.save(MemberFixture.createRevi());
            clearPersistenceContext();

            memberCommandService.updateImage(revi.getId(), "update-image2");

            assertSoftly(softly -> {
                clearPersistenceContext();
                Member updatedRevi = memberRepository.findById(revi.getId()).get();
                softly.assertThat(updatedRevi.hasImage()).isTrue();
                softly.assertThat(updatedRevi.hasImage()).isTrue();
                softly.assertThat(updatedRevi.getProfileImage()).isEqualTo("update-image2");
                softly.assertThat(events.stream(FileDeleteEvent.class).count()).isEqualTo(1);
            });
        }

        @Test
        @DisplayName("회원 프로필 이미지변경에 성공한다 (2)")
        void test2() {
            Member member = MemberFixture.createRevi();
            member.updateImage("update-image1");
            Member revi = memberRepository.save(member);
            memberCommandService.deleteImage(revi.getId());
            clearPersistenceContext();

            memberCommandService.updateImage(revi.getId(), "update-image2");

            assertSoftly(softly -> {
                clearPersistenceContext();
                Member updatedRevi = memberRepository.findById(revi.getId()).get();
                softly.assertThat(updatedRevi.hasImage()).isTrue();
                softly.assertThat(updatedRevi.hasImage()).isTrue();
                softly.assertThat(updatedRevi.getProfileImage()).isEqualTo("update-image2");
                softly.assertThat(events.stream(FileDeleteEvent.class).count()).isEqualTo(1);
            });
        }
    }

    @Nested
    @DisplayName("회원 프로필 이미지 삭제")
    class deleteImage {

        @Test
        @DisplayName("회원 프로필 이미지 삭제에 성공한다 (1)")
        void test1() {
            Member revi = memberRepository.save(MemberFixture.createRevi());
            clearPersistenceContext();

            memberCommandService.deleteImage(revi.getId());
            assertSoftly(softly -> {
                clearPersistenceContext();
                softly.assertThat(memberRepository.findById(revi.getId()).get().hasImage()).isFalse();
                softly.assertThat(events.stream(FileDeleteEvent.class).count()).isEqualTo(1);
            });
        }

        @Test
        @DisplayName("회원 프로필 이미지 삭제에 성공한다 (2)")
        void test2() {
            Member member = MemberFixture.createRevi();
            member.updateImage("update-image1");
            Member revi = memberRepository.save(member);
            clearPersistenceContext();

            memberCommandService.deleteImage(revi.getId());

            assertSoftly(softly -> {
                clearPersistenceContext();
                softly.assertThat(memberRepository.findById(revi.getId()).get().hasImage()).isFalse();
                softly.assertThat(events.stream(FileDeleteEvent.class).count()).isEqualTo(1);
            });
        }
    }
}
