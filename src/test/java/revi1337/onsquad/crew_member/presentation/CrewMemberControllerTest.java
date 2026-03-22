package revi1337.onsquad.crew_member.presentation;

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
import revi1337.onsquad.crew_member.application.CrewMemberCommandService;
import revi1337.onsquad.crew_member.application.CrewMemberQueryService;
import revi1337.onsquad.crew_member.application.response.CrewMemberResponse;
import revi1337.onsquad.crew_member.application.response.CrewMemberStates;
import revi1337.onsquad.crew_member.application.response.MyParticipantCrewResponse;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;
import revi1337.onsquad.member.domain.vo.Mbti;

@WebMvcTest(CrewMemberController.class)
class CrewMemberControllerTest extends PresentationLayerTestSupport {

    @MockBean
    private CrewMemberCommandService crewMemberCommandService;

    @MockBean
    private CrewMemberQueryService crewMemberQueryService;

    @Nested
    @DisplayName("크루 참여자 목록 조회를 문서화한다.")
    class fetchParticipants {

        @Test
        @DisplayName("특정 크루의 참여자 목록 조회에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
            List<CrewMemberResponse> content = getCrewMemberResponses(baseTime);
            PageRequest pageRequest = PageRequest.of(0, 10);
            PageResponse<CrewMemberResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(crewMemberQueryService.fetchParticipants(anyLong(), eq(crewId), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/crews/{crewId}/members", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .queryParam("page", String.valueOf(pageRequest.getPageNumber()))
                            .queryParam("size", String.valueOf(pageRequest.getPageSize()))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("crew-member/success/finds",
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
    @DisplayName("방장 권한 위임을 문서화한다.")
    class delegateOwner {

        @Test
        @DisplayName("방장 권한 위임에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            Long targetMemberId = 2L;
            doNothing().when(crewMemberCommandService).delegateOwner(anyLong(), eq(crewId), eq(targetMemberId));

            mockMvc.perform(patch("/api/crews/{crewId}/members/{targetMemberId}/owner", crewId, targetMemberId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("crew-member/success/delegate",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("crewId").description("크루 식별자(ID)"),
                                    parameterWithName("targetMemberId").description("권한을 위임받을 멤버 식별자(ID)")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루 탈퇴를 문서화한다.")
    class leaveCrew {

        @Test
        @DisplayName("크루 탈퇴에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            doNothing().when(crewMemberCommandService).leaveCrew(anyLong(), eq(crewId));

            mockMvc.perform(delete("/api/crews/{crewId}/members/me", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("crew-member/success/leave",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루 멤버 추방를 문서화한다.")
    class kickOutMember {

        @Test
        @DisplayName("멤버 추방에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            Long targetMemberId = 3L;
            doNothing().when(crewMemberCommandService).kickOutMember(anyLong(), eq(crewId), eq(targetMemberId));

            mockMvc.perform(delete("/api/crews/{crewId}/members/{targetMemberId}", crewId, targetMemberId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("crew-member/success/kick",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("crewId").description("크루 식별자(ID)"),
                                    parameterWithName("targetMemberId").description("강퇴할 멤버 식별자(ID)")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("내가 참여 중인 크루 목록 조회를 문서화한다.")
    class fetchMyParticipatingCrews {

        @Test
        @DisplayName("내 참여 크루 목록 조회에 성공한다.")
        void success() throws Exception {
            LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
            List<MyParticipantCrewResponse> content = getMyParticipantCrewResponses(baseTime);
            PageRequest pageRequest = PageRequest.of(0, 10);
            PageResponse<MyParticipantCrewResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(crewMemberQueryService.fetchMyParticipatingCrews(anyLong(), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/members/me/crew-participants")
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .queryParam("page", String.valueOf(pageRequest.getPageNumber()))
                            .queryParam("size", String.valueOf(pageRequest.getPageSize()))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("crew-member/success/my-participates",
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

    private List<CrewMemberResponse> getCrewMemberResponses(LocalDateTime baseTime) {
        return List.of(
                new CrewMemberResponse(
                        new CrewMemberStates(false, true, true),
                        baseTime.plusHours(6),
                        new SimpleMemberResponse(
                                2L,
                                null,
                                "nickname-2",
                                "introduce-2",
                                Mbti.ENTP.name()
                        )
                ),
                new CrewMemberResponse(
                        new CrewMemberStates(true, false, false),
                        baseTime.plusHours(3),
                        new SimpleMemberResponse(
                                1L,
                                null,
                                "nickname-1",
                                "introduce-1",
                                Mbti.ENTJ.name()
                        )
                )
        );
    }

    private List<MyParticipantCrewResponse> getMyParticipantCrewResponses(LocalDateTime baseTime) {
        return List.of(
                new MyParticipantCrewResponse(
                        new CrewMemberStates(true),
                        baseTime.plusHours(6),
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
                new MyParticipantCrewResponse(
                        new CrewMemberStates(true),
                        baseTime.plusHours(5),
                        new SimpleCrewResponse(
                                2L,
                                "crew-name-2",
                                "crew-introduce-2",
                                "crew-detail-2",
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
                new MyParticipantCrewResponse(
                        new CrewMemberStates(false),
                        baseTime.plusHours(8),
                        new SimpleCrewResponse(
                                3L,
                                "crew-name-3",
                                "crew-introduce-3",
                                "crew-detail-3",
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
