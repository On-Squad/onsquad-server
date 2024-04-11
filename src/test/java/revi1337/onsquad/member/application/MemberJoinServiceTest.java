package revi1337.onsquad.member.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import revi1337.onsquad.member.domain.MemberRepository;
import revi1337.onsquad.member.domain.vo.Nickname;
import revi1337.onsquad.support.TestContainerSupport;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@DisplayName("회원가입 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class MemberJoinServiceTest extends TestContainerSupport {

    @Mock private MemberRepository memberRepository;
    @Mock private JoinMailService joinMailService;
    @InjectMocks private MemberJoinService memberJoinService;

    @DisplayName("이메일 인증코드 전송이 잘 동작하는지 확인한다.")
    @Test
    public void sendAuthCodeToEmail() {
        // given
        String email = "david122123@gmail.com";
        willDoNothing().given(joinMailService).sendAuthCodeToEmail(eq(email), anyString());

        // when
        memberJoinService.sendAuthCodeToEmail(email);

        // then
        verify(joinMailService, times(1))
                .sendAuthCodeToEmail(eq(email), anyString());
    }

    @DisplayName("이메일 인증에 실패하면 true 를 반환한다.")
    @Test
    public void verifyAuthCode() {
        // given
        String email = "david122123@gmail.com";
        String authCode = "1111";
        Duration minutes = Duration.ofMinutes(5);
        given(joinMailService.verifyAuthCode(email, authCode, minutes)).willReturn(true);

        // when
        boolean success = memberJoinService.verifyAuthCode(email, authCode);

        // then
        verify(joinMailService, times(1))
                .verifyAuthCode(eq(email), eq(authCode), eq(minutes));
        assertThat(success).isTrue();
    }

    @DisplayName("이메일 인증에 실패하면 false 를 반환한다.")
    @Test
    public void verifyAuthCode2() {
        // given
        String email = "david122123@gmail.com";
        String authCode = "1111";
        Duration minutes = Duration.ofMinutes(5);
        given(joinMailService.verifyAuthCode(email, authCode, minutes)).willReturn(false);

        // when
        boolean success = memberJoinService.verifyAuthCode(email, authCode);

        // then
        verify(joinMailService, times(1))
                .verifyAuthCode(eq(email), eq(authCode), eq(minutes));
        assertThat(success).isFalse();
    }

    @DisplayName("중복되는 닉네임이 있으면 true 를 반환한다.")
    @Test
    public void checkDuplicateNickname() {
        // given
        String nickname = "nickname";
        Nickname vo = new Nickname(nickname);
        given(memberRepository.existsByNickname(vo)).willReturn(true);

        // when
        boolean exists = memberJoinService.checkDuplicateNickname(nickname);

        // then
        assertThat(exists).isTrue();
        verify(memberRepository, times(1)).existsByNickname(vo);
    }

    @DisplayName("중복되는 닉네임이 없으면 false 를 반환한다.")
    @Test
    public void checkDuplicateNickname2() {
        // given
        String nickname = "nickname";
        Nickname vo = new Nickname(nickname);
        given(memberRepository.existsByNickname(vo)).willReturn(false);

        // when
        boolean exists = memberJoinService.checkDuplicateNickname(nickname);

        // then
        assertThat(exists).isFalse();
        verify(memberRepository, times(1)).existsByNickname(vo);
    }
}
