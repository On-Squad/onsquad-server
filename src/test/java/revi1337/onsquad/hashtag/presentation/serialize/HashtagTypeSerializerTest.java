package revi1337.onsquad.hashtag.presentation.serialize;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import revi1337.onsquad.common.config.web.ObjectMapperConfig;
import revi1337.onsquad.hashtag.domain.HashtagType;

@JsonTest
@Import(ObjectMapperConfig.class)
class HashtagTypeSerializerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("HashtagType Enum을 직렬화하면 한글 텍스트(text 필드값)가 반환되어야 한다.")
    void serialize1() throws JsonProcessingException {
        HashtagType type = HashtagType.ACTIVE;

        String json = objectMapper.writeValueAsString(type);

        assertThat(json).isEqualTo("\"활발한\"");
    }

    @Test
    @DisplayName("HashtagType 리스트를 직렬화하면 각 요소가 한글 텍스트로 변환된 JSON 배열이 반환되어야 한다.")
    void serialize2() throws JsonProcessingException {
        List<HashtagType> types = List.of(HashtagType.ACTIVE, HashtagType.TRENDY);

        String json = objectMapper.writeValueAsString(types);

        assertThat(json).isEqualTo("[\"활발한\",\"트랜디한\"]");
    }
}
