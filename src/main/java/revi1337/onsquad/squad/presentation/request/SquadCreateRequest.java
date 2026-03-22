package revi1337.onsquad.squad.presentation.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.squad.domain.model.SquadCreateSpec;

public record SquadCreateRequest(
        @NotEmpty String title,
        @NotEmpty String content,
        @Positive int capacity,
        String address,
        String addressDetail,
        @NotNull List<CategoryType> categories,
        String kakaoLink,
        String discordLink
) {

    public SquadCreateSpec toDomainSpec() {
        return new SquadCreateSpec(
                title,
                content,
                capacity,
                address,
                addressDetail,
                categories,
                kakaoLink,
                discordLink
        );
    }
}
