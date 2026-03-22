package revi1337.onsquad.category.presentation.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.category.domain.vo.CategoryType;

class CategoryTypeConverterTest {

    private final CategoryTypeConverter converter = new CategoryTypeConverter();

    @Test
    @DisplayName("올바른 한글 카테고리 텍스트를 전달하면 해당 CategoryType 상수를 반환한다.")
    void convert1() {
        String input = "게임";

        CategoryType result = converter.convert(input);

        assertThat(result).isEqualTo(CategoryType.GAME);
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 텍스트를 전달하면 null을 반환한다.")
    void convert2() {
        String input = "존재하지않는카테고리";

        CategoryType result = converter.convert(input);

        assertThat(result).isNull();
    }
}
