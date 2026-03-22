package revi1337.onsquad.common.fixture;

import java.util.List;
import revi1337.onsquad.category.domain.entity.Category;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad_category.domain.entity.SquadCategory;

public class SquadCategoryFixture {

    public static List<SquadCategory> createSquadCategories(Squad squad, CategoryType... categoryTypes) {
        return Category.fromCategoryTypes(List.of(categoryTypes)).stream()
                .map(category -> new SquadCategory(squad, category))
                .toList();
    }

    public static List<SquadCategory> createSquadCategories(Squad squad, List<CategoryType> categoryTypes) {
        return Category.fromCategoryTypes(categoryTypes).stream()
                .map(category -> new SquadCategory(squad, category))
                .toList();
    }
}
