package revi1337.onsquad.squad_member.presentation;

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
import revi1337.onsquad.crew_member.application.response.CrewMemberStates;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.squad.application.response.SimpleSquadResponse;
import revi1337.onsquad.squad_member.application.SquadMemberCommandService;
import revi1337.onsquad.squad_member.application.SquadMemberQueryService;
import revi1337.onsquad.squad_member.application.response.MyParticipantSquadResponse;
import revi1337.onsquad.squad_member.application.response.SquadMemberResponse;
import revi1337.onsquad.squad_member.application.response.SquadMemberStates;

@WebMvcTest(SquadMemberController.class)
class SquadMemberControllerTest extends PresentationLayerTestSupport {

    @MockBean
    private SquadMemberCommandService squadMemberCommandService;

    @MockBean
    private SquadMemberQueryService squadMemberQueryService;

    @Nested
    @DisplayName("스쿼드 참여자 목록 조회를 문서화한다.")
    class fetchParticipants {

        @Test
        @DisplayName("사용자가 스쿼드에 참여하고 있으면 스쿼드 참여자 목록 조회에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            PageRequest pageRequest = PageRequest.of(0, 10);
            List<SquadMemberResponse> content = List.of(
                    new SquadMemberResponse(
                            SquadMemberStates.of(true, false, false),
                            LocalDate.of(2026, 1, 4).atStartOfDay(),
                            new SimpleMemberResponse(
                                    1L,
                                    null,
                                    "nickname-1",
                                    "introduce-1",
                                    Mbti.ENTP.name()
                            )
                    )
            );
            PageResponse<SquadMemberResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(squadMemberQueryService.fetchParticipants(anyLong(), eq(squadId), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/squads/{squadId}/members", squadId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("squad-member/success/finds",
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

        @Test
        @DisplayName("사용자가 스쿼드에 참여하고있지 않아도(크루 리더) 스쿼드 참여자 목록 조회에 성공한다.")
        void success2() throws Exception {
            Long squadId = 1L;
            PageRequest pageRequest = PageRequest.of(0, 10);
            List<SquadMemberResponse> content = List.of(
                    new SquadMemberResponse(
                            SquadMemberStates.of(null, null, null),
                            LocalDate.of(2026, 1, 4).atStartOfDay(),
                            new SimpleMemberResponse(
                                    1L,
                                    null,
                                    "nickname-1",
                                    "introduce-1",
                                    Mbti.ENTP.name()
                            )
                    )
            );
            PageResponse<SquadMemberResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(squadMemberQueryService.fetchParticipants(anyLong(), eq(squadId), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/squads/{squadId}/members", squadId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("squad-member/success/finds2",
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
    @DisplayName("스쿼드 리더 위임을 문서화한다.")
    class delegateLeader {

        @Test
        @DisplayName("리더 권한 위임에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            Long targetMemberId = 2L;
            doNothing().when(squadMemberCommandService).delegateLeader(anyLong(), eq(squadId), eq(targetMemberId));

            mockMvc.perform(patch("/api/squads/{squadId}/members/{targetMemberId}/leader", squadId, targetMemberId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("squad-member/success/delegate",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("squadId").description("스쿼드 식별자(ID)"),
                                    parameterWithName("targetMemberId").description("위임받을 대상 회원 식별자(ID)")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("스쿼드 탈퇴를 문서화한다.")
    class leaveSquad {

        @Test
        @DisplayName("스쿼드 탈퇴에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            doNothing().when(squadMemberCommandService).leaveSquad(anyLong(), eq(squadId));

            mockMvc.perform(delete("/api/squads/{squadId}/members/me", squadId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("squad-member/success/leave",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("squadId").description("스쿼드 식별자(ID)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("스쿼드원 추방를 문서화한다.")
    class kickOutMember {

        @Test
        @DisplayName("참여 중인 스쿼드원 추방에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            Long targetMemberId = 3L;
            doNothing().when(squadMemberCommandService).kickOutMember(anyLong(), eq(squadId), eq(targetMemberId));

            mockMvc.perform(delete("/api/squads/{squadId}/members/{targetMemberId}", squadId, targetMemberId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("squad-member/success/kick",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("squadId").description("스쿼드 식별자(ID)"),
                                    parameterWithName("targetMemberId").description("추방 대상 회원 식별자(ID)")
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("나의 스쿼드 참여 목록 조회를 문서화한다.")
    class fetchMyParticipatingSquads {

        @Test
        @DisplayName("내가 참여 중인 스쿼드 목록 조회에 성공한다.")
        void success() throws Exception {
            List<MyParticipantSquadResponse> response = List.of(
                    new MyParticipantSquadResponse(
                            new CrewMemberStates(true),
                            new MyParticipantSquadResponse.CrewWithSquadsResponse(
                                    1L,
                                    "테스트 크루",
                                    "https://image.url",
                                    new SimpleMemberResponse(
                                            1L,
                                            null,
                                            "nickname-1",
                                            "introduce-1",
                                            Mbti.ENTP.name()
                                    ),
                                    List.of(new MyParticipantSquadResponse.MySquadParticipantResponse(
                                            SquadMemberStates.of(true),
                                            LocalDate.of(2026, 1, 4).atStartOfDay(),
                                            new SimpleSquadResponse(
                                                    1L,
                                                    "스쿼드명",
                                                    10,
                                                    5,
                                                    List.of("게임"),
                                                    new SimpleMemberResponse(
                                                            1L,
                                                            null,
                                                            "nickname-1",
                                                            "introduce-1",
                                                            Mbti.ENTP.name()
                                                    )
                                            )
                                    ))
                            )
                    )
            );
            when(squadMemberQueryService.fetchMyParticipatingSquads(anyLong())).thenReturn(response);

            mockMvc.perform(get("/api/members/me/squad-participants")
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("squad-member/success/my-participating",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            responseBody()
                    ));
        }
    }
}
