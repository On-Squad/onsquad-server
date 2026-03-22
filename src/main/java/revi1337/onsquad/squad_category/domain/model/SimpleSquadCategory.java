package revi1337.onsquad.squad_category.domain.model;

import revi1337.onsquad.category.domain.vo.CategoryType;

public record SimpleSquadCategory(
        Long squadId,
        CategoryType categoryType
) {

}
