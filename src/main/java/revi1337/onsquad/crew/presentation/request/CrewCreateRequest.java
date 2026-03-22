package revi1337.onsquad.crew.presentation.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import revi1337.onsquad.crew.application.dto.CrewCreateDto;
import revi1337.onsquad.hashtag.domain.HashtagType;

public record CrewCreateRequest(
        @NotEmpty String name,
        @NotEmpty String introduce,
        @NotEmpty String detail,
        @NotNull List<HashtagType> hashtags,
        String kakaoLink
) {

    public CrewCreateDto toDto() {
        return new CrewCreateDto(name, introduce, detail, hashtags, kakaoLink);
    }
}
