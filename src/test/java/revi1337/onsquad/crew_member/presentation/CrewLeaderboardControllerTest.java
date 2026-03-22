package revi1337.onsquad.crew_member.presentation;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import revi1337.onsquad.common.PresentationLayerTestSupport;
import revi1337.onsquad.crew_member.application.leaderboard.CrewRankerQueryService;
import revi1337.onsquad.crew_member.application.response.CrewRankerResponse;
import revi1337.onsquad.member.domain.vo.Mbti;

@WebMvcTest(CrewLeaderboardController.class)
class CrewLeaderboardControllerTest extends PresentationLayerTestSupport {

    @MockBean
    private CrewRankerQueryService crewRankerQueryService;

    @Nested
    @DisplayName("크루 랭커 조회를 문서화한다.")
    class findCrewRanker {

        @Test
        @DisplayName("특정 크루의 상위 랭커 목록 조회에 성공한다.")
        void success() throws Exception {
            Long crewId = 1L;
            LocalDateTime baseTime = LocalDate.of(2026, 1, 5).atStartOfDay();
            List<CrewRankerResponse> response = getCrewRankerResponses(crewId, baseTime);
            when(crewRankerQueryService.findCrewRankers(anyLong(), anyLong()))
                    .thenReturn(response);

            mockMvc.perform(get("/api/crews/{crewId}/leaderboard", crewId)
                            .header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE)
                            .contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andDo(document("crew-member/success/leaderboard",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(
                                    headerWithName(AUTHORIZATION_HEADER_KEY).description("사용자 JWT 인증 정보")
                            ),
                            pathParameters(
                                    parameterWithName("crewId").description("크루 식별자(ID)")
                            ),
                            responseBody()
                    ));
        }
    }

    private List<CrewRankerResponse> getCrewRankerResponses(Long crewId, LocalDateTime baseTime) {
        return List.of(
                new CrewRankerResponse(
                        crewId,
                        100L,
                        "name-100",
                        Mbti.INFJ.name(),
                        1,
                        1200,
                        baseTime.plusDays(2)
                ),
                new CrewRankerResponse(
                        crewId,
                        50L,
                        "name-50",
                        Mbti.ISTJ.name(),
                        2,
                        622,
                        baseTime.plusDays(1)
                ),
                new CrewRankerResponse(
                        crewId,
                        70L,
                        "nick-name-70",
                        Mbti.ISTP.name(),
                        3,
                        622,
                        baseTime.plusDays(1)
                )
        );
    }
}
