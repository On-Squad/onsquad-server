package revi1337.onsquad.squad.application.response;

import java.util.ArrayList;
import java.util.List;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad.domain.model.SimpleSquad;

public record SimpleSquadResponse(
        Long id,
        String title,
        int capacity,
        int remain,
        List<String> categories,
        SimpleMemberResponse leader
) {

    public static SimpleSquadResponse from(SimpleSquad simpleSquad) {
        return new SimpleSquadResponse(
                simpleSquad.id(),
                simpleSquad.title().getValue(),
                simpleSquad.capacity(),
                simpleSquad.remain(),
                simpleSquad.categories().stream()
                        .map(CategoryType::getText)
                        .toList(),
                SimpleMemberResponse.from(simpleSquad.leader())
        );
    }

    public static SimpleSquadResponse from(Squad squad) {
        return new SimpleSquadResponse(
                squad.getId(),
                squad.getTitle().getValue(),
                squad.getCapacity(),
                squad.getRemain(),
                new ArrayList<>(),
                SimpleMemberResponse.from(squad.getMember())
        );
    }

    public static SimpleSquadResponse from(Squad squad, List<CategoryType> categories) {
        return new SimpleSquadResponse(
                squad.getId(),
                squad.getTitle().getValue(),
                squad.getCapacity(),
                squad.getRemain(),
                categories.stream()
                        .map(CategoryType::getText)
                        .toList(),
                SimpleMemberResponse.from(squad.getMember())
        );
    }
}
