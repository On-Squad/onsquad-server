package revi1337.onsquad.squad.domain.dto;

import com.querydsl.core.annotations.QueryProjection;
import java.util.ArrayList;
import java.util.List;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.member.domain.dto.SimpleMemberDomainDto;
import revi1337.onsquad.squad.domain.vo.Title;

public record SimpleSquadDomainDto(
        Long id,
        Title title,
        int capacity,
        int remain,
        List<CategoryType> categories,
        SimpleMemberDomainDto leader
) {
    @QueryProjection
    public SimpleSquadDomainDto(Long id, Title title, int capacity, int remain, SimpleMemberDomainDto owner) {
        this(id, title, capacity, remain, new ArrayList<>(), owner);
    }

    public SimpleSquadDomainDto(Long id, Title title, int capacity, int remain, List<CategoryType> categories,
                                SimpleMemberDomainDto leader) {
        this.id = id;
        this.title = title;
        this.capacity = capacity;
        this.remain = remain;
        this.categories = categories;
        this.leader = leader;
    }

    public void addCategories(List<CategoryType> categories) {
        this.categories.addAll(categories);
    }
}
