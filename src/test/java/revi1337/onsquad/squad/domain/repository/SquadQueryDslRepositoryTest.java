package revi1337.onsquad.squad.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;
import static revi1337.onsquad.common.fixture.SquadCategoryFixture.createSquadCategories;
import static revi1337.onsquad.common.fixture.SquadFixture.createSquad;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.common.PersistenceLayerTestSupport;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad.domain.model.SimpleSquad;
import revi1337.onsquad.squad.domain.model.SquadDetail;
import revi1337.onsquad.squad_category.domain.repository.SquadCategoryJpaRepository;

@Sql({"/h2-truncate.sql", "/h2-category.sql"})
@Import(SquadQueryDslRepository.class)
class SquadQueryDslRepositoryTest extends PersistenceLayerTestSupport {

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private CrewJpaRepository crewRepository;

    @Autowired
    private SquadJpaRepository squadRepository;

    @Autowired
    private SquadCategoryJpaRepository squadCategoryRepository;

    @Autowired
    private SquadQueryDslRepository squadQueryDslRepository;

    @Test
    @DisplayName("스쿼드 상세 조회 시 작성자 정보와 연관된 카테고리 목록을 페치 조인으로 함께 가져온다.")
    void fetchSquadDetailById1() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        Squad squad = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad, CategoryType.ESCAPEROOM, CategoryType.ACTIVITY));
        clearPersistenceContext();

        Optional<Squad> result = squadQueryDslRepository.fetchSquadDetailById(squad.getId());

        assertSoftly(softly -> {
            softly.assertThat(result).isPresent();
            softly.assertThat(result.get().getCategories()).hasSize(2);
        });
    }

    @Test
    @DisplayName("카테고리가 등록되지 않은 스쿼드도 상세 조회 시 작성자 정보와 함께 정상적으로 조회된다.")
    void fetchSquadDetailById2() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        Squad squad = squadRepository.save(createSquad(crew, member));
        clearPersistenceContext();

        Optional<Squad> result = squadQueryDslRepository.fetchSquadDetailById(squad.getId());

        assertSoftly(softly -> {
            softly.assertThat(result).isPresent();
            softly.assertThat(result.get().getCategories()).hasSize(0);
        });
    }

    @Test
    @DisplayName("카테고리 필터 없이 크루 ID로 스쿼드 목록을 조회하면 해당 크루의 모든 스쿼드가 페이징되어 반환된다.")
    void fetchSquadsWithDetailByCrewIdAndCategory() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        Squad squad1 = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad1, CategoryType.BADMINTON, CategoryType.ACTIVITY));
        Squad squad2 = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad2, CategoryType.BADMINTON, CategoryType.PERFORMANCE));
        Squad squad3 = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad3, CategoryType.BILLIARDS, CategoryType.FISHING));
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by("createdAt").descending());

        Page<SquadDetail> results = squadQueryDslRepository.fetchSquadsWithDetailByCrewIdAndCategory(crew.getId(), null, pageRequest);

        assertThat(results.getContent().size()).isEqualTo(3);
    }

    @Test
    @DisplayName("특정 카테고리 필터를 적용하여 크루 내 스쿼드 목록을 조회하면 해당 카테고리를 가진 스쿼드만 필터링되어 반환된다.")
    void fetchSquadsWithDetailByCrewIdAndCategory2() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        Squad squad1 = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad1, CategoryType.BADMINTON, CategoryType.ACTIVITY));
        Squad squad2 = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad2, CategoryType.BADMINTON, CategoryType.PERFORMANCE));
        Squad squad3 = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad3, CategoryType.BILLIARDS, CategoryType.FISHING));
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by("createdAt").descending());

        Page<SquadDetail> results = squadQueryDslRepository.fetchSquadsWithDetailByCrewIdAndCategory(crew.getId(), CategoryType.BADMINTON, pageRequest);

        assertSoftly(softly -> {
            softly.assertThat(results.getContent().size()).isEqualTo(2);
            softly.assertThat(results.getContent()).extracting(SquadDetail::getSquadId)
                    .containsExactlyInAnyOrder(squad1.getId(), squad2.getId());
        });
    }

    @Test
    @DisplayName("크루 ID를 기준으로 요약된 스쿼드 목록 정보를 페이징하여 조회한다.")
    void fetchSquadsByCrewId() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        Squad squad1 = squadRepository.save(createSquad(crew, member));
        Squad squad2 = squadRepository.save(createSquad(crew, member));
        Squad squad3 = squadRepository.save(createSquad(crew, member));
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by("createdAt").descending());

        Page<SimpleSquad> results = squadQueryDslRepository.fetchSquadsByCrewId(crew.getId(), pageRequest);

        assertSoftly(softly -> {
            softly.assertThat(results.getContent().size()).isEqualTo(3);
            softly.assertThat(results.getContent()).extracting(SimpleSquad::getSquadId)
                    .containsExactlyInAnyOrder(squad1.getId(), squad2.getId(), squad3.getId());
        });
    }
}
