package revi1337.onsquad.crew.presentation.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import revi1337.onsquad.crew.application.dto.CrewUpdateDto;
import revi1337.onsquad.hashtag.domain.HashtagType;

public record CrewUpdateRequest(
        @NotEmpty String name,
        @NotEmpty String introduce,
        @NotEmpty String detail,
        @NotNull List<HashtagType> hashtags,
        String kakaoLink
) {

    public CrewUpdateDto toDto() {
        return new CrewUpdateDto(name, introduce, detail, hashtags, kakaoLink);
    }
}
