package revi1337.onsquad.category.presentation.serialize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.common.config.web.ObjectMapperConfig;

@JsonTest
@Import(ObjectMapperConfig.class)
class CategoryTypeDeserializerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("JSON 문자열 '액티비티'를 CategoryType.ACTIVITY 역직렬화한다.")
    void deserialize() throws JsonProcessingException {
        String json = "\"액티비티\"";

        CategoryType result = objectMapper.readValue(json, CategoryType.class);

        assertThat(result).isEqualTo(CategoryType.ACTIVITY);
    }

    @Test
    @DisplayName("유효하지 않은 카테고리 텍스트가 들어오면 예외가 발생한다.")
    void deserialize2() {
        String json = "\"잘못된카테고리\"";

        assertThatThrownBy(() -> objectMapper.readValue(json, CategoryType.class))
                .isInstanceOf(Exception.class);
    }
}
