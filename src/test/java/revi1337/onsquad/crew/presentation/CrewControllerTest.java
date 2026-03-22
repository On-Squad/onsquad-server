package revi1337.onsquad.crew.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestPartFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static revi1337.onsquad.common.fixture.CrewFixture.CREW_KAKAO_LINK_VALUE;
import static revi1337.onsquad.common.fixture.MultipartFixture.PNG_BYTES;
import static revi1337.onsquad.common.fixture.MultipartFixture.jsonMuliPart;
import static revi1337.onsquad.common.fixture.MultipartFixture.pngMuliPart;
import static revi1337.onsquad.hashtag.domain.HashtagType.ACTIVE;
import static revi1337.onsquad.hashtag.domain.HashtagType.FOODIE;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import revi1337.onsquad.common.PresentationLayerTestSupport;
import revi1337.onsquad.common.dto.DuplicateResponse;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.crew.application.CrewCommandServiceFacade;
import revi1337.onsquad.crew.application.CrewQueryService;
import revi1337.onsquad.crew.application.dto.CrewCreateDto;
import revi1337.onsquad.crew.application.dto.CrewUpdateDto;
import revi1337.onsquad.crew.application.dto.response.CrewResponse;
import revi1337.onsquad.crew.application.dto.response.CrewStates;
import revi1337.onsquad.crew.application.dto.response.CrewWithParticipantStateResponse;
import revi1337.onsquad.crew.presentation.request.CrewCreateRequest;
import revi1337.onsquad.crew.presentation.request.CrewUpdateRequest;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;
import revi1337.onsquad.member.domain.vo.Mbti;

@WebMvcTest(CrewController.class)
class CrewControllerTest extends PresentationLayerTestSupport {

    @MockBean
    private CrewCommandServiceFacade commandServiceFacade;

    @MockBean
    private CrewQueryService crewQueryService;

    @Nested
    @DisplayName("크루 이름 중복 검사를 문서화한다.")
    class checkNameDuplicate {

