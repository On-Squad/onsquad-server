package revi1337.onsquad.crew_member.application.leaderboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
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
    private static final int SINGLE_QUERY_THRESHOLD = 50_000;
    private static final int PARALLEL_STRATEGY_THRESHOLD = 250_000;

    private final Executor leaderboardProfileExecutor;
    private final CrewRankerJdbcRepository crewRankerJdbcRepository;
    private final CrewLeaderboardProperties leaderboardProperties;

    public void updateLeaderboards(CrewLeaderboards leaderboards) {
        try {
            crewRankerJdbcRepository.dropShadowTable();
            crewRankerJdbcRepository.prepareShadowTable();
            List<CrewRankerCandidate> rankers = selectRankers3(leaderboards);
            crewRankerJdbcRepository.insertBatchToShadowTable(rankers);
            crewRankerJdbcRepository.switchTables();
            log.info("[LeaderboardUpdate] swap successful. New leaderboard is now live. ({} rankers)", rankers.size());
        } catch (Exception exception) {
            log.error("[LeaderboardUpdate] Critical failure during shadow update. Original 'crew_ranker' remains intact.", exception);
            throw exception;
        }
    }

    private List<CrewRankerCandidate> selectRankers1(CrewLeaderboards leaderboards) {
        List<CrewRankerCandidate> rankerCandidates = leaderboards.leaderboardStream()
                .flatMap(CrewLeaderboard::candidateStream)
                .toList();

        int totalSize = rankerCandidates.size();
        Map<Long, RankerProfile> memberMapping;
        if (totalSize <= SINGLE_QUERY_THRESHOLD) {
            memberMapping = crewRankerJdbcRepository.findActiveRankersWithProfile(rankerCandidates);
        } else {
            memberMapping = new HashMap<>((int) (leaderboards.getAllRankerIds().size() / 0.75f) + 1);
            if (totalSize < PARALLEL_STRATEGY_THRESHOLD) {
                fetchProfilesSequentially(rankerCandidates, memberMapping);
            } else {
                fetchProfilesInParallel(rankerCandidates, memberMapping);
            }
        }

        return leaderboards.selectRankers(leaderboardProperties.rankLimit(), memberMapping);
    }

    private List<CrewRankerCandidate> selectRankers2(CrewLeaderboards leaderboards) {
        List<CrewRankerCandidate> candidates = leaderboards.leaderboardStream()
                .flatMap(CrewLeaderboard::candidateStream)
                .toList();

        int taskCount = 0;
        CompletionService<Map<Long, RankerProfile>> completionService = new ExecutorCompletionService<>(leaderboardProfileExecutor);
        for (int i = 0; i < candidates.size(); i += QUERY_BATCH_SIZE) {
            List<CrewRankerCandidate> chunk = candidates.subList(i, Math.min(i + QUERY_BATCH_SIZE, candidates.size()));
            completionService.submit(() -> crewRankerJdbcRepository.findActiveRankersWithProfile(chunk));
            taskCount++;
        }
        Map<Long, RankerProfile> memberMapping = new HashMap<>((int) (leaderboards.getAllRankerIds().size() / 0.75f) + 1);
        try {
            for (int i = 0; i < taskCount; i++) {
                Future<Map<Long, RankerProfile>> future = completionService.take();
                memberMapping.putAll(future.get());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return leaderboards.selectRankers(leaderboardProperties.rankLimit(), memberMapping);
    }

    private List<CrewRankerCandidate> selectRankers3(CrewLeaderboards leaderboards) {
        Iterator<CrewRankerCandidate> iterator = leaderboards.leaderboardStream()
                .flatMap(CrewLeaderboard::candidateStream)
                .iterator();

        int taskCount = 0;
        CompletionService<Map<Long, RankerProfile>> completionService = new ExecutorCompletionService<>(leaderboardProfileExecutor);
        while (iterator.hasNext()) {
            List<CrewRankerCandidate> chunk = new ArrayList<>((int) (QUERY_BATCH_SIZE / 0.75f) + 1);
            for (int i = 0; i < QUERY_BATCH_SIZE && iterator.hasNext(); i++) {
                chunk.add(iterator.next());
            }
            completionService.submit(() -> crewRankerJdbcRepository.findActiveRankersWithProfile(chunk));
            taskCount++;
        }
        Map<Long, RankerProfile> memberMapping = new HashMap<>((int) (leaderboards.getAllRankerIds().size() / 0.75f) + 1);
        try {
            for (int i = 0; i < taskCount; i++) {
                Future<Map<Long, RankerProfile>> future = completionService.take();
                memberMapping.putAll(future.get());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return leaderboards.selectRankers(leaderboardProperties.rankLimit(), memberMapping);
    }

    private void fetchProfilesSequentially(List<CrewRankerCandidate> candidates, Map<Long, RankerProfile> targetMap) {
        for (int i = 0; i < candidates.size(); i += QUERY_BATCH_SIZE) {
            List<CrewRankerCandidate> chunk = candidates.subList(i, Math.min(i + QUERY_BATCH_SIZE, candidates.size()));
            targetMap.putAll(crewRankerJdbcRepository.findActiveRankersWithProfile(chunk));
        }
    }

    private void fetchProfilesInParallel(List<CrewRankerCandidate> candidates, Map<Long, RankerProfile> targetMap) {
        CompletionService<Map<Long, RankerProfile>> completionService = new ExecutorCompletionService<>(leaderboardProfileExecutor);
        int taskCount = 0;
        for (int i = 0; i < candidates.size(); i += QUERY_BATCH_SIZE) {
            List<CrewRankerCandidate> chunk = candidates.subList(i, Math.min(i + QUERY_BATCH_SIZE, candidates.size()));
            completionService.submit(() -> crewRankerJdbcRepository.findActiveRankersWithProfile(chunk));
            taskCount++;
        }
        try {
            for (int i = 0; i < taskCount; i++) {
                Future<Map<Long, RankerProfile>> future = completionService.take();
                targetMap.putAll(future.get());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
