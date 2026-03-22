package revi1337.onsquad.hashtag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.hashtag.domain.error.HashtagDomainException;

class HashtagTypeTest {

    @Test
    @DisplayName("텍스트를 통해 올바른 HashtagType을 조회한다.")
    void fromTextSuccess() {
        String text = "모각코";

        HashtagType result = HashtagType.fromText(text);

        assertThat(result).isEqualTo(HashtagType.CODING_TOGETHER);
        assertThat(result.getPk()).isEqualTo(37L);
    }

    @Test
    @DisplayName("존재하지 않는 텍스트 조회 시 예외가 발생한다.")
    void fromTextFail() {
        String invalidText = "유령태그";

        assertThatThrownBy(() -> HashtagType.fromText(invalidText))
                .isExactlyInstanceOf(HashtagDomainException.InvalidHashtag.class);
    }

    @Test
    @DisplayName("텍스트 리스트를 변환할 때 중복된 텍스트는 하나로 취급한다.")
    void fromTextsDuplicate() {
        List<String> texts = List.of("여행", "여행", "독서");

        List<HashtagType> results = HashtagType.fromTexts(texts);

        assertThat(results).hasSize(2)
                .containsExactlyInAnyOrder(HashtagType.TRAVEL, HashtagType.READING);
    }
}
