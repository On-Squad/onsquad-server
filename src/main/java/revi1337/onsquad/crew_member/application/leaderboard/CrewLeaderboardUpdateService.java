package revi1337.onsquad.crew_member.application.leaderboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
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

    private final CrewRankerJdbcRepository crewRankerJdbcRepository;
    private final CrewLeaderboardProperties leaderboardProperties;
    private final TransactionTemplate transactionTemplate;

    public void updateLeaderboards(CrewLeaderboards leaderboards) {
        try {
            crewRankerJdbcRepository.dropShadowTable();
            crewRankerJdbcRepository.prepareShadowTable();
            List<CrewRankerCandidate> rankers = selectRankers_4(leaderboards);
            crewRankerJdbcRepository.insertBatchToShadowTable(rankers);
            crewRankerJdbcRepository.switchTables();
            log.info("[LeaderboardUpdate] swap successful. New leaderboard is now live. ({} rankers)", rankers.size());
        } catch (Exception exception) {
            log.error("[LeaderboardUpdate] Critical failure during shadow update. Original 'crew_ranker' remains intact.", exception);
            throw exception;
        }
    }

    private List<CrewRankerCandidate> selectRankers(CrewLeaderboards leaderboards) {
        List<CrewRankerCandidate> rankers = leaderboards.leaderboardStream()
                .flatMap(CrewLeaderboard::candidateStream)
                .toList();

        Map<Long, RankerProfile> memberMapping = crewRankerJdbcRepository.findActiveRankersWithProfile(rankers);

        return leaderboards.selectRankers(leaderboardProperties.rankLimit(), memberMapping);
    }

    private List<CrewRankerCandidate> selectRankers_1(CrewLeaderboards leaderboards) {
        Iterator<CrewRankerCandidate> iterator = leaderboards.leaderboardStream()
                .flatMap(CrewLeaderboard::candidateStream)
                .iterator();

        int batchSize = 5000;
        Map<Long, RankerProfile> memberMapping = new HashMap<>();
        while (iterator.hasNext()) {
            List<CrewRankerCandidate> chunk = new ArrayList<>(batchSize);
            for (int i = 0; i < batchSize && iterator.hasNext(); i++) {
                chunk.add(iterator.next());
            }
            memberMapping.putAll(crewRankerJdbcRepository.findActiveRankersWithProfile(chunk));
        }

        return leaderboards.selectRankers(leaderboardProperties.rankLimit(), memberMapping);
    }

    private List<CrewRankerCandidate> selectRankers_2(CrewLeaderboards leaderboards) {
        Iterator<CrewRankerCandidate> iterator = leaderboards.leaderboardStream()
                .flatMap(CrewLeaderboard::candidateStream)
                .iterator();

        int batchSize = 5000;
        int threadCount = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<Map<Long, RankerProfile>>> futures = new ArrayList<>();
        while (iterator.hasNext()) {
            List<CrewRankerCandidate> chunk = new ArrayList<>(batchSize);
            for (int i = 0; i < batchSize && iterator.hasNext(); i++) {
                chunk.add(iterator.next());
            }
            CompletableFuture<Map<Long, RankerProfile>> future = CompletableFuture.supplyAsync(
                    () -> crewRankerJdbcRepository.findActiveRankersWithProfile(chunk), pool);
            futures.add(future);
        }

        Map<Long, RankerProfile> memberMapping = new HashMap<>((int) (leaderboards.getAllRankerIds().size() / 0.75f) + 1);
        for (Future<Map<Long, RankerProfile>> future : futures) {
            try {
                memberMapping.putAll(future.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        pool.shutdown();

        return leaderboards.selectRankers(leaderboardProperties.rankLimit(), memberMapping);
    }

    private List<CrewRankerCandidate> selectRankers_3(CrewLeaderboards leaderboards) {
        List<CrewRankerCandidate> rankers = leaderboards.leaderboardStream()
                .flatMap(CrewLeaderboard::candidateStream)
                .toList();

        Map<Long, RankerProfile> memberMapping = transactionTemplate.execute(status -> crewRankerJdbcRepository.findActiveRankersWithProfile3(rankers));

        return leaderboards.selectRankers(leaderboardProperties.rankLimit(), memberMapping);
    }

    private List<CrewRankerCandidate> selectRankers_4(CrewLeaderboards leaderboards) {
        Iterator<CrewRankerCandidate> iterator = leaderboards.leaderboardStream()
                .flatMap(CrewLeaderboard::candidateStream)
                .iterator();

        crewRankerJdbcRepository.prepareTempTable();

        int batchSize = 10000;
        int threadCount = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        while (iterator.hasNext()) {
            List<CrewRankerCandidate> chunk = new ArrayList<>(batchSize);
            for (int i = 0; i < batchSize && iterator.hasNext(); i++) {
                chunk.add(iterator.next());
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> crewRankerJdbcRepository.insertBatchToTempTable(chunk), pool);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        pool.shutdown();

        crewRankerJdbcRepository.createPrimaryKeyInTempTable();
        Map<Long, RankerProfile> memberMapping = crewRankerJdbcRepository
                .findActiveRankersWithProfileInTempTable((int) (leaderboards.getAllRankerIds().size() / 0.75f) + 1);

        return leaderboards.selectRankers(leaderboardProperties.rankLimit(), memberMapping);
    }
}
