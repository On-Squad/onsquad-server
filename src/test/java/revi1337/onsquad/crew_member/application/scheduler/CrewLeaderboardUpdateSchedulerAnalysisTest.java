package revi1337.onsquad.crew_member.application.scheduler;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.StopWatch;
import revi1337.onsquad.common.config.ApplicationLayerConfiguration;
import revi1337.onsquad.common.container.MySqlTestContainerInitializer;
import revi1337.onsquad.common.container.RedisTestContainerInitializer;
import revi1337.onsquad.common.fixture.MemberFixture;
import revi1337.onsquad.crew_member.application.leaderboard.CompositeScore;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardKeyMapper;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardManager;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardUpdateService;
import revi1337.onsquad.crew_member.domain.model.CrewActivity;
import revi1337.onsquad.crew_member.domain.model.CrewRankerCandidate;
import revi1337.onsquad.crew_member.domain.repository.rank.CrewRankerRepository;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;

@Sql("/mysql-truncate.sql")
@Import({ApplicationLayerConfiguration.class})
@ContextConfiguration(initializers = {
        MySqlTestContainerInitializer.class,
        RedisTestContainerInitializer.class,
})
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class CrewLeaderboardUpdateSchedulerAnalysisTest {

    @Autowired
    private CrewRankerRepository crewRankerRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CrewLeaderboardManager leaderboardManager;

    @SpyBean
    private CrewLeaderboardUpdateService leaderboardUpdateService;

    @Autowired
    private CrewLeaderboardUpdateScheduler leaderboardRefreshScheduler;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("스케줄러 갱신 중 발생하는 유저 활동의 점수 유실 검증")
    void success1() {
        // given
        Long crewId = 1L;
        Long memberId = 100L;
        CountDownLatch schedulerLatched = new CountDownLatch(1);
        CountDownLatch activityDone = new CountDownLatch(1);
        Instant activityTime = CompositeScore.BASE_DATE.toInstant().plusSeconds(1_000_000_000);
        leaderboardManager.applyActivity(crewId, memberId, activityTime.plusSeconds(20), CrewActivity.SQUAD_CREATE);

        doAnswer(invocation -> {
            invocation.callRealMethod();
            schedulerLatched.countDown();
            activityDone.await();
            return null;
        }).when(leaderboardUpdateService).updateLeaderboards(any());

        // when
        CompletableFuture<Void> schedulerFuture = CompletableFuture.runAsync(() -> leaderboardRefreshScheduler.updateLeaderboards());
        waitToStart(schedulerLatched);
        leaderboardManager.applyActivity(crewId, memberId, activityTime.plusSeconds(30), CrewActivity.CREW_PARTICIPANT);
        activityDone.countDown();
        schedulerFuture.join();

        // then
        assertSoftly(softly -> {
            softly.assertThat(stringRedisTemplate.hasKey(CrewLeaderboardKeyMapper.toLeaderboardSnapshotKey(crewId)))
                    .as("스케줄러 작업이 완료된 후 스냅샷 키는 삭제되어야 한다.")
                    .isFalse();
            softly.assertThat(leaderboardManager.getScore(crewId, memberId))
                    .as("스케줄러가 리더보드 rdb 를 갱신하는 사이 발생한 추가 활동 점수가 Redis 에서 누락되지 않고, 새로운 리더보드에 적용된다.")
                    .isEqualTo(CrewActivity.CREW_PARTICIPANT.getScore());
        });
    }

    @Test
    @Disabled
    @DisplayName("운영 중인 테이블에 대규모 데이터(10만개)를 직접 갱신(쓰기)할 떄의 성능 측정 (269ms)")
    void success2() {
        // given
        int memberCount = 100;
        int crewCount = 1000;
        List<Member> members = IntStream.rangeClosed(1, memberCount)
                .mapToObj(MemberFixture::createMember)
                .toList();
        memberJpaRepository.saveAll(members);

        List<CrewRankerCandidate> rankerCandidates = new ArrayList<>();
        for (Long crewId = 1L; crewId <= crewCount; crewId++) {
            int rank = 1;
            int score = 10000;
            for (Member member : members) {
                rankerCandidates.add(createCrewRankerCandidate(crewId, rank++, score--, member));
            }
        }
        crewRankerRepository.insertBatch(rankerCandidates);

        Instant activityTime = CompositeScore.BASE_DATE.toInstant().plusSeconds(1_000_000_000);
        for (Long crewId = 1L; crewId <= crewCount; crewId++) {
            int score = 10000;
            for (Member member : members) {
                CrewActivity crewActivity = mock(CrewActivity.class);
                when(crewActivity.getScore()).thenReturn(score--);
                leaderboardManager.applyActivity(crewId, member.getId(), activityTime.minusSeconds(1), crewActivity);
            }
        }

        // when & then
        long totalTime = stopWatch(() -> leaderboardRefreshScheduler.updateLeaderboards());
        System.out.println("totalTime: " + totalTime + "ms");
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

    private void waitToStart(CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public long stopWatch(Runnable task) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        task.run();
        stopWatch.stop();
        return (long) stopWatch.getTotalTime(TimeUnit.MILLISECONDS);
    }
}
