package revi1337.onsquad.crew_member.application.scheduler;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static revi1337.onsquad.common.fixture.CrewFixture.createCrew;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.StopWatch;
import revi1337.onsquad.common.config.ApplicationLayerConfiguration;
import revi1337.onsquad.common.container.MySqlTestContainerInitializer;
import revi1337.onsquad.common.container.RedisTestContainerInitializer;
import revi1337.onsquad.common.fixture.MemberFixture;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.crew_member.application.leaderboard.CompositeScore;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardKeyMapper;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardManager;
import revi1337.onsquad.crew_member.application.leaderboard.CrewLeaderboardUpdateService;
import revi1337.onsquad.crew_member.domain.CrewRole;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.entity.CrewMemberFactory;
import revi1337.onsquad.crew_member.domain.model.CrewActivity;
import revi1337.onsquad.crew_member.domain.model.CrewRankerCandidate;
import revi1337.onsquad.crew_member.domain.repository.rank.CrewRankerRepository;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;

@Sql("/mysql-truncate.sql")
@Import({ApplicationLayerConfiguration.class})
@ContextConfiguration(initializers = {MySqlTestContainerInitializer.class, RedisTestContainerInitializer.class})
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class CrewLeaderboardUpdateSchedulerAnalysisTest {

    private static final RedisScript<Long> SCRIPT = RedisScript.of(new ClassPathResource("db/redis/apply_score.lua"), Long.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CrewLeaderboardManager leaderboardManager;

    @SpyBean
    private CrewLeaderboardUpdateService leaderboardUpdateService;

    @Autowired
    private CrewLeaderboardUpdateScheduler leaderboardRefreshScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private CrewJpaRepository crewJpaRepository;

    @Autowired
    private CrewRankerRepository crewRankerRepository;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Nested
    class integrity {

        @Test
        @DisplayName("스케줄러 갱신 중 발생하는 유저 활동의 점수 유실 검증")
        void success() {
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
    }

    @Disabled
    @Nested
    class performance {

        @Test
        @DisplayName("[테스트 데이터 1_000] 리더보드 대량 갱신 성능 측정 [크루: 10개 | 각 크루당 100명 ⮕ 상위 50명 추출 | 실제 타겟: 500명]")
        void success10() {
            // given CrewLeaderboardUpdateService: 53ms, 85ms, 62ms, 68ms
            int memberCount = 100;
            int crewCount = 10;
            List<Member> members = setupInitialMembers(memberCount);
            List<Crew> crews = setupInitialCrews(crewCount, members);
            setupInitialCrewMembers(crews, members);
            setupPreviousWeekRankers(crewCount, members);
            setupMassiveLeaderboardData(crewCount, members);

            // when
            long totalTime = stopWatch(() -> leaderboardRefreshScheduler.updateLeaderboards());

            // then
            assertSoftly(softly -> {
                System.out.println("totalTime: " + totalTime + "ms");
                softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crew_ranker", Integer.class))
                        .isEqualTo(5 * crewCount);
                softly.assertThat(jdbcTemplate.queryForObject("SELECT score FROM crew_ranker LIMIT 1", Long.class))
                        .isEqualTo(CrewActivity.CREW_PARTICIPANT.getScore());
            });
        }

        @Test
        @DisplayName("[테스트 데이터 100_000] 리더보드 대량 갱신 성능 측정 [크루: 1,000개 | 각 크루당 100명 ⮕ 상위 50명 추출 | 실제 타겟: 50,000명]")
        void success1_000() {
            // given CrewLeaderboardUpdateService: 334ms, 456ms, 504ms, 359ms
            int memberCount = 100;
            int crewCount = 1000;
            List<Member> members = setupInitialMembers(memberCount);
            List<Crew> crews = setupInitialCrews(crewCount, members);
            setupInitialCrewMembers(crews, members);
            setupPreviousWeekRankers(crewCount, members);
            setupMassiveLeaderboardData(crewCount, members);

            // when
            long totalTime = stopWatch(() -> leaderboardRefreshScheduler.updateLeaderboards());

            // then
            assertSoftly(softly -> {
                System.out.println("totalTime: " + totalTime + "ms");
                softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crew_ranker", Integer.class))
                        .isEqualTo(5 * crewCount);
                softly.assertThat(jdbcTemplate.queryForObject("SELECT score FROM crew_ranker LIMIT 1", Long.class))
                        .isEqualTo(CrewActivity.CREW_PARTICIPANT.getScore());
            });
        }

        @Test
        @DisplayName("[테스트 데이터 500_000] 리더보드 대량 갱신 성능 측정 [크루: 5,000개 | 각 크루당 100명 ⮕ 상위 50명 추출 | 실제 타겟: 250,000명]")
        void success5_000() {
            // given CrewLeaderboardUpdateService: 1314ms, 1309ms, 1350ms, 1263ms
            int memberCount = 100;
            int crewCount = 5000;
            List<Member> members = setupInitialMembers(memberCount);
            List<Crew> crews = setupInitialCrews(crewCount, members);
            setupInitialCrewMembers(crews, members);
            setupPreviousWeekRankers(crewCount, members);
            setupMassiveLeaderboardData(crewCount, members);

            // when
            long totalTime = stopWatch(() -> leaderboardRefreshScheduler.updateLeaderboards());

            // then
            assertSoftly(softly -> {
                System.out.println("totalTime: " + totalTime + "ms");
                softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crew_ranker", Integer.class))
                        .isEqualTo(5 * crewCount);
                softly.assertThat(jdbcTemplate.queryForObject("SELECT score FROM crew_ranker LIMIT 1", Long.class))
                        .isEqualTo(CrewActivity.CREW_PARTICIPANT.getScore());
            });
        }

        @Test
        @DisplayName("[테스트 데이터 1_000_000] 리더보드 대량 갱신 성능 측정 [크루: 10,000개 | 각 크루당 100명 ⮕ 상위 50명 추출 | 실제 타겟: 500_000명]")
        void success10_000() {
            // given CrewLeaderboardUpdateService: 1925ms, 1787ms, 1980ms
            int memberCount = 100;
            int crewCount = 10_000;
            List<Member> members = setupInitialMembers(memberCount);
            List<Crew> crews = setupInitialCrews(crewCount, members);
            setupInitialCrewMembers(crews, members);
            setupPreviousWeekRankers(crewCount, members);
            setupMassiveLeaderboardData(crewCount, members);

            // when
            long totalTime = stopWatch(() -> leaderboardRefreshScheduler.updateLeaderboards());

            // then
            assertSoftly(softly -> {
                System.out.println("totalTime: " + totalTime + "ms");
                softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crew_ranker", Integer.class))
                        .isEqualTo(5 * crewCount);
                softly.assertThat(jdbcTemplate.queryForObject("SELECT score FROM crew_ranker LIMIT 1", Long.class))
                        .isEqualTo(CrewActivity.CREW_PARTICIPANT.getScore());
            });
        }

        @Test
        @DisplayName("[테스트 데이터 2_000_000] 리더보드 대량 갱신 성능 측정 [크루: 20,000개 | 각 크루당 100명 ⮕ 상위 50명 추출 | 실제 타겟: 1_000_000명]")
        void success20_000() {
            // given CrewLeaderboardUpdateService: 4471ms
            int memberCount = 100;
            int crewCount = 20_000;
            List<Member> members = setupInitialMembers(memberCount);
            List<Crew> crews = setupInitialCrews(crewCount, members);
            setupInitialCrewMembersChunk(crews, members);
            setupPreviousWeekRankersChunk(crewCount, members);
            setupMassiveLeaderboardData(crewCount, members);

            // when
            long totalTime = stopWatch(() -> leaderboardRefreshScheduler.updateLeaderboards());

            // then
            assertSoftly(softly -> {
                System.out.println("totalTime: " + totalTime + "ms");
                softly.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crew_ranker", Integer.class))
                        .isEqualTo(5 * crewCount);
                softly.assertThat(jdbcTemplate.queryForObject("SELECT score FROM crew_ranker LIMIT 1", Long.class))
                        .isEqualTo(CrewActivity.CREW_PARTICIPANT.getScore());
            });
        }
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

    private List<Member> setupInitialMembers(int memberCount) {
        List<Member> members = IntStream.rangeClosed(1, memberCount)
                .mapToObj(MemberFixture::createMember)
                .toList();
        memberJpaRepository.saveAll(members);
        return members;
    }

    private List<Crew> setupInitialCrews(int crewCount, List<Member> members) {
        List<Crew> crews = IntStream.rangeClosed(1, crewCount)
                .mapToObj(seq -> createCrew(members.get(0)))
                .toList();
        crewJpaRepository.saveAll(crews);
        return crews;
    }

    private void setupInitialCrewMembers(List<Crew> crews, List<Member> members) {
        List<CrewMember> crewMembers = crews.stream()
                .flatMap(crew -> IntStream.range(1, members.size()).mapToObj(seq -> createGeneralCrewMember(crew, members.get(seq))))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        String sql = "INSERT INTO crew_member(crew_id, member_id, role, participate_at) VALUES (?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(
                sql,
                crewMembers,
                crewMembers.size(),
                (ps, crewMember) -> {
                    ps.setLong(1, crewMember.getCrew().getId());
                    ps.setLong(2, crewMember.getMember().getId());
                    ps.setObject(3, CrewRole.GENERAL.name());
                    ps.setObject(4, now);
                }
        );
    }

    private void setupPreviousWeekRankers(int crewCount, List<Member> members) {
        List<CrewRankerCandidate> rankerCandidates = new ArrayList<>();
        for (Long crewId = 1L; crewId <= crewCount; crewId++) {
            int rank = 1;
            int score = 10000;
            for (Member member : members) {
                rankerCandidates.add(createCrewRankerCandidate(crewId, rank++, score--, member));
            }
        }
        crewRankerRepository.insertBatch(rankerCandidates);
    }

    private void setupInitialCrewMembersChunk(List<Crew> crews, List<Member> members) {
        List<CrewMember> crewMembers = crews.stream()
                .flatMap(crew -> IntStream.range(1, members.size()).mapToObj(seq -> createGeneralCrewMember(crew, members.get(seq))))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        String sql = "INSERT INTO crew_member(crew_id, member_id, role, participate_at) VALUES (?, ?, ?, ?)";

        int totalSize = crewMembers.size();
        int chunkSize = 100_000;
        for (int i = 0; i < totalSize; i += chunkSize) {
            List<CrewMember> subList = crewMembers.subList(i, Math.min(i + chunkSize, totalSize));
            jdbcTemplate.batchUpdate(
                    sql,
                    subList,
                    subList.size(),
                    (ps, crewMember) -> {
                        ps.setLong(1, crewMember.getCrew().getId());
                        ps.setLong(2, crewMember.getMember().getId());
                        ps.setObject(3, CrewRole.GENERAL.name());
                        ps.setObject(4, now);
                    }
            );
        }
    }

    private void setupPreviousWeekRankersChunk(int crewCount, List<Member> members) {
        List<CrewRankerCandidate> rankerCandidates = new ArrayList<>();
        for (long crewId = 1L; crewId <= crewCount; crewId++) {
            int rank = 1;
            int score = 10000;
            for (Member member : members) {
                rankerCandidates.add(createCrewRankerCandidate(crewId, rank++, score--, member));
            }
        }

        int totalSize = rankerCandidates.size();
        int chunkSize = 100_000;
        for (int i = 0; i < totalSize; i += chunkSize) {
            List<CrewRankerCandidate> subList = rankerCandidates.subList(i, Math.min(i + chunkSize, totalSize));
            crewRankerRepository.insertBatch(subList);
        }
    }

    private void setupMassiveLeaderboardData(int crewCount, List<Member> members) {
        Instant activityTime = CompositeScore.BASE_DATE.toInstant().plusSeconds(1_000_000_000);
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long crewId = 1L; crewId <= crewCount; crewId++) {
                byte[] key = CrewLeaderboardKeyMapper.toLeaderboardKey(crewId).getBytes();
                for (Member member : members) {
                    byte[] memberKey = CrewLeaderboardKeyMapper.toMemberKey(member.getId()).getBytes();
                    byte[] scoreToAdd = String.valueOf(CrewActivity.CREW_PARTICIPANT.getScore()).getBytes();
                    byte[] currentEpoch = String.valueOf(activityTime.minusSeconds(1).getEpochSecond()).getBytes();
                    byte[] multiplier = String.valueOf(CompositeScore.MULTIPLIER).getBytes();
                    byte[] baseEpoch = String.valueOf(CompositeScore.BASE_EPOCH_TIME).getBytes();

                    connection.scriptingCommands().eval(
                            SCRIPT.getScriptAsString().getBytes(),
                            ReturnType.INTEGER,
                            1,
                            key,
                            memberKey,
                            scoreToAdd,
                            currentEpoch,
                            multiplier,
                            baseEpoch
                    );
                }
            }
            return null;
        });
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
