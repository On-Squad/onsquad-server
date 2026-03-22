package revi1337.onsquad.category.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import revi1337.onsquad.category.domain.error.CategoryDomainException;
import revi1337.onsquad.category.domain.vo.CategoryType;

class CategoryTypeTest {

    @Test
    @DisplayName("텍스트로부터 CategoryType을 정확하게 찾아야 한다")
    void fromText() {
        String text = "게임";

        CategoryType result = CategoryType.fromText(text);

        assertSoftly(softly -> {
            softly.assertThat(result).isEqualTo(CategoryType.GAME);
            softly.assertThat(result.getPk()).isEqualTo(CategoryType.GAME.getPk());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"배드민턴", "스키장", "눈꽃축제"})
    @DisplayName("다양한 유효 텍스트 입력 시 해당하는 Enum 상수를 반환한다")
    void fromText_Multiple(String text) {
        CategoryType result = CategoryType.fromText(text);
        assertThat(result).isNotNull();
        assertThat(result.getText()).isEqualTo(text);
    }

    @Test
    @DisplayName("텍스트 리스트를 변환할 때 중복된 텍스트는 하나로 취급한다.")
    void fromTextsDuplicate() {
        List<String> texts = List.of("액티비티", "액티비티", "영화");

        List<CategoryType> results = CategoryType.fromTexts(texts);

        assertThat(results).hasSize(2)
                .containsExactlyInAnyOrder(CategoryType.ACTIVITY, CategoryType.MOVIE);
    }

    @Test
    @DisplayName("존재하지 않는 텍스트 입력 시 예외를 반환해야 한다.")
    void fromText_NotFound() {
        String invalidText = "존재하지않는카테고리";

        assertThatThrownBy(() -> CategoryType.fromText(invalidText))
                .isExactlyInstanceOf(CategoryDomainException.InvalidCategory.class);
    }

    @Test
    @DisplayName("불변 리스트는 모든 Enum 상수를 포함해야 한다")
    void unmodifiableList() {
        List<CategoryType> result = CategoryType.unmodifiableList();

        assertThat(result).containsExactly(CategoryType.values());
    }
}
