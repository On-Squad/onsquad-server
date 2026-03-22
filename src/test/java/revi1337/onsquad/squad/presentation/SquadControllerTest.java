package revi1337.onsquad.squad.presentation;

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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.common.PresentationLayerTestSupport;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.squad.application.SquadCommandService;
import revi1337.onsquad.squad.application.SquadQueryService;
import revi1337.onsquad.squad.application.response.SquadResponse;
import revi1337.onsquad.squad.application.response.SquadStates;
import revi1337.onsquad.squad.application.response.SquadWithLeaderStateResponse;
import revi1337.onsquad.squad.application.response.SquadWithStatesResponse;
import revi1337.onsquad.squad.domain.model.SquadCreateSpec;
import revi1337.onsquad.squad.presentation.request.SquadCreateRequest;

@WebMvcTest(SquadController.class)
class SquadControllerTest extends PresentationLayerTestSupport {

    @MockBean
    private SquadCommandService squadCommandService;

    @MockBean
    private SquadQueryService squadQueryService;

    @Nested
    @DisplayName("새로운 스쿼드 생성을 문서화한다.")
    class newSquad {

        @Test
        @DisplayName("새로운 스쿼드 생성에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            SquadCreateRequest request = new SquadCreateRequest(
                    "title",
                    "content",
                    20,
                    "addr",
                    "addr-detail",
                    List.of(CategoryType.GAME),
                    "kakao-link",
                    "discord-link"
            );
            when(squadCommandService.newSquad(anyLong(), eq(crewId), any(SquadCreateSpec.class))).thenReturn(1L);

            mockMvc.perform(post("/api/crews/{crewId}/squads", crewId)
                            .content(objectMapper.writeValueAsString(request))
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(201))
                    .andDo(document("squad/success/new",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            requestFields(
                                    fieldWithPath("title").description("스쿼드 제목"),
                                    fieldWithPath("content").description("스쿼드 상세 내용"),
                                    fieldWithPath("capacity").description("스쿼드 최대 정원"),
                                    fieldWithPath("address").description("스쿼드 활동 장소 (기본 주소)").optional(),
                                    fieldWithPath("addressDetail").description("스쿼드 활동 장소 (상세 주소)").optional(),
                                    fieldWithPath("categories").description("스쿼드 카테고리 목록"),
                                    fieldWithPath("kakaoLink").description("스쿼드 오픈 카카오톡 링크").optional(),
                                    fieldWithPath("discordLink").description("스쿼드 디스코드 링크").optional()
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루 내 스쿼드 목록 조회를 문서화한다.")
    class fetchSquads {

        @Test
        @DisplayName("크루 내 스쿼드 목록 조회에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            CategoryType category = CategoryType.GAME;
            PageRequest pageRequest = PageRequest.of(0, 10);
            List<SquadResponse> content = List.of(new SquadResponse(
                    1L,
                    "title",
                    "content",
                    8,
                    7,
                    "",
                    "",
                    "https://kakao-link",
                    "https://discord-link",
                    List.of(category.getText(), CategoryType.MOVIE.getText()),
                    new SimpleMemberResponse(
                            1L,
                            null,
                            "nickname",
                            "introduce",
                            Mbti.ENTJ.name()
                    )
            ));
            PageResponse<SquadResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(squadQueryService.fetchSquadsByCrewId(anyLong(), eq(crewId), eq(category), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/crews/{crewId}/squads", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .param("category", category.getText())
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("squad/success/finds",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            queryParameters(
                                    parameterWithName("category").description("검색할 카테고리").optional(),
                                    parameterWithName("page").description("페이지 번호").optional(),
                                    parameterWithName("size").description("한 페이지당 개수").optional()
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("스쿼드 단건 상세 조회를 문서화한다.")
    class fetchSquad {

        @Test
        @DisplayName("스쿼드에 참여하고 있는 경우 조회에 성공한다.")
        void success1() throws Exception {
            Long squadId = 1L;
            SquadWithStatesResponse response = new SquadWithStatesResponse(
                    SquadStates.of(
                            null,
                            true,
                            true,
                            true,
                            false,
                            true
                    ),
                    squadId,
                    "title",
                    "content",
                    8,
                    7,
                    "",
                    "",
                    "https://kakao-link",
                    "https://discord-link",
                    List.of(CategoryType.GAME.getText(), CategoryType.MOVIE.getText()),
                    new SimpleMemberResponse(
                            1L,
                            null,
                            "nickname",
                            "introduce",
                            Mbti.ENTJ.name()
                    )
            );
            when(squadQueryService.fetchSquad(anyLong(), eq(squadId))).thenReturn(response);

            mockMvc.perform(get("/api/squads/{squadId}", squadId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("squad/success/find",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("squadId").description("스쿼드 식별자(ID)")),
                            responseBody()
                    ));
        }

        @Test
        @DisplayName("스쿼드에 참여하고 있지 않고, 크루만 참여하고 있는 경우(리더) 조회에 성공한다.")
        void success2() throws Exception {
            Long squadId = 1L;
            SquadWithStatesResponse response = new SquadWithStatesResponse(
                    SquadStates.of(
                            false,
                            false,
                            null,
                            true,
                            null,
                            true
                    ),
                    squadId,
                    "title",
                    "content",
                    8,
                    7,
                    "",
                    "",
                    "https://kakao-link",
                    "https://discord-link",
                    List.of(CategoryType.GAME.getText(), CategoryType.MOVIE.getText()),
                    new SimpleMemberResponse(
                            1L,
                            null,
                            "nickname",
                            "introduce",
                            Mbti.ENTJ.name()
                    )
            );
            when(squadQueryService.fetchSquad(anyLong(), eq(squadId))).thenReturn(response);

            mockMvc.perform(get("/api/squads/{squadId}", squadId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("squad/success/find2",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("squadId").description("스쿼드 식별자(ID)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("스쿼드 삭제를 문서화한다.")
    class deleteSquad {

        @Test
        @DisplayName("스쿼드 삭제에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            doNothing().when(squadCommandService).deleteSquad(anyLong(), eq(squadId));

            mockMvc.perform(delete("/api/squads/{squadId}", squadId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("squad/success/delete",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("squadId").description("스쿼드 식별자(ID)")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("크루 내 스쿼드 관리 목록 조회를 문서화한다.")
    class fetchManageList {

        @Test
        @DisplayName("크루 내 스쿼드 관리 목록 조회에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            PageRequest pageRequest = PageRequest.of(0, 10);
            List<SquadWithLeaderStateResponse> content = List.of(new SquadWithLeaderStateResponse(
                    SquadStates.of(true, true),
                    1L,
                    "title",
                    20,
                    19,
                    List.of(),
                    new SimpleMemberResponse(
                            1L,
                            null,
                            "nickname",
                            "introduce",
                            Mbti.ENTJ.name()
                    )
            ));
            PageResponse<SquadWithLeaderStateResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(squadQueryService.fetchManageList(anyLong(), eq(crewId), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/crews/{crewId}/squads/manage", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("squad/success/manage",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                            queryParameters(
                                    parameterWithName("page").description("페이지 번호").optional(),
                                    parameterWithName("size").description("한 페이지당 개수").optional()
                            ),
                            responseBody()
                    ));
        }
    }
}
