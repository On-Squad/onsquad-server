package revi1337.onsquad.member.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import revi1337.onsquad.factory.MemberFactory;
import revi1337.onsquad.member.application.MemberJoinService;
import revi1337.onsquad.member.domain.Member;
import revi1337.onsquad.member.domain.MemberRepository;
import revi1337.onsquad.member.domain.redis.RedisMailRepository;

import java.time.Duration;

import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("회원가입 api 테스트")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class MemberJoinControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @SpyBean private MemberJoinService memberJoinService;
    @Autowired private RedisMailRepository redisMailRepository;
    @Autowired private MemberRepository memberRepository;

    private static final String TEST_EMAIL = "test@email.com";
    private static final String TEST_AUTH_CODE = "1111";

    @DisplayName("인증코드 발송이 정상적으로 동작하는지 확인한다.")
    @Test
    public void sendAuthCodeToEmail() throws Exception {
        // given
        String testEmail = "david122123@gmail.com";
        willDoNothing().given(memberJoinService).sendAuthCodeToEmail(testEmail);

        // when & then
        mockMvc.perform(
                        get("/api/v1/auth/send")
                                .queryParam("email", testEmail)
                                .contentType(APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk());

        verify(memberJoinService, times(1)).sendAuthCodeToEmail(testEmail);
    }

    @DisplayName("인증코드 검증이 성공하면 true 를 반환한다.")
    @Test
    public void verifyAuthCode() throws Exception {
        // given
        redisMailRepository.saveAuthCode(TEST_EMAIL, TEST_AUTH_CODE, Duration.ofMinutes(5));

        // when & then
        mockMvc.perform(
                        get("/api/v1/auth/valid")
                                .queryParam("email", TEST_EMAIL)
                                .queryParam("authCode", TEST_AUTH_CODE)
                                .contentType(APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @DisplayName("인증코드 검증에 실패하면 false 를 반환한다.")
    @Test
    public void verifyAuthCode2() throws Exception {
        // given
        redisMailRepository.saveAuthCode("another_email", TEST_AUTH_CODE, Duration.ofMinutes(5));

        // when & then
        mockMvc.perform(
                        get("/api/v1/auth/valid")
                                .queryParam("email", TEST_EMAIL)
                                .queryParam("authCode", TEST_AUTH_CODE)
                                .contentType(APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.data.valid").value(false));
    }

    @DisplayName("닉네임 중복이 확인되면 true 를 반환한다.")
    @Test
    public void checkDuplicateNickname() throws Exception {
        // given
        Member member = MemberFactory.defaultMember().build();
        memberRepository.save(member);

        // when & then
        mockMvc.perform(
                        get("/api/v1/auth/check")
                                .queryParam("nickname", "nickname")
                                .contentType(APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(true));
    }

    @DisplayName("닉네임 중복이 확인되지 않으면 false 를 반환한다.")
    @Test
    public void checkDuplicateNickname2() throws Exception {
        // given
        String nickname = "nickname";

        // when & then
        mockMvc.perform(
                        get("/api/v1/auth/check")
                                .queryParam("nickname", nickname)
                                .contentType(APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(false));
    }
}

