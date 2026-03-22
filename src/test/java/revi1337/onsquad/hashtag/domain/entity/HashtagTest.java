package revi1337.onsquad.hashtag.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.hashtag.domain.HashtagType;

class HashtagTest {

    @Test
    @DisplayName("HashtagType을 통해 Hashtag 엔티티를 생성하면 ID와 타입이 일치해야 한다.")
    void fromHashtagType() {
        HashtagType type = HashtagType.ACTIVE;

        Hashtag hashtag = Hashtag.fromHashtagType(type);

        assertSoftly(softly -> {
            softly.assertThat(hashtag.getId()).isEqualTo(type.getPk());
            softly.assertThat(hashtag.getHashtagType()).isEqualTo(type);
        });
    }

    @Test
    @DisplayName("ID가 동일한 Hashtag 객체는 서로 같은 것으로 간주한다.")
    void fromHashtagType2() {
        Hashtag hashtag1 = Hashtag.fromHashtagType(HashtagType.ACTIVE);
        Hashtag hashtag2 = Hashtag.fromHashtagType(HashtagType.ACTIVE);

        assertThat(hashtag1).isEqualTo(hashtag2);
        assertThat(hashtag1.hashCode()).isEqualTo(hashtag2.hashCode());

        Set<Hashtag> hashtagSet = new HashSet<>();
        hashtagSet.add(hashtag1);
        hashtagSet.add(hashtag2);

        assertThat(hashtagSet).hasSize(1);
    }
}
