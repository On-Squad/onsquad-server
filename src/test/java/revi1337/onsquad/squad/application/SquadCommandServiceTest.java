package revi1337.onsquad.squad.application;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
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
import revi1337.onsquad.common.ApplicationLayerTestSupport;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad.domain.model.SquadCreateSpec;
import revi1337.onsquad.squad.domain.repository.SquadJpaRepository;
import revi1337.onsquad.squad_category.domain.repository.SquadCategoryJpaRepository;

@Sql({"/h2-truncate.sql", "/h2-category.sql"})
class SquadCommandServiceTest extends ApplicationLayerTestSupport {

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private CrewJpaRepository crewRepository;

    @Autowired
    private SquadJpaRepository squadRepository;

    @Autowired
    private SquadCategoryJpaRepository squadCategoryRepository;

    @Autowired
    private SquadCommandService squadCommandService;

    @Test
    @DisplayName("새로운 스쿼드를 생성하면 스쿼드 정보와 카테고리가 함께 저장된다.")
    void newSquad() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        SquadCreateSpec spec = new SquadCreateSpec(
                "title",
                "content",
                10,
                "add",
                "add-detail",
                List.of(CategoryType.GAME, CategoryType.MANGACAFE),
                "kakao",
                "discord"
        );

        Long squadId = squadCommandService.newSquad(member.getId(), crew.getId(), spec);

        assertSoftly(softly -> {
            clearPersistenceContext();
            softly.assertThat(squadRepository.findById(squadId)).isPresent();
            softly.assertThat(squadCategoryRepository.findAll()).hasSize(2);
        });
    }

    @Test
    @DisplayName("스쿼드를 삭제하면 해당 스쿼드와 관련된 카테고리 정보가 모두 제거된다.")
    void deleteSquad() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        Squad squad = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad, CategoryType.GAME, CategoryType.MANGACAFE));
        clearPersistenceContext();

        squadCommandService.deleteSquad(member.getId(), squad.getId());

        assertSoftly(softly -> {
            clearPersistenceContext();
            softly.assertThat(squadRepository.findById(squad.getId())).isEmpty();
            softly.assertThat(squadCategoryRepository.findAll().size()).isEqualTo(0);
        });
    }
}
