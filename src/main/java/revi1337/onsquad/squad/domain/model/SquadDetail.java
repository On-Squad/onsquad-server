package revi1337.onsquad.squad.domain.model;

import java.util.ArrayList;
import java.util.List;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.member.domain.model.SimpleMember;
import revi1337.onsquad.squad.domain.SquadLinkable;
import revi1337.onsquad.squad.domain.vo.Address;
import revi1337.onsquad.squad.domain.vo.Content;
import revi1337.onsquad.squad.domain.vo.Title;

public record SquadDetail(
        Long id,
        Title title,
        Content content,
        int capacity,
        int remain,
        Address address,
        String kakaoLink,
        String discordLink,
        List<CategoryType> categories,
        SimpleMember leader
) implements SquadLinkable {

    public SquadDetail(
            Long id,
            Title title,
            Content content,
            int capacity,
            int remain,
            Address address,
            String kakaoLink,
            String discordLink,
            SimpleMember owner
    ) {
        this(id, title, content, capacity, remain, address, kakaoLink, discordLink, new ArrayList<>(), owner);
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
