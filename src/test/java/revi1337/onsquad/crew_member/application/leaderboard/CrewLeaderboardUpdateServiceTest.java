package revi1337.onsquad.crew_member.application.leaderboard;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static revi1337.onsquad.common.fixture.MemberFixture.createMember;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import revi1337.onsquad.common.config.ApplicationLayerConfiguration;
import revi1337.onsquad.common.container.MySqlTestContainerInitializer;
import revi1337.onsquad.common.container.RedisTestContainerInitializer;
import revi1337.onsquad.common.fixture.CrewFixture;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.crew.domain.repository.CrewJpaRepository;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.entity.CrewMemberFactory;
import revi1337.onsquad.crew_member.domain.entity.CrewRanker;
import revi1337.onsquad.crew_member.domain.model.CrewLeaderboard;
import revi1337.onsquad.crew_member.domain.model.CrewLeaderboards;
import revi1337.onsquad.crew_member.domain.model.CrewRankerCandidate;
import revi1337.onsquad.crew_member.domain.repository.CrewMemberJpaRepository;
import revi1337.onsquad.crew_member.domain.repository.rank.CrewRankerJdbcRepository;
import revi1337.onsquad.crew_member.domain.repository.rank.CrewRankerJpaRepository;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.repository.MemberJpaRepository;

