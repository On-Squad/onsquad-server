package revi1337.onsquad.category.application;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import revi1337.onsquad.category.domain.entity.Category;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.category.domain.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class DefaultCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private DefaultCategoryService categoryService;

    @Test
    @DisplayName("DB에 저장된 모든 카테고리의 한글 텍스트 리스트를 반환한다.")
    void findCategories_Success() {
        List<Category> categories = List.of(
                new Category(CategoryType.GAME),
                new Category(CategoryType.BADMINTON),
                new Category(CategoryType.TENNIS)
        );
        given(categoryRepository.findAll()).willReturn(categories);

        List<String> result = categoryService.findCategories();

        assertThat(result).hasSize(3).containsExactly("게임", "배드민턴", "테니스");
        verify(categoryRepository, times(1)).findAll();
    }
}
