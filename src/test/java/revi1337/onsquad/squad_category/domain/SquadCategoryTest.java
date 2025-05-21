package revi1337.onsquad.squad_category.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static revi1337.onsquad.common.fixture.SquadFixture.SQUAD;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.category.domain.Category;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.squad.domain.Squad;

class SquadCategoryTest {

    @Test
    @DisplayName("SquadCategory 생성에 성공한다.")
    void success() {
        Squad squad = SQUAD();
        Category movie = new Category(CategoryType.MOVIE);

        SquadCategory squadCategory = new SquadCategory(squad, movie);

        assertThat(squadCategory).isNotNull();
    }
}
