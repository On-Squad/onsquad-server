package revi1337.onsquad.squad_comment.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.squad_comment.application.SquadCommentCommandService;
import revi1337.onsquad.squad_comment.application.SquadCommentQueryService;
import revi1337.onsquad.squad_comment.application.response.SquadCommentResponse;
import revi1337.onsquad.squad_comment.application.response.SquadCommentStates;
import revi1337.onsquad.squad_comment.domain.SquadCommentPolicy;
import revi1337.onsquad.squad_comment.presentation.request.CommentCreateRequest;

@WebMvcTest(SquadCommentController.class)
class SquadCommentControllerTest extends PresentationLayerTestSupport {

    @MockBean
    private SquadCommentCommandService squadCommentCommandService;

    @MockBean
    private SquadCommentQueryService squadCommentQueryService;

    @Nested
    @DisplayName("스쿼드 댓글 작성을 문서화한다.")
    class add {

        @Test
        @DisplayName("스쿼드 댓글 작성에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            CommentCreateRequest request = new CommentCreateRequest("댓글 내용입니다.");
            doNothing().when(squadCommentCommandService).add(anyLong(), eq(squadId), anyString());

            mockMvc.perform(post("/api/squads/{squadId}/comments", squadId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(201))
                    .andDo(document("squad-comment/success/new",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(parameterWithName("squadId").description("스쿼드 식별자(ID)")),
                            requestFields(fieldWithPath("content").description("댓글 내용")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("스쿼드 답글 작성을 문서화한다.")
    class addReply {

        @Test
        @DisplayName("스쿼드 답글 작성에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            Long parentId = 10L;
            CommentCreateRequest request = new CommentCreateRequest("답글 내용입니다.");
            doNothing().when(squadCommentCommandService).addReply(anyLong(), eq(squadId), eq(parentId), anyString());

            mockMvc.perform(post("/api/squads/{squadId}/replies/{parentId}", squadId, parentId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(201))
                    .andDo(document("squad-comment/success/reply-new",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("squadId").description("스쿼드 식별자(ID)"),
                                    parameterWithName("parentId").description("부모 댓글 식별자(ID)")
                            ),
                            requestFields(fieldWithPath("content").description("답글 내용")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("스쿼드 댓글 목록 조회를 문서화한다.")
    class fetchInitialComments {

        @Test
        @DisplayName("부모 댓글 목록 조회에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            PageRequest pageRequest = PageRequest.of(0, 10);
            List<SquadCommentResponse> content = List.of(
                    new SquadCommentResponse(
                            new SquadCommentStates(true),
                            null,
                            2L,
                            false,
                            "부모 댓글입니다.",
                            null,
                            LocalDate.of(2026, 1, 4).atStartOfDay(),
                            LocalDate.of(2026, 1, 4).atStartOfDay(),
                            new SimpleMemberResponse(
                                    1L,
                                    null,
                                    "nickname-1",
                                    "introduce-1",
                                    Mbti.ENTP.name()
                            ),
                            new ArrayList<>()
                    ),
                    new SquadCommentResponse(
                            new SquadCommentStates(false),
                            null,
                            1L,
                            true,
                            SquadCommentPolicy.DELETED_CONTENT,
                            LocalDate.of(2026, 1, 4).atStartOfDay(),
                            null,
                            null,
                            null,
                            new ArrayList<>()
                    )
            );
            PageResponse<SquadCommentResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(squadCommentQueryService.fetchInitialComments(anyLong(), eq(squadId), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/squads/{squadId}/comments", squadId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("squad-comment/success/finds",
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
    @DisplayName("스쿼드 답글 목록 조회를 문서화한다.")
    class fetchMoreChildren {

        @Test
        @DisplayName("특정 댓글의 답글 목록 조회에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            Long parentId = 1L;
            PageRequest pageRequest = PageRequest.of(0, 10);
            List<SquadCommentResponse> content = List.of(
                    new SquadCommentResponse(
                            new SquadCommentStates(true),
                            parentId,
                            3L,
                            false,
                            "답글 내용입니다.",
                            null,
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            new SimpleMemberResponse(
                                    3L,
                                    null,
                                    "nickname-3",
                                    "introduce-3",
                                    Mbti.ENTP.name()
                            ),
                            new ArrayList<>()
                    ),
                    new SquadCommentResponse(
                            new SquadCommentStates(false),
                            parentId,
                            2L,
                            true,
                            SquadCommentPolicy.DELETED_CONTENT,
                            LocalDate.of(2026, 1, 4).atStartOfDay(),
                            null,
                            null,
                            null,
                            new ArrayList<>()
                    )
            );
            PageResponse<SquadCommentResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageRequest, content.size()));
            when(squadCommentQueryService.fetchMoreChildren(anyLong(), eq(squadId), eq(parentId), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/squads/{squadId}/comments/{parentId}/replies", squadId, parentId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andDo(document("squad-comment/success/reply-finds",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("squadId").description("스쿼드 식별자(ID)"),
                                    parameterWithName("parentId").description("부모 댓글 식별자(ID)")
                            ),
                            queryParameters(
                                    parameterWithName("page").description("페이지 번호").optional(),
                                    parameterWithName("size").description("한 페이지당 개수").optional()
                            ),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("스쿼드 댓글 수정을 문서화한다.")
    class updateComment {

        @Test
        @DisplayName("댓글 수정에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            Long commentId = 1L;
            CommentCreateRequest request = new CommentCreateRequest("수정된 댓글 내용입니다.");
            doNothing().when(squadCommentCommandService).update(anyLong(), eq(squadId), eq(commentId), anyString());

            mockMvc.perform(patch("/api/squads/{squadId}/comments/{commentId}", squadId, commentId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("squad-comment/success/update",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("squadId").description("스쿼드 식별자(ID)"),
                                    parameterWithName("commentId").description("댓글 식별자(ID)")
                            ),
                            requestFields(fieldWithPath("content").description("수정할 댓글 내용")),
                            responseBody()
                    ));
        }
    }

    @Nested
    @DisplayName("스쿼드 댓글 삭제를 문서화한다.")
    class deleteComment {

        @Test
        @DisplayName("댓글 삭제에 성공한다.")
        void success() throws Exception {
            Long squadId = 1L;
            Long commentId = 1L;
            doNothing().when(squadCommentCommandService).delete(anyLong(), eq(squadId), eq(commentId));

            mockMvc.perform(delete("/api/squads/{squadId}/comments/{commentId}", squadId, commentId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(204))
                    .andDo(document("squad-comment/success/delete",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                            pathParameters(
                                    parameterWithName("squadId").description("스쿼드 식별자(ID)"),
                                    parameterWithName("commentId").description("댓글 식별자(ID)")
                            ),
                            responseBody()
                    ));
        }
    }
}
