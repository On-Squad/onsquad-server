package revi1337.onsquad.hashtag.presentation.serialize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import revi1337.onsquad.common.config.web.ObjectMapperConfig;
import revi1337.onsquad.hashtag.domain.HashtagType;

@JsonTest
@Import(ObjectMapperConfig.class)
class HashtagTypeDeserializerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("JSON 문자열 '모각코'를 HashtagType.CODING_TOGETHER로 역직렬화한다.")
    void deserialize() throws JsonProcessingException {
        String json = "\"모각코\"";

        HashtagType result = objectMapper.readValue(json, HashtagType.class);

        assertThat(result).isEqualTo(HashtagType.CODING_TOGETHER);
    }

    @Test
    @DisplayName("유효하지 않은 해시태그 텍스트가 들어오면 예외가 발생한다.")
    void deserialize2() {
        String json = "\"잘못된태그\"";

        assertThatThrownBy(() -> objectMapper.readValue(json, HashtagType.class))
                .isInstanceOf(Exception.class);
    }
}
