package revi1337.onsquad.category.presentation;

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
import revi1337.onsquad.category.application.CategoryService;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.common.PresentationLayerTestSupport;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest extends PresentationLayerTestSupport {

    @MockBean(name = "cachedCategoryService")
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리 목록을 문서화한다.")
    void getAllCategories() throws Exception {
        List<String> response = Arrays.stream(CategoryType.values())
                .map(CategoryType::getText)
                .toList();
        when(categoryService.findCategories()).thenReturn(response);

        mockMvc.perform(get("/api/categories")
                        .contentType(APPLICATION_JSON))
                .andDo(document("categories/success/fetch",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseBody()
                ));
    }
}
