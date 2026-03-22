package revi1337.onsquad.squad_request.presentation;

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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import revi1337.onsquad.common.PresentationLayerTestSupport;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.crew.application.dto.response.SimpleCrewResponse;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.squad.application.response.SimpleSquadResponse;
import revi1337.onsquad.squad_request.application.SquadRequestCommandService;
import revi1337.onsquad.squad_request.application.SquadRequestQueryService;
import revi1337.onsquad.squad_request.application.response.MySquadRequestResponse;
import revi1337.onsquad.squad_request.application.response.SquadRequestResponse;

@WebMvcTest(SquadRequestController.class)
class SquadRequestControllerTest extends PresentationLayerTestSupport {

    @MockBean
    private SquadRequestCommandService squadRequestCommandService;

    @MockBean
    private SquadRequestQueryService squadRequestQueryService;

    @Nested
    @DisplayName("스쿼드 참가 신청을 문서화한다.")
    class request {

        @Test
        @DisplayName("스쿼드 참가 신청에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            doNothing().when(squadRequestCommandService).request(anyLong(), eq(squadId));

            mockMvc.perform(post("/api/squads/{squadId}/requests", squadId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(201))
                    .andDo(document("squad-request/success/new",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("squadId").description("스쿼드 식별자(ID)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("스쿼드 참가 신청 수락을 문서화한다.")
    class acceptRequest {

        @Test
        @DisplayName("참가 신청 수락에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            Long requestId = 10L;
            doNothing().when(squadRequestCommandService).acceptRequest(anyLong(), eq(squadId), eq(requestId));

            mockMvc.perform(patch("/api/squads/{squadId}/requests/{requestId}", squadId, requestId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("squad-request/success/accept",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("squadId").description("스쿼드 식별자(ID)"),
                                    parameterWithName("requestId").description("참가 신청 식별자(ID)")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("스쿼드 참가 신청 거절을 문서화한다.")
    class rejectRequest {

        @Test
        @DisplayName("참가 신청 거절에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            Long requestId = 10L;
            doNothing().when(squadRequestCommandService).rejectRequest(anyLong(), eq(squadId), eq(requestId));

            mockMvc.perform(delete("/api/squads/{squadId}/requests/{requestId}", squadId, requestId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("squad-request/success/reject",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("squadId").description("스쿼드 식별자(ID)"),
                                    parameterWithName("requestId").description("참가 신청 식별자(ID)")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("본인의 스쿼드 신청 취소를 문서화한다.")
    class cancelMyRequest {

        @Test
        @DisplayName("참가 신청 취소에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            when(squadRequestCommandService.cancelMyRequest(anyLong(), eq(squadId))).thenReturn(1);

            mockMvc.perform(delete("/api/squads/{squadId}/requests/me", squadId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("squad-request/success/cancel",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("squadId").description("스쿼드 식별자(ID)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("스쿼드별 신청 목록 조회를 문서화한다.")
    class fetchAllRequests {

        @Test
        @DisplayName("스쿼드 리더의 신청 목록 조회에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            PageRequest pageRequest = PageRequest.of(0, 10);
            List<SquadRequestResponse> content = List.of(
                    new SquadRequestResponse(
                            10L,
                            LocalDate.of(2026, 1, 4).atStartOfDay(),
                            new SimpleMemberResponse(
                                    1L,
                                    null,
                                    "nickname-1",
                                    "introduce-1",
                                    Mbti.ENTJ.name()
                            )
                    )
            );
            PageResponse<SquadRequestResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(squadRequestQueryService.fetchAllRequests(anyLong(), eq(squadId), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/squads/{squadId}/requests", squadId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("squad-request/success/finds",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("squadId").description("스쿼드 식별자(ID)")),
                            queryParameters(
                                    parameterWithName("page").description("페이지 번호").optional(),
                                    parameterWithName("size").description("한 페이지당 개수").optional()
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("나의 모든 스쿼드 신청 내역 조회를 문서화한다.")
    class fetchMyRequests {

        @Test
        @DisplayName("내 신청 내역 조회에 성공한다.")
        void success() throws Exception {
            PageRequest pageRequest = PageRequest.of(0, 10);
            List<MySquadRequestResponse> content = getMySquadRequestResponses();
            PageResponse<MySquadRequestResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(squadRequestQueryService.fetchMyRequests(anyLong(), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/members/me/squad-requests")
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("squad-request/success/my-requests",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            queryParameters(
                                    parameterWithName("page").description("페이지 번호").optional(),
                                    parameterWithName("size").description("한 페이지당 개수").optional()
                            ),
                            responseBody()
                    ));
        }

        private List<MySquadRequestResponse> getMySquadRequestResponses() {
            return List.of(
                    new MySquadRequestResponse(
                            100L,
                            LocalDate.of(2026, 1, 4).atStartOfDay(),
                            new SimpleCrewResponse(
                                    1L,
                                    "크루명",
                                    "소개",
                                    "상세",
                                    "",
                                    new SimpleMemberResponse(
                                            1L,
                                            null,
                                            "nickname-1",
                                            "introduce-1",
                                            Mbti.ENTJ.name()
                                    )
                            ),
                            new SimpleSquadResponse(
                                    1L,
                                    "스쿼드명",
                                    10,
                                    5,
                                    List.of("게임"),
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
        }
    }
}
