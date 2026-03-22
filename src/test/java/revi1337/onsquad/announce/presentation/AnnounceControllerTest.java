package revi1337.onsquad.announce.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import revi1337.onsquad.announce.application.AnnounceCommandService;
import revi1337.onsquad.announce.application.AnnounceQueryService;
import revi1337.onsquad.announce.application.dto.AnnounceCreateDto;
import revi1337.onsquad.announce.application.dto.AnnounceUpdateDto;
import revi1337.onsquad.announce.application.dto.response.AnnounceResponse;
import revi1337.onsquad.announce.application.dto.response.AnnounceStates;
import revi1337.onsquad.announce.application.dto.response.AnnounceWithPinAndModifyStateResponse;
import revi1337.onsquad.announce.application.dto.response.AnnouncesWithWriteStateResponse;
import revi1337.onsquad.announce.presentation.request.AnnounceCreateRequest;
import revi1337.onsquad.announce.presentation.request.AnnounceUpdateRequest;
import revi1337.onsquad.common.PresentationLayerTestSupport;
import revi1337.onsquad.crew_member.domain.CrewRole;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;
import revi1337.onsquad.member.domain.vo.Mbti;

@WebMvcTest(AnnounceController.class)
class AnnounceControllerTest extends PresentationLayerTestSupport {

    @MockBean
    private AnnounceCommandService announceCommandService;

    @MockBean
    private AnnounceQueryService announceQueryService;

    @Nested
    @DisplayName("새로운 공지사항 생성을 문서화한다.")
    class newAnnounce {

        @Test
        @DisplayName("공지사항 생성에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            AnnounceCreateRequest request = new AnnounceCreateRequest("announce-title", "announce-content");
            doNothing().when(announceCommandService).newAnnounce(anyLong(), eq(crewId), any(AnnounceCreateDto.class));

            mockMvc.perform(post("/api/crews/{crewId}/announces", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(201))
                    .andDo(document("announce/success/new",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            requestFields(
                                    fieldWithPath("title").description("공지사항 제목"),
                                    fieldWithPath("content").description("공지사항 내용")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("공지사항 단건 조회를 문서화한다.")
    class findAnnounce {

        @Test
        @DisplayName("공지사항 상세 조회에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            Long announceId = 2L;
            LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
            AnnounceWithPinAndModifyStateResponse response = new AnnounceWithPinAndModifyStateResponse(
                    new AnnounceStates(CrewRole.OWNER, true, true),
                    announceId,
                    "announce-title",
                    "announce-content",
                    baseTime,
                    true,
                    baseTime.plusDays(1),
                    new SimpleMemberResponse(
                            1L,
                            null,
                            "nickname",
                            "introduce",
                            Mbti.ENTJ.name()
                    )
            );
            when(announceQueryService.findAnnounce(anyLong(), eq(crewId), eq(announceId))).thenReturn(response);

            mockMvc.perform(get("/api/crews/{crewId}/announces/{announceId}", crewId, announceId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("announce/success/find",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("crewId").description("크루 식별자(ID)"),
                                    parameterWithName("announceId").description("공지사항 식별자(ID)")
                            ),
                            relaxedResponseFields(fieldWithPath("data.pinnedAt").description("상단 고정된 날짜 (pinned: true 일때만 값이 있음)").optional()),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("공지사항 목록 조회를 문서화한다.")
    class findAnnounces {

        @Test
        @DisplayName("공지사항 목록 조회를 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
            AnnouncesWithWriteStateResponse response = new AnnouncesWithWriteStateResponse(
                    new AnnounceStates(true),
                    List.of(
                            new AnnounceResponse(
                                    new AnnounceStates(CrewRole.OWNER),
                                    2L,
                                    "announce-title-2",
                                    "announce-content-2",
                                    baseTime.plusHours(20),
                                    true,
                                    baseTime.plusDays(1),
                                    new SimpleMemberResponse(
                                            1L,
                                            null,
                                            "nickname-1",
                                            "introduce-1",
                                            Mbti.ENTJ.name()
                                    )
                            ),
                            new AnnounceResponse(
                                    new AnnounceStates(CrewRole.MANAGER),
                                    1L,
                                    "announce-title-1",
                                    "announce-content-1",
                                    baseTime.plusHours(18),
                                    false,
                                    null,
                                    new SimpleMemberResponse(
                                            2L,
                                            null,
                                            "nickname-2",
                                            "introduce-2",
                                            Mbti.ENTP.name()
                                    )
                            )
                    )
            );
            when(announceQueryService.findAnnounces(anyLong(), eq(crewId))).thenReturn(response);

            mockMvc.perform(get("/api/crews/{crewId}/announces", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("announce/success/finds",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("공지사항 수정을 문서화한다.")
    class updateAnnounce {

        @Test
        @DisplayName("공지사항 정보 수정에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            Long announceId = 1L;
            AnnounceUpdateRequest request = new AnnounceUpdateRequest("changed-announce-title", "changed-announce-content");
            doNothing().when(announceCommandService).updateAnnounce(anyLong(), eq(crewId), eq(announceId), any(AnnounceUpdateDto.class));

            mockMvc.perform(put("/api/crews/{crewId}/announces/{announceId}", crewId, announceId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("announce/success/update",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("crewId").description("크루 식별자(ID)"),
                                    parameterWithName("announceId").description("공지사항 식별자(ID)")
                            ),
                            requestFields(
                                    fieldWithPath("title").description("변경할 제목"),
                                    fieldWithPath("content").description("변경할 내용")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("공지사항 고정 상태 변경을 문서화한다.")
    class changeAnnouncePinned {

        @Test
        @DisplayName("공지사항 핀 고정 상태 변경에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            Long announceId = 1L;
            boolean pinState = true;
            doNothing().when(announceCommandService).changePinState(anyLong(), eq(crewId), eq(announceId), eq(pinState));

            mockMvc.perform(patch("/api/crews/{crewId}/announces/{announceId}/pin", crewId, announceId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .queryParam("state", String.valueOf(pinState))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("announce/success/pin",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("crewId").description("크루 식별자(ID)"),
                                    parameterWithName("announceId").description("공지사항 식별자(ID)")
                            ),
                            queryParameters(parameterWithName("state").description("공지사항 상단 고정 여부")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("공지사항 삭제를 문서화한다.")
    class deleteAnnounce {

        @Test
        @DisplayName("공지사항 삭제에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            Long announceId = 1L;
            doNothing().when(announceCommandService).deleteAnnounce(anyLong(), eq(crewId), eq(announceId));

            mockMvc.perform(delete("/api/crews/{crewId}/announces/{announceId}", crewId, announceId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("announce/success/delete",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("crewId").description("크루 식별자(ID)"),
                                    parameterWithName("announceId").description("공지사항 식별자(ID)")
                            ),
                            responseBody()
                    ));
        }
    }
}
