package revi1337.onsquad.member.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static revi1337.onsquad.common.fixture.MemberFixture.ANDONG_PASSWORD_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.KWANGWON_PASSWORD_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_ADDRESS_DETAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_ADDRESS_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_EMAIL_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_INTRODUCE_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_KAKAO_LINK;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_MBTI_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_NICKNAME_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_PASSWORD_VALUE;
import static revi1337.onsquad.common.fixture.MemberFixture.REVI_PROFILE_IMAGE_LINK;
import static revi1337.onsquad.common.fixture.MultipartFixture.PNG_BYTES;
import static revi1337.onsquad.common.fixture.MultipartFixture.pngMuliPart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import revi1337.onsquad.common.PresentationLayerTestSupport;
import revi1337.onsquad.common.dto.DuplicateResponse;
import revi1337.onsquad.member.application.MemberCommandServiceFacade;
import revi1337.onsquad.member.application.MemberQueryService;
import revi1337.onsquad.member.application.dto.MemberCreateDto;
import revi1337.onsquad.member.application.dto.response.MemberResponse;
import revi1337.onsquad.member.domain.vo.UserType;
import revi1337.onsquad.member.presentation.request.MemberCreateRequest;
import revi1337.onsquad.member.presentation.request.MemberPasswordUpdateRequest;
import revi1337.onsquad.member.presentation.request.MemberUpdateRequest;

@WebMvcTest(MemberController.class)
class MemberControllerTest extends PresentationLayerTestSupport {

    @MockBean
    private MemberQueryService queryService;

    @MockBean
    private MemberCommandServiceFacade commandServiceFacade;

    @Nested
    @DisplayName("닉네임 중복 검사를 문서화한다.")
    class checkDuplicateNickname {

