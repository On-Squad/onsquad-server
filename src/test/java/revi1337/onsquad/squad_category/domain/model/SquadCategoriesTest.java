package revi1337.onsquad.squad_category.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.category.domain.vo.CategoryType;

class SquadCategoriesTest {

    @Test
    @DisplayName("squadId를 기준으로 CategoryType들을 그룹화한다.")
    void groupBySquadId() {
        SimpleSquadCategory item1 = new SimpleSquadCategory(1L, CategoryType.ACTIVITY);
        SimpleSquadCategory item2 = new SimpleSquadCategory(1L, CategoryType.PERFORMANCE);
        SimpleSquadCategory item3 = new SimpleSquadCategory(2L, CategoryType.GAME);
        SquadCategories squadCategories = new SquadCategories(List.of(item1, item2, item3));

        Map<Long, List<CategoryType>> result = squadCategories.groupBySquadId();

        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(2);
            softly.assertThat(result.get(1L)).containsExactlyInAnyOrder(CategoryType.ACTIVITY, CategoryType.PERFORMANCE);
            softly.assertThat(result.get(2L)).containsExactly(CategoryType.GAME);
        });
    }

    @Test
    @DisplayName("squadId를 기준으로 각각의 SquadCategories 객체로 분리한다.")
    void splitBySquad() {
        SimpleSquadCategory item1 = new SimpleSquadCategory(1L, CategoryType.ACTIVITY);
        SimpleSquadCategory item2 = new SimpleSquadCategory(2L, CategoryType.GAME);
        SquadCategories squadCategories = new SquadCategories(List.of(item1, item2));

        Map<Long, SquadCategories> result = squadCategories.splitBySquad();

        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(2);
            softly.assertThat(result.get(1L)).isInstanceOf(SquadCategories.class);
            softly.assertThat(result.get(1L).values()).extracting(SimpleSquadCategory::categoryType)
                    .containsExactly(CategoryType.ACTIVITY);
        });
    }

    @Test
    @DisplayName("[Edge Case] 데이터가 없는 상태에서 그룹화 호출 시 빈 맵을 반환한다.")
    void groupBySquadId_Empty() {
        SquadCategories emptyCategories = new SquadCategories();

        Map<Long, List<CategoryType>> groupResult = emptyCategories.groupBySquadId();
        Map<Long, SquadCategories> splitResult = emptyCategories.splitBySquad();

        assertSoftly(softly -> {
            softly.assertThat(groupResult).isEmpty();
            softly.assertThat(groupResult).isNotNull();
            softly.assertThat(splitResult).isEmpty();
            softly.assertThat(splitResult).isNotNull();
        });
    }

    @Test
    @DisplayName("불변 리스트를 보장하여 외부에서 수정을 시도하면 예외가 발생한다.")
    void unmodifiableList() {
        SimpleSquadCategory item = new SimpleSquadCategory(1L, CategoryType.ACTIVITY);
        SquadCategories squadCategories = new SquadCategories(new ArrayList<>(List.of(item)));

        assertThatThrownBy(() -> squadCategories.values().add(new SimpleSquadCategory(2L, CategoryType.GAME)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
