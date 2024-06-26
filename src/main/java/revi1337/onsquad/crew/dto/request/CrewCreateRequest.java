package revi1337.onsquad.crew.dto.request;

import jakarta.validation.constraints.NotEmpty;
import revi1337.onsquad.crew.dto.CrewCreateDto;

import java.util.List;

public record CrewCreateRequest(
        @NotEmpty String name,
        @NotEmpty String introduce,
        @NotEmpty String detail,
        List<String> hashTags,
        String kakaoLink
) {
    public CrewCreateDto toDto() {
        return new CrewCreateDto(name, introduce, detail, hashTags, kakaoLink);
    }
}