@Sql("/mysql-truncate.sql")
@Import({ApplicationLayerConfiguration.class})
@ContextConfiguration(initializers = {MySqlTestContainerInitializer.class, RedisTestContainerInitializer.class})
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class CrewLeaderboardUpdateServiceTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private CrewJpaRepository crewJpaRepository;

    @Autowired
    private CrewMemberJpaRepository crewMemberJpaRepository;

    @Autowired
    private CrewRankerJpaRepository rankerJpaRepository;

    @Autowired
    private CrewRankerJdbcRepository rankerJdbcRepository;

    @Autowired
    private CrewLeaderboardUpdateService leaderboardUpdateService;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("기존 리더보드 데이터를 초기화하고, 새로운 후보군 중 상위 순위 멤버들만 선별하여 갱신한다.")
    void updateLeaderboards() {
        //  given
        Member member1 = createMember(1);
        Member member2 = createMember(2);
        Member member3 = createMember(3);
        Member member4 = createMember(4);
        Member member5 = createMember(5);
        Member member6 = createMember(6);
        Member member7 = createMember(7);
        memberJpaRepository.saveAll(List.of(member1, member2, member3, member4, member5, member6, member7));
        Crew crew = CrewFixture.createCrew(member1);
        crew.addCrewMember(
                createGeneralCrewMember(crew, member2),
                createGeneralCrewMember(crew, member3),
                createGeneralCrewMember(crew, member4),
                createGeneralCrewMember(crew, member5),
                createGeneralCrewMember(crew, member6),
                createGeneralCrewMember(crew, member7)
        );
        crewJpaRepository.save(crew);
        rankerJdbcRepository.insertBatch(List.of(
                createCrewRankerCandidate(1L, 1, 200, member1),
                createCrewRankerCandidate(1L, 2, 100, member2)
        ));

        LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
        CrewRankerCandidate candidate1 = createCrewRankerCandidate(1L, 1, 9, member7, baseTime.plusDays(3));
        CrewRankerCandidate candidate2 = createCrewRankerCandidate(1L, 2, 8, member6, baseTime.plusDays(3).plusHours(10));
        CrewRankerCandidate candidate3 = createCrewRankerCandidate(1L, 3, 8, member5, baseTime.plusDays(2).plusHours(5));
        CrewRankerCandidate candidate4 = createCrewRankerCandidate(1L, 4, 7, member4, baseTime.plusDays(2).plusHours(6));
        CrewRankerCandidate candidate5 = createCrewRankerCandidate(1L, 5, 7, member3, baseTime.plusDays(2).plusHours(3));
        CrewRankerCandidate candidate6 = createCrewRankerCandidate(1L, 6, 6, member2, baseTime.plusDays(2));
        CrewRankerCandidate candidate7 = createCrewRankerCandidate(1L, 7, 5, member1, baseTime.plusDays(2));
        CrewLeaderboards leaderboards = new CrewLeaderboards(
                Stream.of(candidate1, candidate2, candidate3, candidate4, candidate5, candidate6, candidate7)
                        .collect(Collectors.groupingBy(CrewRankerCandidate::crewId, Collectors.collectingAndThen(Collectors.toList(), CrewLeaderboard::new)))
        );

        // when
        leaderboardUpdateService.updateLeaderboards(leaderboards);

        // then
        assertSoftly(softly -> {
            List<CrewRanker> rankers = rankerJpaRepository.findAll();
            softly.assertThat(rankers).hasSize(5);
            softly.assertThat(rankers).extracting(CrewRanker::getRank)
                    .containsExactlyInAnyOrder(1, 2, 3, 4, 5);
            softly.assertThat(rankers).extracting(CrewRanker::getNickname)
                    .containsExactlyInAnyOrder("nick7", "nick6", "nick5", "nick4", "nick3");
        });
    }

    @Test
    @DisplayName("기존 리더보드 데이터를 초기화하고, 새로운 후보군 중 상위 순위 멤버들만 선별하여 갱신한다.")
    void updateLeaderboards2() {
        //  given
        Member member1 = createMember(1);
        Member member2 = createMember(2);
        Member member3 = createMember(3);
        Member member4 = createMember(4);
        Member member5 = createMember(5);
        Member member6 = createMember(6);
        Member member7 = createMember(7);
        memberJpaRepository.saveAll(List.of(member1, member2, member3, member4, member5, member6, member7));
        Crew crew = CrewFixture.createCrew(member1);
        crew.addCrewMember(
                createGeneralCrewMember(crew, member2),
                createGeneralCrewMember(crew, member3),
                createGeneralCrewMember(crew, member4),
                createGeneralCrewMember(crew, member5),
                createGeneralCrewMember(crew, member6),
                createGeneralCrewMember(crew, member7)
        );
        crewJpaRepository.save(crew);
        rankerJdbcRepository.insertBatch(List.of(
                createCrewRankerCandidate(1L, 1, 200, member1),
                createCrewRankerCandidate(1L, 2, 100, member2)
        ));

        LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
        CrewRankerCandidate candidate1 = createCrewRankerCandidate(1L, 1, 9, member7, baseTime.plusDays(3));
        CrewRankerCandidate candidate2 = createCrewRankerCandidate(1L, 2, 8, member6, baseTime.plusDays(3).plusHours(10));
        CrewRankerCandidate candidate3 = createCrewRankerCandidate(1L, 3, 8, member5, baseTime.plusDays(2).plusHours(5));
        CrewRankerCandidate candidate4 = createCrewRankerCandidate(1L, 4, 7, member4, baseTime.plusDays(2).plusHours(6));
        CrewRankerCandidate candidate5 = createCrewRankerCandidate(1L, 5, 7, member3, baseTime.plusDays(2).plusHours(3));
        CrewRankerCandidate candidate6 = createCrewRankerCandidate(1L, 6, 6, member2, baseTime.plusDays(2));
        CrewRankerCandidate candidate7 = createCrewRankerCandidate(1L, 7, 5, member1, baseTime.plusDays(2));
        CrewLeaderboards leaderboards = new CrewLeaderboards(
                Stream.of(candidate1, candidate2, candidate3, candidate4, candidate5, candidate6, candidate7)
                        .collect(Collectors.groupingBy(CrewRankerCandidate::crewId, Collectors.collectingAndThen(Collectors.toList(), CrewLeaderboard::new)))
        );

        // when
        leaderboardUpdateService.updateLeaderboards(leaderboards);

        // then
        assertSoftly(softly -> {
            List<CrewRanker> rankers = rankerJpaRepository.findAll();
            softly.assertThat(rankers).hasSize(5);
            softly.assertThat(rankers).extracting(CrewRanker::getRank)
                    .containsExactlyInAnyOrder(1, 2, 3, 4, 5);
            softly.assertThat(rankers).extracting(CrewRanker::getNickname)
                    .containsExactlyInAnyOrder("nick7", "nick6", "nick5", "nick4", "nick3");
        });
    }

    @Test
    @DisplayName("랭킹 후보군에 탈퇴한 회원이 포함된 경우, 해당 회원을 제외하고 남은 멤버들로 순위를 재조정하여 갱신한다.")
    void updateLeaderboards3() {
        //  given
        Member member1 = createMember(1);
        Member member2 = createMember(2);
        Member member3 = createMember(3);
        Member member4 = createMember(4);
        Member member5 = createMember(5);
        Member member6 = createMember(6);
        Member member7 = createMember(7);
        memberJpaRepository.saveAll(List.of(member1, member2, member3, member4, member5, member6, member7));
        Crew crew = CrewFixture.createCrew(member1);
        crew.addCrewMember(
                createGeneralCrewMember(crew, member2),
                createGeneralCrewMember(crew, member3),
                createGeneralCrewMember(crew, member4),
                createGeneralCrewMember(crew, member5),
                createGeneralCrewMember(crew, member6),
                createGeneralCrewMember(crew, member7)
        );
        crewJpaRepository.save(crew);
        rankerJdbcRepository.insertBatch(List.of(
                createCrewRankerCandidate(1L, 1, 200, member1),
                createCrewRankerCandidate(1L, 2, 100, member2)
        ));
        crewMemberJpaRepository.deleteByCrewIdAndMemberId(crew.getId(), member7.getId());
        crewMemberJpaRepository.deleteByCrewIdAndMemberId(crew.getId(), member6.getId());
        crewMemberJpaRepository.deleteByCrewIdAndMemberId(crew.getId(), member5.getId());

        LocalDateTime baseTime = LocalDate.of(2026, 1, 4).atStartOfDay();
        CrewRankerCandidate candidate1 = createCrewRankerCandidate(1L, 1, 9, member7, baseTime.plusDays(3));
        CrewRankerCandidate candidate2 = createCrewRankerCandidate(1L, 2, 8, member6, baseTime.plusDays(3).plusHours(10));
        CrewRankerCandidate candidate3 = createCrewRankerCandidate(1L, 3, 8, member5, baseTime.plusDays(2).plusHours(5));
        CrewRankerCandidate candidate4 = createCrewRankerCandidate(1L, 4, 7, member4, baseTime.plusDays(2).plusHours(6));
        CrewRankerCandidate candidate5 = createCrewRankerCandidate(1L, 5, 7, member3, baseTime.plusDays(2).plusHours(3));
        CrewRankerCandidate candidate6 = createCrewRankerCandidate(1L, 6, 6, member2, baseTime.plusDays(2));
        CrewRankerCandidate candidate7 = createCrewRankerCandidate(1L, 7, 5, member1, baseTime.plusDays(2));
        CrewLeaderboards leaderboards = new CrewLeaderboards(
                Stream.of(candidate1, candidate2, candidate3, candidate4, candidate5, candidate6, candidate7)
                        .collect(Collectors.groupingBy(CrewRankerCandidate::crewId, Collectors.collectingAndThen(Collectors.toList(), CrewLeaderboard::new)))
        );

        // when
        leaderboardUpdateService.updateLeaderboards(leaderboards);

        // then
        assertSoftly(softly -> {
            List<CrewRanker> rankers = rankerJpaRepository.findAll();
            softly.assertThat(rankers).hasSize(4);
            softly.assertThat(rankers).extracting(CrewRanker::getRank)
                    .containsExactlyInAnyOrder(1, 2, 3, 4);
            softly.assertThat(rankers).extracting(CrewRanker::getNickname)
                    .containsExactlyInAnyOrder("nick4", "nick3", "nick2", "nick1");
        });
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

    private static CrewRankerCandidate createCrewRankerCandidate(Long crewId, int rank, long score, Member member, LocalDateTime lastActivityTime) {
        return new CrewRankerCandidate(
                crewId,
                rank,
                score,
                member.getId(),
                member.getNickname().getValue(),
                member.getMbti().name(),
                lastActivityTime
        );
    }
}
