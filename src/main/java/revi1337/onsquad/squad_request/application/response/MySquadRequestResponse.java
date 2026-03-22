package revi1337.onsquad.squad_request.application.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.crew.application.dto.response.SimpleCrewResponse;
import revi1337.onsquad.squad.application.response.SimpleSquadResponse;
import revi1337.onsquad.squad.domain.SquadLinkable;
import revi1337.onsquad.squad_request.domain.entity.SquadRequest;

public record MySquadRequestResponse(
        Long id,
        LocalDateTime requestAt,
        SimpleCrewResponse crew,
        SimpleSquadResponse squad
) implements SquadLinkable {

    public static MySquadRequestResponse from(SquadRequest request) {
        return new MySquadRequestResponse(
                request.getId(),
                request.getRequestAt(),
                SimpleCrewResponse.from(request.getSquad().getCrew()),
                SimpleSquadResponse.from(request.getSquad())

        );
    }

    public static MySquadRequestResponse from(SquadRequest request, List<CategoryType> categories) {
        return new MySquadRequestResponse(
                request.getId(),
                request.getRequestAt(),
                SimpleCrewResponse.from(request.getSquad().getCrew()),
                SimpleSquadResponse.from(request.getSquad(), categories)
        );
    }

    @JsonIgnore
    @Override
    public Long getSquadId() {
        return squad.id();
    }

    @Override
    public void addCategories(List<CategoryType> categories) {
        squad.categories().addAll(
                categories.stream()
                        .map(CategoryType::getText)
                        .toList()
        );
    }
}
