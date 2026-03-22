package revi1337.onsquad.squad_category.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import revi1337.onsquad.category.domain.vo.CategoryType;

@Getter
public class SquadCategories {

    private final List<SimpleSquadCategory> categories;

    public SquadCategories() {
        this.categories = List.of();
    }

    public SquadCategories(List<SimpleSquadCategory> categories) {
        this.categories = Collections.unmodifiableList(categories);
    }

    public Map<Long, List<CategoryType>> groupBySquadId() {
        return categories.stream()
                .collect(Collectors.groupingBy(
                        SimpleSquadCategory::squadId,
                        Collectors.mapping(SimpleSquadCategory::categoryType, Collectors.toList())
                ));
    }

    public Map<Long, SquadCategories> splitBySquad() {
        return categories.stream()
                .collect(Collectors.groupingBy(
                        SimpleSquadCategory::squadId,
                        Collectors.collectingAndThen(Collectors.toList(), SquadCategories::new)
                ));
    }

    public List<SimpleSquadCategory> values() {
        return categories;
    }
}
