package revi1337.onsquad.crew_member.domain.repository.rank;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import revi1337.onsquad.crew_member.domain.model.CrewRankerCandidate;
import revi1337.onsquad.crew_member.domain.model.RankerProfile;
import revi1337.onsquad.member.domain.vo.Nickname;

@Repository
@RequiredArgsConstructor
public class CrewRankerJdbcRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public void insertBatch(List<CrewRankerCandidate> candidates) {
        String sql = "INSERT INTO crew_ranker(crew_id, member_id, nickname, mbti, last_activity_time, score, ranks) VALUES (?, ?, ?, ?, ?, ?, ?)";
        namedJdbcTemplate.getJdbcOperations().batchUpdate(
                sql,
                candidates,
                candidates.size(),
                (ps, candidate) -> {
                    ps.setLong(1, candidate.crewId());
                    ps.setLong(2, candidate.memberId());
                    ps.setString(3, candidate.nickname());
                    ps.setString(4, candidate.mbti());
                    ps.setObject(5, candidate.lastActivityTime());
                    ps.setLong(6, candidate.score());
                    ps.setInt(7, candidate.rank());
                }
        );
    }

    public void insertBatchToShadowTable(List<CrewRankerCandidate> candidates) {
        String sql = "INSERT INTO crew_ranker_shadow(crew_id, member_id, nickname, mbti, last_activity_time, score, ranks) VALUES (?, ?, ?, ?, ?, ?, ?)";
        namedJdbcTemplate.getJdbcOperations().batchUpdate(
                sql,
                candidates,
                candidates.size(),
                (ps, candidate) -> {
                    ps.setLong(1, candidate.crewId());
                    ps.setLong(2, candidate.memberId());
                    ps.setString(3, candidate.nickname());
                    ps.setString(4, candidate.mbti());
                    ps.setObject(5, candidate.lastActivityTime());
                    ps.setLong(6, candidate.score());
                    ps.setInt(7, candidate.rank());
                }
        );
    }

    public void prepareShadowTable() {
        namedJdbcTemplate.getJdbcOperations().execute("CREATE TABLE IF NOT EXISTS crew_ranker_shadow LIKE crew_ranker");
        namedJdbcTemplate.getJdbcOperations().execute("TRUNCATE TABLE crew_ranker_shadow");
    }

    public void switchTables() {
        namedJdbcTemplate.getJdbcOperations().execute("""
                RENAME TABLE
                        crew_ranker TO crew_ranker_swapping,
                        crew_ranker_shadow TO crew_ranker,
                        crew_ranker_swapping TO crew_ranker_shadow;
                """);
    }

    public void dropShadowTable() {
        namedJdbcTemplate.getJdbcOperations().execute("DROP TABLE IF EXISTS crew_ranker_shadow");
    }

    public void truncate() {
        namedJdbcTemplate.getJdbcOperations().execute("TRUNCATE TABLE crew_ranker");
    }

    /**
     * For more information, visit <a href="https://www.h2database.com/html/functions-window.html">this link</a>.
     *
     * @see #aggregateRankedMembersGivenActivityWeight(LocalDateTime, LocalDateTime, Integer)
     * @deprecated
     */
    @Deprecated
    public List<CrewRankerCandidate> aggregateRankedMembersByActivityOccurrence(LocalDateTime from, LocalDateTime to, Integer rankLimit) {
        String sql = """
                    \n
                    SELECT
                        ranked_activities.crew_id AS crew_id,
                        ranked_activities.mem_id AS mem_id,
                        ranked_activities.mem_nickname AS mem_nickname,
                        ranked_activities.mem_mbti AS mem_mbti,
                        ranked_activities.last_activity_time AS mem_last_activity_time,
                        ranked_activities.counter AS score,
                        ranked_activities.ranks AS ranks
                    FROM (
                        SELECT
                            crew_id, mem_id, mem_nickname, mem_mbti, last_activity_time, counter,
                            DENSE_RANK() OVER (PARTITION BY crew_id ORDER BY counter DESC, last_activity_time DESC) AS ranks
                        FROM (
                            SELECT DISTINCT
                                raw_activities.crew_id AS crew_id,
                                m.id AS mem_id,
                                m.nickname AS mem_nickname,
                                m.mbti AS mem_mbti,
                                MAX(raw_activities.created_at) OVER (PARTITION BY raw_activities.crew_id, m.id) AS last_activity_time,
                                COUNT(*) OVER (PARTITION BY raw_activities.crew_id, m.id) AS counter
                            FROM (
                                -- crew participant
                                SELECT cm.crew_id, cm.member_id, cm.participate_at AS created_at
                                FROM crew_member cm
                                WHERE cm.participate_at BETWEEN :from AND :to
                                UNION ALL
                                -- squad create
                                SELECT s.crew_id, s.member_id, s.created_at AS created_at
                                FROM squad s
                                WHERE s.created_at BETWEEN :from AND :to
                                UNION ALL
                                -- squad participant
                                SELECT s.crew_id, sm.member_id, sm.participate_at AS created_at
                                FROM squad_member sm
                                INNER JOIN squad s ON s.id = sm.squad_id
                                WHERE sm.participate_at BETWEEN :from AND :to
                                UNION ALL
                                -- squad comment create
                                SELECT s.crew_id, sc.member_id, sc.created_at AS created_at
                                FROM squad_comment sc
                                INNER JOIN squad s ON s.id = sc.squad_id
                                WHERE sc.created_at BETWEEN :from AND :to
                            ) AS raw_activities
                            INNER JOIN member m ON m.id = raw_activities.member_id
                        ) AS aggregated_activities
                    ) AS ranked_activities
                    WHERE ranks <= :rankLimit
                    ORDER BY crew_id, ranks;
                """;

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to)
                .addValue("rankLimit", rankLimit);

        return namedJdbcTemplate.query(sql, sqlParameterSource, crewRankerCandidateMapper());
    }

    public List<CrewRankerCandidate> aggregateRankedMembersGivenActivityWeight(LocalDateTime from, LocalDateTime to, Integer rankLimit) {
        String sql = """
                    \n
                    SELECT
                        ranked_activities.crew_id AS crew_id,
                        ranked_activities.mem_id AS mem_id,
                        ranked_activities.mem_nickname AS mem_nickname,
                        ranked_activities.mem_mbti AS mem_mbti,
                        ranked_activities.last_activity_time AS mem_last_activity_time,
                        ranked_activities.total_score AS score,
                        ranked_activities.ranks AS ranks
                    FROM (
                        SELECT
                            crew_id, mem_id, mem_nickname, mem_mbti, last_activity_time, total_score,
                            DENSE_RANK() OVER (PARTITION BY crew_id ORDER BY total_score DESC, last_activity_time DESC) AS ranks
                        FROM (
                            SELECT DISTINCT
                                raw_activities.crew_id AS crew_id,
                                m.id AS mem_id,
                                m.nickname AS mem_nickname,
                                m.mbti AS mem_mbti,
                                MAX(raw_activities.created_at) OVER (PARTITION BY raw_activities.crew_id, m.id) AS last_activity_time,
                                SUM(raw_activities.point) OVER (PARTITION BY raw_activities.crew_id, m.id) AS total_score
                            FROM (
                                -- crew participant (Weight: 5)
                                SELECT cm.crew_id, cm.member_id, cm.participate_at AS created_at, 5 AS point
                                FROM crew_member cm
                                WHERE cm.participate_at BETWEEN :from AND :to
                                UNION ALL
                                -- squad create (Weight: 10)
                                SELECT s.crew_id, s.member_id, s.created_at AS created_at, 10 AS point
                                FROM squad s
                                WHERE s.created_at BETWEEN :from AND :to
                                UNION ALL
                                -- squad participant (Weight: 3)
                                SELECT s.crew_id, sm.member_id, sm.participate_at AS created_at, 3 AS point
                                FROM squad_member sm
                                INNER JOIN squad s ON s.id = sm.squad_id
                                WHERE sm.participate_at BETWEEN :from AND :to
                                UNION ALL
                                -- squad comment create (Weight: 1)
                                SELECT s.crew_id, sc.member_id, sc.created_at AS created_at, 1 AS point
                                FROM squad_comment sc
                                INNER JOIN squad s ON s.id = sc.squad_id
                                WHERE sc.created_at BETWEEN :from AND :to
                            ) AS raw_activities
                            INNER JOIN member m ON m.id = raw_activities.member_id
                        ) AS aggregated_activities
                    ) AS ranked_activities
                    WHERE ranks <= :rankLimit
                    ORDER BY crew_id, ranks;
                """;

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to)
                .addValue("rankLimit", rankLimit);

        return namedJdbcTemplate.query(sql, sqlParameterSource, crewRankerCandidateMapper());
    }

    public Map<Long, RankerProfile> findActiveRankersWithProfile(List<CrewRankerCandidate> candidates) {
        String inClause = candidates.stream()
                .map(r -> "(" + r.crewId() + "," + r.memberId() + ")")
                .collect(Collectors.joining(","));

        String sql = """
                SELECT m.id as member_id, m.nickname, m.mbti \
                FROM crew_member cm \
                INNER JOIN member m ON cm.member_id = m.id \
                WHERE (cm.crew_id, cm.member_id) IN (%s) \
                """.formatted(inClause);

        return namedJdbcTemplate.query(sql, rs -> {
            Map<Long, RankerProfile> result = new HashMap<>(candidates.size());
            while (rs.next()) {
                result.put(rs.getLong("member_id"), new RankerProfile(
                        new Nickname(rs.getString("nickname")), rs.getString("mbti"))
                );
            }
            return result;
        });
    }

    public Map<Long, RankerProfile> findActiveRankersWithProfile2(List<CrewRankerCandidate> candidates) {
        String valuesClause = candidates.stream()
                .map(r -> "ROW(" + r.crewId() + "," + r.memberId() + ")")
                .collect(Collectors.joining(","));

        String sql = """
                SELECT m.id AS member_id, m.nickname, m.mbti \
                FROM (VALUES %s) AS target(c_id, m_id) \
                INNER JOIN crew_member cm ON cm.crew_id = target.c_id AND cm.member_id = target.m_id \
                INNER JOIN member m ON m.id = cm.member_id \
                """.formatted(valuesClause);

        return namedJdbcTemplate.query(sql, rs -> {
            Map<Long, RankerProfile> result = new HashMap<>(candidates.size());
            while (rs.next()) {
                result.put(rs.getLong("member_id"), new RankerProfile(
                        new Nickname(rs.getString("nickname")), rs.getString("mbti")));
            }
            return result;
        });
    }

    public Map<Long, RankerProfile> findActiveRankersWithProfile3(List<CrewRankerCandidate> candidates) {
        return namedJdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Map<Long, RankerProfile>>) conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                            CREATE TEMPORARY TABLE tmp_ranker (
                                crew_id BIGINT,
                                member_id BIGINT,
                                PRIMARY KEY (crew_id, member_id)
                            )
                        """);
            }

            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO tmp_ranker (crew_id, member_id) VALUES (?, ?)")) {
                for (CrewRankerCandidate candidate : candidates) {
                    pstmt.setLong(1, candidate.crewId());
                    pstmt.setLong(2, candidate.memberId());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            Map<Long, RankerProfile> result = new HashMap<>(candidates.size());
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT m.id as member_id, m.nickname, m.mbti
                    FROM tmp_ranker t
                    JOIN crew_member cm ON cm.crew_id = t.crew_id AND cm.member_id = t.member_id
                    JOIN member m ON m.id = cm.member_id
                    """)) {

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.put(rs.getLong("member_id"), new RankerProfile(
                                new Nickname(rs.getString("nickname")), rs.getString("mbti"))
                        );
                    }
                }
            }

            return result;
        });
    }

    public void prepareTempTable() {
        namedJdbcTemplate.getJdbcOperations().execute("DROP TABLE IF EXISTS tmp_ranker");
        namedJdbcTemplate.getJdbcOperations().execute("""
                    CREATE TABLE IF NOT EXISTS tmp_ranker (
                        crew_id BIGINT,
                        member_id BIGINT
                    )
                """);
    }

    public void insertBatchToTempTable(List<CrewRankerCandidate> candidates) {
        String sql = "INSERT INTO tmp_ranker(crew_id, member_id) VALUES (?, ?)";
        namedJdbcTemplate.getJdbcOperations().batchUpdate(
                sql,
                candidates,
                candidates.size(),
                (ps, candidate) -> {
                    ps.setLong(1, candidate.crewId());
                    ps.setLong(2, candidate.memberId());
                }
        );
    }

    public void createPrimaryKeyInTempTable() {
        namedJdbcTemplate.getJdbcOperations().execute("ALTER TABLE tmp_ranker ADD PRIMARY KEY (crew_id, member_id)");
    }

    public Map<Long, RankerProfile> findActiveRankersWithProfileInTempTable(int initSize) {
        String sql = """
                SELECT m.id as member_id, m.nickname, m.mbti \
                FROM tmp_ranker t \
                JOIN crew_member cm ON cm.crew_id = t.crew_id AND cm.member_id = t.member_id \
                JOIN member m ON m.id = cm.member_id \
                """;

        return namedJdbcTemplate.query(sql, rs -> {
            Map<Long, RankerProfile> result = new HashMap<>(initSize);
            while (rs.next()) {
                result.put(rs.getLong("member_id"), new RankerProfile(
                        new Nickname(rs.getString("nickname")), rs.getString("mbti"))
                );
            }
            return result;
        });
    }

    private RowMapper<CrewRankerCandidate> crewRankerCandidateMapper() {
        return (rs, rowNum) -> new CrewRankerCandidate(
                rs.getLong("crew_id"),
                rs.getInt("ranks"),
                rs.getLong("score"),
                rs.getLong("mem_id"),
                rs.getString("mem_nickname"),
                rs.getString("mem_mbti"),
                rs.getObject("mem_last_activity_time", LocalDateTime.class)
        );
    }
}
