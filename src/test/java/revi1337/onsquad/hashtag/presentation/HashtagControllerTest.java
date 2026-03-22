package revi1337.onsquad.hashtag.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import revi1337.onsquad.common.PresentationLayerTestSupport;
import revi1337.onsquad.hashtag.application.HashtagService;
import revi1337.onsquad.hashtag.domain.HashtagType;

@WebMvcTest(HashtagController.class)
class HashtagControllerTest extends PresentationLayerTestSupport {

    @MockBean(name = "cachedHashtagService")
    private HashtagService hashtagService;

    @Test
    @DisplayName("해시태그 목록을 문서화한다.")
    void getAllCategories() throws Exception {
        List<String> response = Arrays.stream(HashtagType.values())
                .map(HashtagType::getText)
                .toList();
        when(hashtagService.findHashtags()).thenReturn(response);

        mockMvc.perform(get("/api/hashtags")
                        .contentType(APPLICATION_JSON))
                .andDo(document("hashtags/success/fetch",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseBody()
                ));
    }
}
