package revi1337.onsquad.category.util;

import static revi1337.onsquad.squad.error.SquadErrorCode.INVALID_CATEGORY;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.squad.error.exception.SquadDomainException;

public abstract class CategoryTypeUtil {

    public static List<CategoryType> extractPossible(List<CategoryType> categoryTypes) {
        if (categoryTypes.contains(CategoryType.ALL)) {
            return Collections.singletonList(CategoryType.ALL);
        }

        return categoryTypes.stream()
                .peek(CategoryTypeUtil::validateCategory)
                .collect(Collectors.toList());
    }

    public static void validateCategory(CategoryType categoryType) {
        if (categoryType == null) {
            throw new SquadDomainException.InvalidCategory(INVALID_CATEGORY);
        }
    }
}
