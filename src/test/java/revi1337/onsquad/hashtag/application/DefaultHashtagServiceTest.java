package revi1337.onsquad.hashtag.application;

import static org.assertj.core.api.Assertions.assertThat;
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
import revi1337.onsquad.hashtag.domain.entity.Hashtag;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.hashtag.domain.repository.HashtagJpaRepository;

@ExtendWith(MockitoExtension.class)
class DefaultHashtagServiceTest {

    @Mock
    private HashtagJpaRepository hashtagJpaRepository;

    @InjectMocks
    private DefaultHashtagService hashtagService;

    @Test
    @DisplayName("DB에 저장된 모든 해시태그의 한글 텍스트 리스트를 반환한다.")
    void findHashtags_Success() {
        List<Hashtag> hashtags = List.of(
                Hashtag.fromHashtagType(HashtagType.ACTIVE),
                Hashtag.fromHashtagType(HashtagType.READING)
        );
        given(hashtagJpaRepository.findAll()).willReturn(hashtags);

        List<String> result = hashtagService.findHashtags();

        assertThat(result).hasSize(2).containsExactly("활발한", "독서");
        verify(hashtagJpaRepository, times(1)).findAll();
    }
}
