package revi1337.onsquad.squad.domain.model;

import java.util.ArrayList;
import java.util.List;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.member.domain.model.SimpleMember;
import revi1337.onsquad.squad.domain.SquadLinkable;
import revi1337.onsquad.squad.domain.vo.Title;

public record SimpleSquad(
        Long id,
        Title title,
        int capacity,
        int remain,
        List<CategoryType> categories,
        SimpleMember leader
) implements SquadLinkable {

    public SimpleSquad(Long id, Title title, int capacity, int remain, SimpleMember owner) {
        this(id, title, capacity, remain, new ArrayList<>(), owner);
    }

    @Override
    public Long getSquadId() {
        return id;
    }

    @Override
    public void addCategories(List<CategoryType> categories) {
        this.categories.addAll(categories);
    }
}
