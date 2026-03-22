package revi1337.onsquad.squad.domain;

import java.util.List;
import revi1337.onsquad.category.domain.vo.CategoryType;

public interface SquadLinkable {

    Long getSquadId();

    void addCategories(List<CategoryType> categories);

}