        @Test
        @DisplayName("사용자 닉네임 중복 검사에 성공한다.")
        void success() throws Exception {
            when(queryService.checkDuplicateNickname(REVI_NICKNAME_VALUE)).thenReturn(new DuplicateResponse(false));

            mockMvc.perform(get("/api/members/check-nickname")
                            .param("value", REVI_NICKNAME_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.duplicate").value(false))
                    .andDo(document("member/success/check-nickname",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("value").description("검사할 Nickname")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("이메일 중복 검사를 문서화한다.")
    class checkDuplicateEmail {

        @Test
        @DisplayName("사용자 이메일 중복 검사에 성공한다.")
        void success() throws Exception {
            when(queryService.checkDuplicateEmail(REVI_EMAIL_VALUE)).thenReturn(new DuplicateResponse(false));

            mockMvc.perform(get("/api/members/check-email")
                            .param("value", REVI_EMAIL_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.duplicate").value(false))
                    .andDo(document("member/success/check-email",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("value").description("검사할 Email")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("사용자 생성을 문서화한다.")
    class newMember {

        @Test
        @DisplayName("사용자 생성에 성공한다.")
        void success() throws Exception {
            MemberCreateRequest request = new MemberCreateRequest(
                    REVI_EMAIL_VALUE,
                    REVI_PASSWORD_VALUE,
                    REVI_PASSWORD_VALUE,
                    REVI_NICKNAME_VALUE,
                    REVI_ADDRESS_VALUE,
                    REVI_ADDRESS_DETAIL_VALUE
            );
            doNothing().when(commandServiceFacade).newMember(any(MemberCreateDto.class));

            mockMvc.perform(post("/api/members")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(201))
                    .andDo(document("member/success/new",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("email").description("이메일"),
                                    fieldWithPath("password").description("비밀번호"),
                                    fieldWithPath("passwordConfirm").description("비밀번호 확인"),
                                    fieldWithPath("nickname").description("닉네임"),
                                    fieldWithPath("address").description("주소"),
                                    fieldWithPath("addressDetail").description("상세 주소")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("내 정보를 문서화한다.")
    class findMember {

        @Test
        @DisplayName("내 정보 조회에 성공한다.")
        void success() throws Exception {
            MemberResponse response = new MemberResponse(
                    1L,
                    REVI_EMAIL_VALUE,
                    REVI_NICKNAME_VALUE,
                    REVI_INTRODUCE_VALUE,
                    REVI_MBTI_VALUE,
                    REVI_KAKAO_LINK,
                    REVI_PROFILE_IMAGE_LINK,
                    UserType.GENERAL.getText(),
                    REVI_ADDRESS_VALUE,
                    REVI_ADDRESS_DETAIL_VALUE
            );
            when(queryService.findMember(any())).thenReturn(response);

            mockMvc.perform(get("/api/members/me")
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andDo(document("member/success/me",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("사용자 업데이트를 문서화한다.")
    class updateMember {

        @Test
        @DisplayName("사용자 업데이트를 성공한다.")
        void success() throws Exception {
            MemberUpdateRequest request = new MemberUpdateRequest(
                    REVI_NICKNAME_VALUE,
                    REVI_INTRODUCE_VALUE,
                    REVI_MBTI_VALUE,
                    REVI_KAKAO_LINK,
                    REVI_ADDRESS_VALUE,
                    REVI_ADDRESS_DETAIL_VALUE
            );
            doNothing().when(commandServiceFacade).updateProfile(any(), any());

            mockMvc.perform(put("/api/members/me")
                            .content(objectMapper.writeValueAsString(request))
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("member/success/update",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("사용자 탈퇴를 문서화한다.")
    class deleteMember {

        @Test
        @DisplayName("사용자 탈퇴에 성공한다.")
        void success() throws Exception {
            doNothing().when(commandServiceFacade).deleteMember(any());

            mockMvc.perform(delete("/api/members/me")
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("member/success/delete",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("사용자 비밀번호 변경을 문서화한다.")
    class updatePassword {

        @Test
        @DisplayName("사용자 비밀번호 변경에 성공한다.")
        void success() throws Exception {
            MemberPasswordUpdateRequest request = new MemberPasswordUpdateRequest(
                    REVI_PASSWORD_VALUE,
                    ANDONG_PASSWORD_VALUE,
                    ANDONG_PASSWORD_VALUE
            );
            doNothing().when(commandServiceFacade).updatePassword(any(), any());

            mockMvc.perform(patch("/api/members/me/password")
                            .content(objectMapper.writeValueAsString(request))
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("member/success/update-password",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            requestFields(
                                    fieldWithPath("currentPassword").description("기존 비밀번호"),
                                    fieldWithPath("newPassword").description("새로운 비밀번호"),
                                    fieldWithPath("newPasswordConfirm").description("새로운 비밀번호 확인")
                            ),
                            responseBody()
                    ));
        }

        @Test
        @DisplayName("새로운 비밀번호와 새로운 비밀번호 확인란이 일치하지 않으면 실패한다.")
        void fail() throws Exception {
            MemberPasswordUpdateRequest request = new MemberPasswordUpdateRequest(
                    REVI_PASSWORD_VALUE,
                    ANDONG_PASSWORD_VALUE,
                    KWANGWON_PASSWORD_VALUE
            );
            doNothing().when(commandServiceFacade).updatePassword(any(), any());

            mockMvc.perform(patch("/api/members/me/password")
                            .content(objectMapper.writeValueAsString(request))
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("사용자 이미지 변경을 문서화한다.")
    class updateImage {

        @Test
        @DisplayName("사용자 이미지 변경에 성공한다.")
        void success() throws Exception {
            MockMultipartFile filePart = pngMuliPart("file", "dummy.png", PNG_BYTES);
            doNothing().when(commandServiceFacade).updateImage(any(), any());

            mockMvc.perform(multipart("/api/members/me/image")
                            .file(filePart)
                            .with(request -> {
                                request.setMethod("PATCH");
                                return request;
                            })
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(MULTIPART_FORM_DATA_VALUE))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("member/success/update-image",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            requestParts(partWithName("file").description("사용자 이미지 파일 (Content-Type: multipart/form-data)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("사용자 이미지 삭제를 문서화한다.")
    class deleteImage {

        @Test
        @DisplayName("사용자 이미지 삭제에 성공한다.")
        void success() throws Exception {
            mockMvc.perform(delete("/api/members/me/image")
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("member/success/delete-image",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            responseBody()
                    ));
        }
    }
}
