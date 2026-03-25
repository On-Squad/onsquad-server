package revi1337.onsquad.crew_member.application.scheduler;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.MemberFixture.createAndong;
import static revi1337.onsquad.common.fixture.MemberFixture.createKwangwon;
import static revi1337.onsquad.common.fixture.MemberFixture.createRevi;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.common.config.ApplicationLayerConfiguration;
import revi1337.onsquad.common.container.MySqlTestContainerInitializer;
import revi1337.onsquad.common.container.RedisTestContainerInitializer;
import revi1337.onsquad.common.fixture.CrewFixture;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.crew_member.application.leaderboard.CompositeScore;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardManager;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.entity.CrewMemberFactory;
import revi1337.onsquad.crew_member.domain.entity.CrewRanker;
import revi1337.onsquad.crew_member.domain.model.CrewActivity;
import revi1337.onsquad.crew_member.domain.model.CrewLeaderboards;
import revi1337.onsquad.crew_member.domain.model.CrewRankerCandidate;
import revi1337.onsquad.crew_member.domain.repository.rank.CrewRankerRepository;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;

@Sql("/mysql-truncate.sql")
@Import({ApplicationLayerConfiguration.class})
@ContextConfiguration(initializers = {MySqlTestContainerInitializer.class, RedisTestContainerInitializer.class})
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class CrewLeaderboardUpdateSchedulerTest {

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private CrewJpaRepository crewJpaRepository;

    @Autowired
    private CrewRankerRepository crewRankerRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CrewLeaderboardManager leaderboardManager;

    @Autowired
    private CrewLeaderboardUpdateScheduler leaderboardRefreshScheduler;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("스케줄러 실행 시 기존 랭킹은 사라지고, 최신 활동 기반의 새로운 랭킹이 DB에 반영된다")
    void refreshLeaderboard() {
        // given
        Member revi = createRevi();
        Member andong = createAndong();
        Member kwangwon = createKwangwon();
        memberRepository.saveAll(List.of(revi, andong, kwangwon));
        Crew crew = CrewFixture.createCrew(revi);
        crew.addCrewMember(createGeneralCrewMember(crew, andong), createGeneralCrewMember(crew, kwangwon));
        crewJpaRepository.save(crew);
        CrewRankerCandidate candidate1 = createCrewRankerCandidate(1L, 1, 1, revi);
        CrewRankerCandidate candidate2 = createCrewRankerCandidate(1L, 2, 1, andong);
        crewRankerRepository.insertBatch(List.of(candidate1, candidate2));

        Instant activityTime = CompositeScore.BASE_DATE.toInstant();
        leaderboardManager.applyActivity(1L, revi.getId(), activityTime.plusSeconds(600), CrewActivity.SQUAD_CREATE);
        leaderboardManager.applyActivity(1L, andong.getId(), activityTime.plusSeconds(660), CrewActivity.SQUAD_CREATE);
        leaderboardManager.applyActivity(1L, kwangwon.getId(), activityTime.plusSeconds(720), CrewActivity.SQUAD_COMMENT);

        // when
        leaderboardRefreshScheduler.updateLeaderboards();

        // then
        assertSoftly(softly -> {
            List<CrewRanker> currentRankedMembers = crewRankerRepository.findAllByCrewId(1L);
            softly.assertThat(currentRankedMembers).hasSize(3);
            softly.assertThat(currentRankedMembers.get(0).getMemberId()).isEqualTo(andong.getId());
            softly.assertThat(currentRankedMembers.get(1).getMemberId()).isEqualTo(revi.getId());
            softly.assertThat(currentRankedMembers.get(2).getMemberId()).isEqualTo(kwangwon.getId());

            CrewLeaderboards leaderboards = leaderboardManager.getAllLeaderboards(-1);
            softly.assertThat(leaderboards.isEmpty()).isTrue();
        });
    }

    private static CrewMember createGeneralCrewMember(Crew crew, Member member) {
        return CrewMemberFactory.general(crew, member, LocalDateTime.now());
    }

    private static CrewRankerCandidate createCrewRankerCandidate(Long crewId, int rank, long score, Member member) {
        return new CrewRankerCandidate(
                crewId,
                rank,
                score,
                member.getId(),
                member.getNickname().getValue(),
                member.getMbti().name(),
                LocalDateTime.now()
        );
    }
}
