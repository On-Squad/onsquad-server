package revi1337.onsquad.crew_member.application.leaderboard;

import io.lettuce.core.RedisException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import revi1337.onsquad.crew_member.domain.model.CrewLeaderboard;
import revi1337.onsquad.crew_member.domain.model.CrewLeaderboards;
import revi1337.onsquad.crew_member.domain.model.CrewRankerCandidate;
import revi1337.onsquad.infrastructure.storage.redis.RedisCacheEvictor;
import revi1337.onsquad.infrastructure.storage.redis.RedisDataStructureUtils;
import revi1337.onsquad.infrastructure.storage.redis.RedisDataStructureUtils.ZSetRange;
import revi1337.onsquad.infrastructure.storage.redis.RedisScanUtils.ScanSize;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrewLeaderboardSnapshotManager {

    private static final RedisScript<List> APPLY_SNAPSHOT_SCRIPT = RedisScript.of(new ClassPathResource("db/redis/snapshot_leaderboard.lua"), List.class);

    private final StringRedisTemplate stringRedisTemplate;

    public List<String> captureSnapshots() {
        try {
            List<String> snapshotKeys = stringRedisTemplate.execute(
                    APPLY_SNAPSHOT_SCRIPT,
                    Collections.emptyList(),
                    CrewLeaderboardKeyMapper.getLeaderboardPattern(),
                    String.valueOf(ScanSize.DEFAULT.getCount()),
                    CrewLeaderboardKeyMapper.LEADERBOARD_SNAPSHOT_POSTFIX
            );
            return snapshotKeys != null ? snapshotKeys : Collections.emptyList();
        } catch (RedisException exception) {
            log.error("[Leaderboard-Snapshot] Critical failure during Lua script execution.", exception);
            throw exception;
        }
    }

    public CrewLeaderboards getSnapshots(List<Long> crewIds, int rankLimitInclusive) {
        List<String> computedKeys = CrewLeaderboardKeyMapper.toLeaderboardSnapshotKeys(crewIds);
        if (computedKeys.isEmpty()) {
            return CrewLeaderboards.empty();
        }

        ZSetRange range = RedisDataStructureUtils.toZSetRange(1, rankLimitInclusive);
        RedisSerializer<String> serializer = stringRedisTemplate.getStringSerializer();
        List<Object> pipelinedResults = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String computedKey : computedKeys) {
                byte[] serializedKey = serializer.serialize(computedKey);
                if (serializedKey != null) {
                    connection.zSetCommands().zRevRangeWithScores(serializedKey, range.start(), range.end());
                }
            }
            return null;
        });

        Map<Long, CrewLeaderboard> leaderboards = new HashMap<>();
        for (int i = 0; i < computedKeys.size(); i++) {
            if (!(pipelinedResults.get(i) instanceof Set<?> rawSet) || rawSet.isEmpty()) {
                continue;
            }
            Long crewId = CrewLeaderboardKeyMapper.parseCrewIdFromKey(computedKeys.get(i));
            leaderboards.put(crewId, convertToLeaderboard(crewId, 1, (Set<TypedTuple<String>>) rawSet));
        }

        return new CrewLeaderboards(leaderboards);
    }

    public void clearSnapshots(List<String> snapshotKeys) {
        RedisCacheEvictor.unlinkKeys(stringRedisTemplate, snapshotKeys);
    }

    public void removeSnapshots(List<Long> crewIds) {
        List<String> snapshotKeys = CrewLeaderboardKeyMapper.toLeaderboardSnapshotKeys(crewIds);
        RedisCacheEvictor.unlinkKeys(stringRedisTemplate, snapshotKeys);
    }

    private CrewLeaderboard convertToLeaderboard(Long crewId, int startRank, Set<TypedTuple<String>> tuples) {
        int currentRank = startRank;
        List<CrewRankerCandidate> candidates = new ArrayList<>(tuples.size());
        for (TypedTuple<String> tuple : tuples) {
            CompositeScore compositeScore = CompositeScore.from(tuple.getScore());
            candidates.add(CrewRankerCandidate.from(
                    crewId,
                    CrewLeaderboardKeyMapper.parseMemberIdFromKey(tuple.getValue()),
                    currentRank++,
                    compositeScore.getActualScore(),
                    compositeScore.getActivityTime()
            ));
        }

        return new CrewLeaderboard(candidates);
    }
}
