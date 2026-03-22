package revi1337.onsquad.crew_request.presentation;

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
import java.time.LocalDateTime;
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
import revi1337.onsquad.crew_request.application.CrewRequestCommandService;
import revi1337.onsquad.crew_request.application.CrewRequestQueryService;
import revi1337.onsquad.crew_request.application.response.CrewRequestResponse;
import revi1337.onsquad.crew_request.application.response.CrewRequestWithCrewResponse;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;
import revi1337.onsquad.member.domain.vo.Mbti;

@WebMvcTest(CrewRequestController.class)
class CrewRequestControllerTest extends PresentationLayerTestSupport {

    @MockBean
    private CrewRequestCommandService crewRequestCommandService;

    @MockBean
    private CrewRequestQueryService crewRequestQueryService;

    @Nested
    @DisplayName("크루 가입 요청을 문서화한다.")
    class request {

        @Test
        @DisplayName("크루 가입 신청에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            doNothing().when(crewRequestCommandService).request(anyLong(), eq(crewId));

            mockMvc.perform(post("/api/crews/{crewId}/requests", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(201))
                    .andDo(document("crew-request/success/new",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루 가입 요청 수락을 문서화한다.")
    class acceptRequest {

        @Test
        @DisplayName("가입 요청 수락에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            Long requestId = 10L;
            doNothing().when(crewRequestCommandService).acceptRequest(anyLong(), eq(crewId), eq(requestId));

            mockMvc.perform(patch("/api/crews/{crewId}/requests/{requestId}", crewId, requestId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("crew-request/success/accept",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("crewId").description("크루 식별자(ID)"),
                                    parameterWithName("requestId").description("가입 요청 식별자(ID)")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루 가입 요청 거절을 문서화한다.")
    class rejectRequest {

        @Test
        @DisplayName("가입 요청 거절에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            Long requestId = 10L;
            doNothing().when(crewRequestCommandService).rejectRequest(anyLong(), eq(crewId), eq(requestId));

            mockMvc.perform(delete("/api/crews/{crewId}/requests/{requestId}", crewId, requestId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("crew-request/success/reject",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("crewId").description("크루 식별자(ID)"),
                                    parameterWithName("requestId").description("가입 요청 식별자(ID)")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("특정 크루의 가입 요청 목록 조회를 문서화한다.")
    class fetchAllRequests {

        @Test
        @DisplayName("크루 관리자의 가입 요청 목록 조회에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            PageRequest pageRequest = PageRequest.of(0, 5);
            LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
            List<CrewRequestResponse> content = getCrewRequests(baseTime);
            PageResponse<CrewRequestResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(crewRequestQueryService.fetchAllRequests(anyLong(), eq(crewId), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/crews/{crewId}/requests", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .queryParam("page", String.valueOf(pageRequest.getPageNumber()))
                            .queryParam("size", String.valueOf(pageRequest.getPageSize()))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("crew-request/success/finds",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            queryParameters(
                                    parameterWithName("page").description("페이지 번호 (0부터 시작(1과 동일))").optional(),
                                    parameterWithName("size").description("한 페이지당 개수").optional()
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("본인의 크루 가입 신청 취소를 문서화한다.")
    class cancelMyRequest {

        @Test
        @DisplayName("가입 신청 취소에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            doNothing().when(crewRequestCommandService).cancelMyRequest(anyLong(), eq(crewId));

            mockMvc.perform(delete("/api/crews/{crewId}/requests/me", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("crew-request/success/cancel",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("나의 모든 크루 가입 신청 내역 조회를 문서화한다.")
    class fetchAllCrewRequests {

        @Test
        @DisplayName("내 신청 내역 조회에 성공한다.")
        void success() throws Exception {
            PageRequest pageRequest = PageRequest.of(0, 5);
            LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
            List<CrewRequestWithCrewResponse> content = getMyRequests(baseTime);
            PageResponse<CrewRequestWithCrewResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(crewRequestQueryService.fetchAllCrewRequests(anyLong(), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/members/me/crew-requests")
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .queryParam("page", String.valueOf(pageRequest.getPageNumber()))
                            .queryParam("size", String.valueOf(pageRequest.getPageSize()))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("crew-request/success/my-requests",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            queryParameters(
                                    parameterWithName("page").description("페이지 번호 (0부터 시작(1과 동일))").optional(),
                                    parameterWithName("size").description("한 페이지당 개수").optional()
                            ),
                            responseBody()
                    ));
        }
    }

    private List<CrewRequestResponse> getCrewRequests(LocalDateTime baseTime) {
        return List.of(
                new CrewRequestResponse(
                        12L,
                        baseTime.plusHours(6),
                        new SimpleMemberResponse(
                                1L,
                                null,
                                "nickname-1",
                                "introduce-1",
                                Mbti.ENTJ.name()
                        )
                ),
                new CrewRequestResponse(
                        11L,
                        baseTime.plusHours(3),
                        new SimpleMemberResponse(
                                2L,
                                null,
                                "nickname-2",
                                "introduce-2",
                                Mbti.ENTP.name()
                        )
                )
        );
    }

    private List<CrewRequestWithCrewResponse> getMyRequests(LocalDateTime baseTime) {
        return List.of(
                new CrewRequestWithCrewResponse(
                        2L,
                        baseTime.plusHours(5),
                        new SimpleCrewResponse(
                                1L,
                                "crew-name-1",
                                "crew-introduce-1",
                                "crew-detail-1",
                                "",
                                new SimpleMemberResponse(
                                        1L,
                                        null,
                                        "nickname-1",
                                        "introduce-1",
                                        Mbti.ENTJ.name()
                                )
                        )
                ),
                new CrewRequestWithCrewResponse(
                        1L,
                        baseTime.plusHours(4),
                        new SimpleCrewResponse(
                                2L,
                                "crew-name-2",
                                "crew-introduce-2",
                                "crew-detail-2",
                                "",
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
