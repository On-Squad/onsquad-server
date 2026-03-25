package revi1337.onsquad.crew_member.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CrewLeaderboards {

    private final Map<Long, CrewLeaderboard> leaderboards;

    public static CrewLeaderboards empty() {
        return new CrewLeaderboards(Collections.emptyMap());
    }

    public CrewLeaderboards(Map<Long, CrewLeaderboard> leaderboards) {
        this.leaderboards = Collections.unmodifiableMap(leaderboards);
    }

    public boolean isEmpty() {
        return leaderboards.isEmpty();
    }

    public int size() {
        return leaderboards.size();
    }

    public Stream<CrewLeaderboard> leaderboardStream() {
        return leaderboards.values().stream();
    }

    public CrewLeaderboard getLeaderboard(Long crewId) {
        return leaderboards.get(crewId);
    }

    public List<CrewRankerCandidate> selectRankers(int limit, Map<Long, RankerProfile> profileMap) {
        return leaderboards.values().stream()
                .flatMap(leaderboard -> leaderboard.selectRankers(limit, profileMap).stream())
                .toList();
    }

    public List<Long> getAllRankerIds() {
        return leaderboards.values().stream()
                .flatMap(CrewLeaderboard::candidateStream)
                .map(CrewRankerCandidate::memberId)
                .distinct()
                .toList();
    }
}
