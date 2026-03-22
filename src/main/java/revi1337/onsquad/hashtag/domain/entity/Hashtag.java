package revi1337.onsquad.hashtag.domain.entity;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.hashtag.domain.HashtagType;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
public class Hashtag {

    @Id
    private Long id;

    @Enumerated(STRING)
    @Column(nullable = false)
    private HashtagType hashtagType;

    private Hashtag(HashtagType hashtagType) {
        this.id = hashtagType.getPk();
        this.hashtagType = hashtagType;
    }

    public static List<Hashtag> fromHashtagTypes(List<HashtagType> hashtagTypes) {
        return hashtagTypes.stream()
                .map(Hashtag::fromHashtagType)
                .collect(Collectors.toList());
    }

    public static Hashtag fromHashtagType(HashtagType hashtagType) {
        return new Hashtag(hashtagType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Hashtag hashtag)) {
            return false;
        }
        return id != null && Objects.equals(getId(), hashtag.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
