package revi1337.onsquad.crew_member.application.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import revi1337.onsquad.common.container.RedisTestContainerInitializer;
import revi1337.onsquad.crew_member.domain.model.CrewActivity;
import revi1337.onsquad.crew_member.domain.model.CrewLeaderboards;

@ContextConfiguration(
        initializers = RedisTestContainerInitializer.class,
        classes = {CrewLeaderboardManager.class, CrewLeaderboardSnapshotManager.class}
)
@ImportAutoConfiguration({RedisAutoConfiguration.class, JacksonAutoConfiguration.class})
@ExtendWith(SpringExtension.class)
class CrewLeaderboardSnapshotManagerTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CrewLeaderboardManager leaderboardManager;

    @Autowired
    private CrewLeaderboardSnapshotManager leaderboardSnapshotManager;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("[Redis Lua + RENAME 기반] 기존 리더보드 키가 스냅샷 키로 이름이 변경되어 격리된다")
    void captureSnapshots() {
        Long crewId = 1L;
        List<Long> memberIds = List.of(1L, 2L, 3L);
        Instant baseTime = CompositeScore.BASE_DATE.toInstant();
        leaderboardManager.applyActivity(crewId, memberIds.get(0), baseTime, CrewActivity.CREW_PARTICIPANT);
        leaderboardManager.applyActivity(crewId, memberIds.get(1), baseTime.plusSeconds(10), CrewActivity.SQUAD_CREATE);
        leaderboardManager.applyActivity(crewId, memberIds.get(2), baseTime.plusSeconds(10), CrewActivity.SQUAD_COMMENT);
        assertThat(stringRedisTemplate.hasKey(CrewLeaderboardKeyMapper.toLeaderboardKey(crewId))).isTrue();

        List<String> snapshotKeys = leaderboardSnapshotManager.captureSnapshots();

        assertSoftly(softly -> {
            softly.assertThat(snapshotKeys).hasSize(1);
            softly.assertThat(stringRedisTemplate.hasKey(CrewLeaderboardKeyMapper.toLeaderboardKey(crewId))).isFalse();
            softly.assertThat(stringRedisTemplate.hasKey(snapshotKeys.get(0))).isTrue();
        });
    }

    @Test
    @DisplayName("격리된 스냅샷 데이터로부터 순위, 점수, 활동 시간이 포함된 후보자 목록을 정확히 추출한다")
    void getSnapshots() {
        Long crewId = 1L;
        List<Long> memberIds = List.of(1L, 2L, 3L);
        Instant baseTime = CompositeScore.BASE_DATE.toInstant();
        leaderboardManager.applyActivity(crewId, memberIds.get(0), baseTime.plusSeconds(10), CrewActivity.SQUAD_COMMENT);
        leaderboardManager.applyActivity(crewId, memberIds.get(1), baseTime.plusSeconds(20), CrewActivity.SQUAD_COMMENT);
        leaderboardManager.applyActivity(crewId, memberIds.get(2), baseTime.plusSeconds(40), CrewActivity.SQUAD_CREATE);
        assertThat(stringRedisTemplate.hasKey(CrewLeaderboardKeyMapper.toLeaderboardKey(crewId))).isTrue();
        leaderboardSnapshotManager.captureSnapshots();

        CrewLeaderboards snapshots = leaderboardSnapshotManager.getSnapshots(List.of(crewId), 3);

        assertSoftly(softly -> {
            softly.assertThat(snapshots.size()).isEqualTo(1);
            softly.assertThat(snapshots.getLeaderboard(crewId).size()).isEqualTo(3);
            softly.assertThat(snapshots.getLeaderboard(crewId).candidateStream().toList())
                    .extracting("rank", "memberId")
                    .containsExactlyInAnyOrder(
                            tuple(1, memberIds.get(2)),
                            tuple(2, memberIds.get(1)),
                            tuple(3, memberIds.get(0))
                    );
        });
    }

    @Test
    @DisplayName("크루 ID 목록을 전달하면, 해당 크루들의 리더보드 스냅샷 키를 Redis에서 영구 삭제한다")
    void removeSnapshots() {
        List<Long> crewIds = List.of(1L, 2L, 3L);
        List<Long> memberIds = List.of(1L, 2L, 3L);
        Instant baseTime = CompositeScore.BASE_DATE.toInstant();
        leaderboardManager.applyActivity(crewIds.get(0), memberIds.get(0), baseTime.plusSeconds(10), CrewActivity.SQUAD_COMMENT);
        leaderboardManager.applyActivity(crewIds.get(1), memberIds.get(1), baseTime.plusSeconds(20), CrewActivity.SQUAD_COMMENT);
        leaderboardManager.applyActivity(crewIds.get(2), memberIds.get(2), baseTime.plusSeconds(40), CrewActivity.SQUAD_CREATE);
        assertThat(stringRedisTemplate.hasKey(CrewLeaderboardKeyMapper.toLeaderboardKey(crewIds.get(0)))).isTrue();
        assertThat(stringRedisTemplate.hasKey(CrewLeaderboardKeyMapper.toLeaderboardKey(crewIds.get(1)))).isTrue();
        assertThat(stringRedisTemplate.hasKey(CrewLeaderboardKeyMapper.toLeaderboardKey(crewIds.get(2)))).isTrue();
        leaderboardSnapshotManager.captureSnapshots();

        leaderboardSnapshotManager.removeSnapshots(crewIds);

        assertSoftly(softly -> {
            softly.assertThat(stringRedisTemplate.hasKey(CrewLeaderboardKeyMapper.toLeaderboardKey(crewIds.get(0)))).isFalse();
            softly.assertThat(stringRedisTemplate.hasKey(CrewLeaderboardKeyMapper.toLeaderboardKey(crewIds.get(1)))).isFalse();
            softly.assertThat(stringRedisTemplate.hasKey(CrewLeaderboardKeyMapper.toLeaderboardKey(crewIds.get(2)))).isFalse();
        });
    }
}
