package revi1337.onsquad.squad.domain.model;

import java.util.List;
import lombok.Getter;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.squad.domain.SquadPolicy;
import revi1337.onsquad.squad.domain.vo.Address;
import revi1337.onsquad.squad.domain.vo.Content;
import revi1337.onsquad.squad.domain.vo.Title;
import revi1337.onsquad.squad_category.domain.SquadCategoryPolicy;

@Getter
public class SquadCreateSpec {

    private final Title title;
    private final Content content;
    private final int capacity;
    private final Address address;
    private final List<CategoryType> categories;
    private final String kakaoLink;
    private final String discordLink;

    public SquadCreateSpec(
            String title,
            String content,
            int capacity,
            String address,
            String addressDetail,
            List<CategoryType> categories,
            String kakaoLink,
            String discordLink
    ) {
        SquadPolicy.validateCapacity(capacity);
        SquadCategoryPolicy.ensureNotExceedingCategoryLimit(categories);
        this.title = new Title(title);
        this.content = new Content(content);
        this.capacity = capacity;
        this.address = new Address(address, addressDetail);
        this.categories = categories;
        this.kakaoLink = kakaoLink;
        this.discordLink = discordLink;
    }
}
