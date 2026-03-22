package revi1337.onsquad.squad.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.squad.domain.SquadLinkable;
import revi1337.onsquad.squad_category.domain.model.SquadCategories;

public record SquadLinkableGroup<T extends SquadLinkable>(List<T> results) {

    public SquadLinkableGroup(List<T> results) {
        this.results = Collections.unmodifiableList(results);
    }

    public List<Long> getSquadIds() {
        return results.stream()
                .map(SquadLinkable::getSquadId)
                .toList();
    }

    public List<T> values() {
        return results;
    }

    public Stream<T> stream() {
        return results.stream();
    }

    public boolean isNotEmpty() {
        return !results.isEmpty();
    }

    public void linkCategories(SquadCategories categories) {
        Map<Long, List<CategoryType>> categoryMap = categories.groupBySquadId();
        this.results.forEach(squadLink -> {
            List<CategoryType> categoryTypes = categoryMap.get(squadLink.getSquadId());
            if (categoryTypes != null) {
                squadLink.addCategories(categoryTypes);
            }
        });
    }
}
