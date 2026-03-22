package revi1337.onsquad.squad_category.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;
import static revi1337.onsquad.common.fixture.SquadCategoryFixture.createSquadCategories;
import static revi1337.onsquad.common.fixture.SquadFixture.createSquad;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.common.ApplicationLayerTestSupport;
import revi1337.onsquad.common.config.web.ObjectMapperConfig;
import revi1337.onsquad.common.container.RedisTestContainerInitializer;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad.domain.repository.SquadJpaRepository;
import revi1337.onsquad.squad_category.domain.model.SimpleSquadCategory;
import revi1337.onsquad.squad_category.domain.model.SquadCategories;
import revi1337.onsquad.squad_category.domain.repository.SquadCategoryJpaRepository;

@Sql({"/h2-truncate.sql", "/h2-category.sql"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(ObjectMapperConfig.class)
@ContextConfiguration(initializers = RedisTestContainerInitializer.class)
class SquadCategoryCacheServiceTest extends ApplicationLayerTestSupport {

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private CrewJpaRepository crewRepository;

    @Autowired
    private SquadJpaRepository squadRepository;

    @Autowired
    private SquadCategoryJpaRepository squadCategoryRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @SpyBean
    private SquadCategoryAccessor squadCategoryAccessor;

    @Autowired
    private SquadCategoryCacheService squadCategoryCacheService;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("[Full Cache Miss] 캐시가 전혀 없으면 DB에서 조회한 후 결과를 레디스에 저장하고 반환한다")
    void getCategoriesWhenFullCacheMiss() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        Squad squad1 = squadRepository.save(createSquad(crew, member));
        Squad squad2 = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad1, CategoryType.ACTIVITY, CategoryType.PERFORMANCE));
        squadCategoryRepository.saveAll(createSquadCategories(squad2, CategoryType.BADMINTON, CategoryType.ESCAPEROOM));

        SquadCategories result = squadCategoryCacheService.getCategoriesBySquadIdIn(List.of(squad1.getId(), squad2.getId()));

        assertSoftly(softly -> {
            softly.assertThat(result.values()).hasSize(4);
            softly.assertThat(result.values()).extracting(SimpleSquadCategory::categoryType)
                    .containsExactlyInAnyOrder(CategoryType.ACTIVITY, CategoryType.PERFORMANCE, CategoryType.BADMINTON, CategoryType.ESCAPEROOM);

            softly.assertThat(stringRedisTemplate.hasKey(String.format("onsquad:squad:%d:categories", squad1.getId()))).isTrue();
            softly.assertThat(stringRedisTemplate.hasKey(String.format("onsquad:squad:%d:categories", squad2.getId()))).isTrue();
        });
    }

    @Test
    @DisplayName("[Full Cache Hit] 모든 데이터가 캐시에 있으면 DB 조회 없이 캐시 데이터를 반환한다")
    void getCategoriesWhenFullCacheHit() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        Squad squad1 = squadRepository.save(createSquad(crew, member));
        Squad squad2 = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad1, CategoryType.ACTIVITY, CategoryType.PERFORMANCE));
        squadCategoryRepository.saveAll(createSquadCategories(squad2, CategoryType.BADMINTON, CategoryType.ESCAPEROOM));
        squadCategoryCacheService.getCategoriesBySquadIdIn(List.of(squad1.getId(), squad2.getId()));
        Mockito.clearInvocations(squadCategoryAccessor);

        SquadCategories result = squadCategoryCacheService.getCategoriesBySquadIdIn(List.of(squad1.getId(), squad2.getId()));

        assertSoftly(softly -> {
            softly.assertThat(result.values()).hasSize(4);
            softly.assertThat(result.values()).extracting(SimpleSquadCategory::categoryType)
                    .containsExactlyInAnyOrder(CategoryType.ACTIVITY, CategoryType.PERFORMANCE, CategoryType.BADMINTON, CategoryType.ESCAPEROOM);
            verify(squadCategoryAccessor, never()).fetchCategoriesBySquadIdIn(anyList());
        });
    }

    @Test
    @DisplayName("[Partial Cache Hit] 일부 캐시만 존재할 경우, 누락된 ID만 DB에서 조회하여 캐시와 합쳐서 반환한다")
    void getCategoriesWhenPartialCacheHit() {
        Member member = memberRepository.save(createRevi());
        Crew crew = crewRepository.save(createCrew(member));
        Squad squad1 = squadRepository.save(createSquad(crew, member));
        Squad squad2 = squadRepository.save(createSquad(crew, member));
        squadCategoryRepository.saveAll(createSquadCategories(squad1, CategoryType.ACTIVITY, CategoryType.PERFORMANCE));
        squadCategoryRepository.saveAll(createSquadCategories(squad2, CategoryType.BADMINTON, CategoryType.ESCAPEROOM));
        squadCategoryCacheService.getCategoriesBySquadIdIn(List.of(squad1.getId()));
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.clearInvocations(squadCategoryAccessor);

        SquadCategories result = squadCategoryCacheService.getCategoriesBySquadIdIn(List.of(squad1.getId(), squad2.getId()));

        assertSoftly(softly -> {
            softly.assertThat(result.values()).hasSize(4);
            softly.assertThat(result.values()).extracting(SimpleSquadCategory::categoryType)
                    .containsExactlyInAnyOrder(CategoryType.ACTIVITY, CategoryType.PERFORMANCE, CategoryType.BADMINTON, CategoryType.ESCAPEROOM);

            verify(squadCategoryAccessor).fetchCategoriesBySquadIdIn(captor.capture());
            List<Long> capturedIds = captor.getValue();
            softly.assertThat(capturedIds).hasSize(1);
            softly.assertThat(capturedIds).containsExactly(squad2.getId());
        });
    }

    @Test
    @DisplayName("[Edge Case] 조회하려는 ID 리스트가 비어있으면 빈 결과를 반환한다.")
    void getCategoriesWhenEmptyInput() {
        SquadCategories result = squadCategoryCacheService.getCategoriesBySquadIdIn(List.of());

        assertThat(result.values()).isEmpty();
    }
}
