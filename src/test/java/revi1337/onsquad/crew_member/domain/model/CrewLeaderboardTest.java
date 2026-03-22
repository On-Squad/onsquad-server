package revi1337.onsquad.crew_member.domain.model;

import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.member.domain.vo.Nickname;

class CrewLeaderboardTest {

    @Test
    @DisplayName("프로필 정보가 존재하는 멤버만 선별하고, 정해진 limit 내에서 순위를 재할당하여 반환한다")
    void selectRankers() {
        Long crewId = 1L;
        CrewRankerCandidate candidate1 = createCrewRankerCandidate(crewId, 1, 99L, 10L);
        CrewRankerCandidate candidate2 = createCrewRankerCandidate(crewId, 2, 90L, 20L);
        CrewRankerCandidate candidate3 = createCrewRankerCandidate(crewId, 3, 80L, 30L);
        CrewRankerCandidate candidate4 = createCrewRankerCandidate(crewId, 4, 70L, 40L);
        CrewRankerCandidate candidate5 = createCrewRankerCandidate(crewId, 5, 60L, 50L);
        CrewLeaderboard leaderboard = new CrewLeaderboard(List.of(candidate1, candidate2, candidate3, candidate4, candidate5));
        Map<Long, RankerProfile> profileMap = Map.of(
                10L, new RankerProfile(new Nickname("name-10"), peekRandomMbti()),
                20L, new RankerProfile(new Nickname("name-20"), peekRandomMbti()),
                30L, new RankerProfile(new Nickname("name-30"), peekRandomMbti()),
                40L, new RankerProfile(new Nickname("name-40"), peekRandomMbti()),
                50L, new RankerProfile(new Nickname("name-50"), peekRandomMbti())
        );

        List<CrewRankerCandidate> result = leaderboard.selectRankers(4, profileMap);

        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(4);
            softly.assertThat(result).extracting("rank", "memberId").containsExactly(
                    tuple(1, 10L), tuple(2, 20L), tuple(3, 30L), tuple(4, 40L)
            );
        });
    }

    @Test
    @DisplayName("랭킹 중간에 누락된 멤버가 발생하더라도, 남은 멤버들의 순위를 1위부터 촘촘하게 재할당하여 선별한다")
    void selectRankers2() {
        Long crewId = 1L;
        CrewRankerCandidate candidate1 = createCrewRankerCandidate(crewId, 1, 99L, 10L); // delete
        CrewRankerCandidate candidate2 = createCrewRankerCandidate(crewId, 2, 90L, 20L);
        CrewRankerCandidate candidate3 = createCrewRankerCandidate(crewId, 3, 80L, 30L); // delete
        CrewRankerCandidate candidate4 = createCrewRankerCandidate(crewId, 4, 70L, 40L);
        CrewRankerCandidate candidate5 = createCrewRankerCandidate(crewId, 5, 60L, 50L);
        CrewLeaderboard leaderboard = new CrewLeaderboard(List.of(candidate1, candidate2, candidate3, candidate4, candidate5));
        Map<Long, RankerProfile> profileMap = Map.of(
                20L, new RankerProfile(new Nickname("name-20"), peekRandomMbti()),
                40L, new RankerProfile(new Nickname("name-40"), peekRandomMbti()),
                50L, new RankerProfile(new Nickname("name-50"), peekRandomMbti())
        );

        List<CrewRankerCandidate> result = leaderboard.selectRankers(5, profileMap);

        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(3);
            softly.assertThat(result).extracting("rank", "memberId").containsExactlyInAnyOrder(
                    tuple(1, 20L), tuple(2, 40L), tuple(3, 50L)
            );
        });
    }

    private static CrewRankerCandidate createCrewRankerCandidate(Long crewId, int rank, long score, Long memberId) {
        return new CrewRankerCandidate(
                crewId,
                rank,
                score,
                memberId,
                "name" + memberId,
                peekRandomMbti().name(),
                LocalDateTime.now()
        );
    }

    private static Mbti peekRandomMbti() {
        List<Mbti> mbtiList = Arrays.asList(Mbti.values());
        Collections.shuffle(mbtiList);
        return mbtiList.get(0);
    }
}
