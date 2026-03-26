package revi1337.onsquad.crew_member.application.leaderboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import revi1337.onsquad.crew_member.config.CrewLeaderboardProperties;
import revi1337.onsquad.crew_member.domain.model.CrewLeaderboard;
import revi1337.onsquad.crew_member.domain.model.CrewLeaderboards;
import revi1337.onsquad.crew_member.domain.model.CrewRankerCandidate;
import revi1337.onsquad.crew_member.domain.model.RankerProfile;
import revi1337.onsquad.crew_member.domain.repository.rank.CrewRankerJdbcRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrewLeaderboardUpdateService {

    private static final int QUERY_BATCH_SIZE = 5000;
    private static final int SINGLE_QUERY_THRESHOLD = 100_000;
    private static final int PARALLEL_STRATEGY_THRESHOLD = 500_000;

    private final Executor leaderboardProfileExecutor;
    private final CrewRankerJdbcRepository crewRankerJdbcRepository;
    private final CrewLeaderboardProperties leaderboardProperties;

    public void updateLeaderboards(CrewLeaderboards leaderboards) {
        try {
            crewRankerJdbcRepository.dropShadowTable();
            crewRankerJdbcRepository.prepareShadowTable();
            List<CrewRankerCandidate> rankers = selectRankers(leaderboards);
            crewRankerJdbcRepository.insertBatchToShadowTable(rankers);
            crewRankerJdbcRepository.switchTables();
            log.info("[LeaderboardUpdate] swap successful. New leaderboard is now live. ({} rankers)", rankers.size());
        } catch (Exception exception) {
            log.error("[LeaderboardUpdate] Critical failure during shadow update. Original 'crew_ranker' remains intact.", exception);
            throw exception;
        }
    }

    private List<CrewRankerCandidate> selectRankers(CrewLeaderboards leaderboards) {
        List<CrewRankerCandidate> rankerCandidates = leaderboards.leaderboardStream()
                .flatMap(CrewLeaderboard::candidateStream)
                .toList();

        int totalSize = rankerCandidates.size();
        Map<Long, RankerProfile> memberMapping;
        if (totalSize <= SINGLE_QUERY_THRESHOLD) {
            memberMapping = crewRankerJdbcRepository.findActiveRankersWithProfile(rankerCandidates);
        } else {
            memberMapping = new HashMap<>((int) (leaderboards.getAllRankerIds().size() / 0.75f) + 1);
            if (totalSize <= PARALLEL_STRATEGY_THRESHOLD) {
                fetchProfilesSequentially(rankerCandidates, memberMapping);
            } else {
                fetchProfilesInParallel(rankerCandidates, memberMapping);
            }
        }

        return leaderboards.selectRankers(leaderboardProperties.rankLimit(), memberMapping);
    }

    private void fetchProfilesSequentially(List<CrewRankerCandidate> candidates, Map<Long, RankerProfile> targetMap) {
        for (int i = 0; i < candidates.size(); i += QUERY_BATCH_SIZE) {
            List<CrewRankerCandidate> chunk = createChunk(candidates, i);
            targetMap.putAll(crewRankerJdbcRepository.findActiveRankersWithProfile(chunk));
        }
    }

    private void fetchProfilesInParallel(List<CrewRankerCandidate> candidates, Map<Long, RankerProfile> targetMap) {
        List<CompletableFuture<Map<Long, RankerProfile>>> futures = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i += QUERY_BATCH_SIZE) {
            int start = i;
            CompletableFuture<Map<Long, RankerProfile>> future = CompletableFuture.supplyAsync(() -> {
                List<CrewRankerCandidate> chunk = createChunk(candidates, start);
                return crewRankerJdbcRepository.findActiveRankersWithProfile(chunk);
            }, leaderboardProfileExecutor);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        futures.forEach(f -> targetMap.putAll(f.join()));
    }

    private List<CrewRankerCandidate> createChunk(List<CrewRankerCandidate> rankers, int start) {
        int end = Math.min(start + QUERY_BATCH_SIZE, rankers.size());
        List<CrewRankerCandidate> chunk = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            chunk.add(rankers.get(i));
        }
        return chunk;
    }
}