        @Test
        @DisplayName("크루 이름 중복 검사에 성공한다.")
        void success() throws Exception {
            String crewName = "crew-name";
            DuplicateResponse response = new DuplicateResponse(true);
            when(crewQueryService.checkNameDuplicate(anyString())).thenReturn(response);

            mockMvc.perform(get("/api/crews/check")
                            .param("name", crewName)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("crew/success/name-duplicate",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            queryParameters(parameterWithName("name").description("검사할 크루 이름")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("새로운 크루 생성을 문서화한다.")
    class newCrew {

        @Test
        @DisplayName("새로운 크루 생성에 성공한다.")
        void success() throws Exception {
            CrewCreateRequest request = new CrewCreateRequest(
                    "crew-name",
                    "introduce",
                    "detail",
                    List.of(ACTIVE, FOODIE),
                    CREW_KAKAO_LINK_VALUE
            );
            MockMultipartFile jsonPart = jsonMuliPart("request", "request", objectMapper.writeValueAsString(request));
            MockMultipartFile filePart = pngMuliPart("file", "dummy.png", PNG_BYTES);
            doNothing().when(commandServiceFacade).newCrew(anyLong(), any(CrewCreateDto.class), any(MultipartFile.class));

            mockMvc.perform(multipart("/api/crews")
                            .file(jsonPart)
                            .file(filePart)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(MULTIPART_FORM_DATA_VALUE))
                    .andExpect(jsonPath("$.status").value(201))
                    .andDo(document("crew/success/new",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            requestParts(
                                    partWithName("file").description("크루 이미지 파일 (Content-Type: multipart/form-data)"),
                                    partWithName("request").description("크루 생성 요청 JSON 데이터 (Content-Type: application/json)")
                            ),
                            requestPartFields(
                                    "request",
                                    fieldWithPath("name").description("크루 이름"),
                                    fieldWithPath("introduce").description("크루 한줄 소개"),
                                    fieldWithPath("detail").description("크루 상세 정보"),
                                    fieldWithPath("hashtags").description("크루 해시태그"),
                                    fieldWithPath("kakaoLink").description("크루 오픈 카카오톡 링크")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루 상세 정보 조회를 문서화한다.")
    class findCrew {

        @Test
        @DisplayName("토큰이 있는 경우에 크루 조회에 성공한다.")
        void success1() throws Exception {
            Long crewId = 1L;
            CrewWithParticipantStateResponse response = new CrewWithParticipantStateResponse(
                    new CrewStates(Boolean.TRUE, Boolean.FALSE),
                    crewId,
                    "crew-name",
                    "crew-introduce",
                    "crew-detail",
                    "",
                    "https://kakao-link",
                    List.of(HashtagType.ACTIVE.getText()),
                    1L,
                    new SimpleMemberResponse(
                            1L,
                            null,
                            "nickname",
                            "introduce",
                            Mbti.ENTJ.name()
                    )
            );
            when(crewQueryService.findCrewById(anyLong(), eq(crewId))).thenReturn(response);

            mockMvc.perform(get("/api/crews/{crewId}", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("crew/success/find-auth",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보").optional()),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            responseBody()
                    ));
        }

        @Test
        @DisplayName("토큰이 없는 경우에도 크루 조회에 성공한다.")
        void success2() throws Exception {
            Long crewId = 1L;
            CrewWithParticipantStateResponse response = new CrewWithParticipantStateResponse(
                    new CrewStates(null, null),
                    crewId,
                    "crew-name",
                    "crew-introduce",
                    "crew-detail",
                    "",
                    "https://kakao-link",
                    List.of(ACTIVE.getText()),
                    1L,
                    new SimpleMemberResponse(
                            1L,
                            null,
                            "nickname",
                            "introduce",
                            Mbti.ENTJ.name()
                    )
            );
            when(crewQueryService.findCrewById(nullable(Long.class), eq(crewId))).thenReturn(response);

            mockMvc.perform(get("/api/crews/{crewId}", crewId)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("crew/success/find-no-auth",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루들 조회를 문서화한다.")
    class fetchCrewsByName {

        @Test
        @DisplayName("크루들 조회를 성공한다.")
        void success() throws Exception {
            String crewName = "crew-name";
            PageRequest pageRequest = PageRequest.of(0, 2);
            List<CrewResponse> content = List.of(new CrewResponse(
                    1L,
                    "crew-name-1",
                    "crew-introduce-1",
                    "crew-detail-1",
                    "",
                    "https://kakao-link",
                    List.of(HashtagType.ACTIVE.getText()),
                    1L,
                    new SimpleMemberResponse(
                            1L,
                            null,
                            "nickname",
                            "introduce",
                            Mbti.ENTJ.name()
                    )
            ));
            PageResponse<CrewResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(crewQueryService.fetchCrewsByName(eq(crewName), eq(pageRequest))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/crews")
                            .param("name", crewName)
                            .param("page", String.valueOf(pageRequest.getPageNumber()))
                            .param("size", String.valueOf(pageRequest.getPageSize()))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("crew/success/finds",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("name").description("검색할 크루 이름").optional(),
                                    parameterWithName("page").description("페이지 번호 (0부터 시작(1과 동일))").optional(),
                                    parameterWithName("size").description("한 페이지당 개수").optional()
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루 정보 수정을 문서화한다.")
    class updateCrew {

        @Test
        @DisplayName("크루 정보 수정에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            CrewUpdateRequest request = new CrewUpdateRequest(
                    "changed-name",
                    "changed-introduce",
                    "changed-introduce-detail",
                    List.of(HashtagType.ESCAPE),
                    "https://changed-kakao-link"
            );
            doNothing().when(commandServiceFacade).updateCrew(any(), eq(crewId), any(CrewUpdateDto.class));

            mockMvc.perform(put("/api/crews/{crewId}", 1L)
                            .content(objectMapper.writeValueAsString(request))
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("crew/success/update",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 아이디")),
                            requestFields(
                                    fieldWithPath("name").description("변경할 크루 이름"),
                                    fieldWithPath("introduce").description("변경할 크루 한줄 소개"),
                                    fieldWithPath("detail").description("변경할 크루 상세 정보"),
                                    fieldWithPath("hashtags").description("변경할 크루 해시태그"),
                                    fieldWithPath("kakaoLink").description("변경할 크루 오픈 카카오 링크")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루 삭제를 문서화한다.")
    class deleteCrew {

        @Test
        @DisplayName("크루 삭제에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            doNothing().when(commandServiceFacade).deleteCrew(any(), eq(crewId));

            mockMvc.perform(delete("/api/crews/{crewId}", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("crew/success/delete",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루 이미지 수정을 문서화한다.")
    class updateCrewImage {

        @Test
        @DisplayName("크루 이미지 수정에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            MockMultipartFile filePart = pngMuliPart("file", "test.png", PNG_BYTES);
            doNothing().when(commandServiceFacade).updateImage(anyLong(), eq(crewId), any(MultipartFile.class));

            mockMvc.perform(multipart("/api/crews/{crewId}/image", crewId)
                            .file(filePart)
                            .with(request -> {
                                request.setMethod(PATCH.name());
                                return request;
                            })
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(MULTIPART_FORM_DATA_VALUE))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("crew/success/update-image",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            requestParts(partWithName("file").description("크루 이미지 파일 (Content-Type: multipart/form-data)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루 이미지 삭제를 문서화한다.")
    class deleteCrewImage {

        @Test
        @DisplayName("크루 이미지 삭제에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            doNothing().when(commandServiceFacade).deleteImage(anyLong(), eq(crewId));

            mockMvc.perform(delete("/api/crews/{crewId}/image", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("crew/success/delete-image",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            responseBody()
                    ));
        }
    }
}
