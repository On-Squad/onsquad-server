package revi1337.onsquad.squad.application.response;

import java.util.List;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.member.application.dto.response.SimpleMemberResponse;
import revi1337.onsquad.squad.domain.model.SimpleSquad;

public record SquadWithLeaderStateResponse(
        SquadStates states,
        Long id,
        String title,
        int capacity,
        int remain,
        List<String> categories,
        SimpleMemberResponse leader
) {

    public static SquadWithLeaderStateResponse from(boolean isLeader, boolean canDestroy, SimpleSquad squad) {
        return new SquadWithLeaderStateResponse(
                SquadStates.of(isLeader, canDestroy),
                squad.id(),
                squad.title().getValue(),
                squad.capacity(),
                squad.capacity(),
                squad.categories().stream()
                        .map(CategoryType::getText)
                        .toList(),
                SimpleMemberResponse.from(squad.leader())
        );
    }
}
