package revi1337.onsquad.crew.presentation;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import revi1337.onsquad.announce.application.dto.response.AnnounceResponse;
import revi1337.onsquad.announce.application.dto.response.AnnounceStates;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.common.PresentationLayerTestSupport;
import revi1337.onsquad.crew.application.CrewMainService;
import revi1337.onsquad.crew.application.dto.response.CrewMainResponse;
import revi1337.onsquad.crew.application.dto.response.CrewManageResponse;
import revi1337.onsquad.crew.application.dto.response.CrewResponse;
import revi1337.onsquad.crew.application.dto.response.CrewStates;
import revi1337.onsquad.crew_member.application.response.CrewRankerResponse;
import revi1337.onsquad.crew_member.domain.CrewRole;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.squad.application.response.SquadResponse;

@WebMvcTest(CrewMainController.class)
class CrewMainControllerTest extends PresentationLayerTestSupport {

    @MockBean
    private CrewMainService crewMainService;

    @Test
    @DisplayName("크루 메인을 문서화한다.")
    void fetchMain() throws Exception {
        Long crewId = 1L;
        PageRequest pageRequest = PageRequest.of(0, 2);
        LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
        CrewMainResponse response = getMainResponse(crewId, baseTime);
        given(crewMainService.fetchMain(anyLong(), eq(crewId), eq(pageRequest))).willReturn(response);

        mockMvc.perform(get("/api/crews/{crewId}/main", crewId)
                        .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                        .param("page", String.valueOf(pageRequest.getPageNumber()))
                        .param("size", String.valueOf(pageRequest.getPageSize()))
                        .contentType(APPLICATION_JSON))
                .andDo(document("crew/success/main",
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

    @Test
    @DisplayName("크루 관리를 문서화한다.")
    void fetchManageInfo() throws Exception {
        Long crewId = 1L;
        CrewManageResponse response = getManageResponse();
        given(crewMainService.fetchManageInfo(anyLong(), eq(crewId))).willReturn(response);

        mockMvc.perform(get("/api/crews/{crewId}/manage", crewId)
                        .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                        .contentType(APPLICATION_JSON))
                .andDo(document("crew/success/manage",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")),
                        pathParameters(parameterWithName("crewId").description("크루 식별자(ID)")),
                        responseBody()
                ));
    }

    private static CrewManageResponse getManageResponse() {
        return new CrewManageResponse(
                new CrewStates(true, true),
                0,
                1,
                1
        );
    }

    private CrewMainResponse getMainResponse(Long crewId, LocalDateTime baseTime) {
        return new CrewMainResponse(
                new CrewStates(true),
                new CrewResponse(
                        crewId,
                        "crew-name",
                        "crew-introduce",
                        "crew-detail",
                        "",
                        "kakao-link",
                        List.of(HashtagType.ACTIVE.getText(), HashtagType.POSITIVE.getText()),
                        1L,
                        new SimpleMemberResponse(1L, null, "nickname", "introduce", Mbti.ENTJ.name())
                ),
                List.of(new AnnounceResponse(
                        new AnnounceStates(CrewRole.OWNER),
                        1L,
                        "title",
                        "content",
                        baseTime,
                        false,
                        null,
                        new SimpleMemberResponse(1L, null, "nickname", "introduce", Mbti.ENTJ.name())
                )),
                List.of(new CrewRankerResponse(
                        crewId,
                        1L,
                        "nickname",
                        Mbti.ENTJ.name(),
                        1,
                        10,
                        baseTime
                )),
                List.of(new SquadResponse(
                        1L,
                        "title",
                        "content",
                        10,
                        9,
                        "address",
                        "address-detail",
                        "kakao-link",
                        "discord-link",
                        List.of(CategoryType.GAME.getText()),
                        new SimpleMemberResponse(1L, null, "nickname", "introduce", Mbti.ENTJ.name())
                ))
        );
    }
}
