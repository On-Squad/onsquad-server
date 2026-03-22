package revi1337.onsquad.squad_category.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;
import static revi1337.onsquad.common.fixture.SquadCategoryFixture.createSquadCategories;
import static revi1337.onsquad.common.fixture.SquadFixture.createSquad;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.common.PersistenceLayerTestSupport;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad.domain.repository.SquadJpaRepository;
import revi1337.onsquad.squad_category.domain.model.SimpleSquadCategory;

@Sql({"/h2-category.sql"})
class SquadCategoryJpaRepositoryTest extends PersistenceLayerTestSupport {

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private CrewJpaRepository crewRepository;

    @Autowired
    private SquadJpaRepository squadRepository;

    @Autowired
    private SquadCategoryJpaRepository squadCategoryRepository;

    @Test
    @DisplayName("조회된 DTO 리스트의 크기와 포함된 CategoryType 항목들이 저장된 데이터와 일치해야 한다")
    void fetchCategoriesBySquadIdIn() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        Squad squad1 = squadRepository.save(createSquad(crew, member));
        Squad squad2 = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad1, CategoryType.ACTIVITY, CategoryType.PERFORMANCE));
        squadCategoryRepository.saveAll(createSquadCategories(squad2, CategoryType.GAME, CategoryType.MOVIE));

        List<SimpleSquadCategory> squadCategories = squadCategoryRepository.fetchCategoriesBySquadIdIn(List.of(squad1.getId(), squad2.getId()));

        assertThat(squadCategories)
                .hasSize(4)
                .extracting(SimpleSquadCategory::categoryType)
                .containsExactlyInAnyOrder(CategoryType.ACTIVITY, CategoryType.PERFORMANCE, CategoryType.GAME, CategoryType.MOVIE);
    }

    @Test
    @DisplayName("삭제 요청 후 영향받은 행의 수가 정확해야 하며, 나머지 스쿼드의 데이터는 보존되어야 한다")
    void deleteBySquadIdIn() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        Squad squad1 = squadRepository.save(createSquad(crew, member));
        Squad squad2 = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad1, CategoryType.ACTIVITY, CategoryType.PERFORMANCE));
        squadCategoryRepository.saveAll(createSquadCategories(squad2, CategoryType.GAME, CategoryType.MOVIE));

        int deleted = squadCategoryRepository.deleteBySquadIdIn(List.of(squad1.getId()));

        assertThat(deleted).isEqualTo(2);
        assertThat(squadCategoryRepository.findAll()).hasSize(2);
    }
}
