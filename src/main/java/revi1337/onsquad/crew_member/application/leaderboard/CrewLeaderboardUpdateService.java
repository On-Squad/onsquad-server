package revi1337.onsquad.crew_member.application.leaderboard;

import java.util.List;
import java.util.Map;
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
        List<CrewRankerCandidate> rankers = leaderboards.leaderboardStream()
                .flatMap(CrewLeaderboard::candidateStream)
                .toList();

        Map<Long, RankerProfile> memberMapping = crewRankerJdbcRepository.findActiveRankersWithProfile(rankers);

        return leaderboards.selectRankers(leaderboardProperties.rankLimit(), memberMapping);
    }
}
