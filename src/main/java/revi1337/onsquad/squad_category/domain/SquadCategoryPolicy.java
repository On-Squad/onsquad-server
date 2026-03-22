package revi1337.onsquad.squad_category.domain;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import lombok.NoArgsConstructor;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.squad_category.domain.error.SquadCategoryDomainException;
import revi1337.onsquad.squad_category.domain.error.SquadCategoryErrorCode;

@NoArgsConstructor(access = PRIVATE)
public final class SquadCategoryPolicy {

    private static final int MAX_CATEGORY_SIZE = 5;

    public static void ensureNotExceedingCategoryLimit(List<CategoryType> categories) {
        if (categories.size() > MAX_CATEGORY_SIZE) {
            throw new SquadCategoryDomainException.InvalidCategorySize(SquadCategoryErrorCode.INVALID_CATEGORY_SIZE);
        }
    }
}
