package revi1337.onsquad.crew_member.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
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

class CrewLeaderboardsTest {

    @Test
    @DisplayName("크루 ID를 통해 특정 크루의 리더보드를 정확히 조회한다")
    void getLeaderboard() {
        Long crewId = 1L;
        CrewLeaderboard expected = new CrewLeaderboard(List.of(createCrewRankerCandidate(crewId, 1, 100L, 10L)));
        CrewLeaderboards leaderboards = new CrewLeaderboards(Map.of(crewId, expected));

        CrewLeaderboard actual = leaderboards.getLeaderboard(crewId);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("모든 크루별로 지정된 limit만큼의 랭커들을 선별하여 통합 리스트로 반환한다")
    void selectRankers() {
        CrewLeaderboards leaderboards = new CrewLeaderboards(Map.of(
                1L, new CrewLeaderboard(List.of(
                        createCrewRankerCandidate(1L, 1, 110L, 10L),
                        createCrewRankerCandidate(1L, 2, 109L, 11L),
                        createCrewRankerCandidate(1L, 3, 108L, 12L),
                        createCrewRankerCandidate(1L, 4, 107L, 13L)
                )),
                2L, new CrewLeaderboard(List.of(
                        createCrewRankerCandidate(2L, 1, 99L, 14L),
                        createCrewRankerCandidate(2L, 2, 98L, 15L),
                        createCrewRankerCandidate(2L, 3, 97L, 16L)
                ))
        ));
        Map<Long, RankerProfile> profileMap = Map.of(
                10L, new RankerProfile(new Nickname("name-10"), peekRandomMbti()),
                11L, new RankerProfile(new Nickname("name-11"), peekRandomMbti()),
                12L, new RankerProfile(new Nickname("name-12"), peekRandomMbti()),
                13L, new RankerProfile(new Nickname("name-13"), peekRandomMbti()),
                14L, new RankerProfile(new Nickname("name-14"), peekRandomMbti()),
                15L, new RankerProfile(new Nickname("name-15"), peekRandomMbti()),
                16L, new RankerProfile(new Nickname("name-16"), peekRandomMbti())
        );

        List<CrewRankerCandidate> result = leaderboards.selectRankers(2, profileMap);

        assertThat(result).hasSize(4)
                .extracting(CrewRankerCandidate::memberId)
                .containsExactlyInAnyOrder(10L, 11L, 14L, 15L);
    }

    @Test
    @DisplayName("각 크루별로 탈퇴하거나 정보가 없는 멤버를 제외하고, 남은 후보자들로 순위를 빈틈없이 재구성하여 선별한다")
    void selectRankers2() {
        CrewLeaderboards leaderboards = new CrewLeaderboards(Map.of(
                1L, new CrewLeaderboard(List.of(
                        createCrewRankerCandidate(1L, 1, 110L, 10L),
                        createCrewRankerCandidate(1L, 2, 109L, 11L),
                        createCrewRankerCandidate(1L, 3, 108L, 12L),
                        createCrewRankerCandidate(1L, 4, 107L, 13L)
                )),
                2L, new CrewLeaderboard(List.of(
                        createCrewRankerCandidate(2L, 1, 99L, 14L),
                        createCrewRankerCandidate(2L, 2, 98L, 15L),
                        createCrewRankerCandidate(2L, 3, 97L, 16L)
                ))
        ));
        Map<Long, RankerProfile> profileMap = Map.of(
                11L, new RankerProfile(new Nickname("name-11"), peekRandomMbti()),
                12L, new RankerProfile(new Nickname("name-12"), peekRandomMbti()),
                13L, new RankerProfile(new Nickname("name-13"), peekRandomMbti()),
                15L, new RankerProfile(new Nickname("name-15"), peekRandomMbti()),
                16L, new RankerProfile(new Nickname("name-16"), peekRandomMbti())
        );

        List<CrewRankerCandidate> result = leaderboards.selectRankers(3, profileMap);

        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(5)
                    .extracting(CrewRankerCandidate::memberId)
                    .containsExactlyInAnyOrder(11L, 12L, 13L, 15L, 16L);

            softly.assertThat(result)
                    .extracting("crewId", "rank", "memberId")
                    .containsExactlyInAnyOrder(
                            tuple(1L, 1, 11L), tuple(1L, 2, 12L), tuple(1L, 3, 13L),
                            tuple(2L, 1, 15L), tuple(2L, 2, 16L)
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
