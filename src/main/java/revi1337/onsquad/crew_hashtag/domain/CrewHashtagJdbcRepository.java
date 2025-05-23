package revi1337.onsquad.crew_hashtag.domain;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import revi1337.onsquad.hashtag.domain.Hashtag;

@RequiredArgsConstructor
@Repository
public class CrewHashtagJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void batchInsert(Long crewId, List<Hashtag> hashtags) {
        String sql = "INSERT INTO crew_hashtag(crew_id, hashtag_id) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(
                sql,
                hashtags,
                hashtags.size(),
                (ps, hashtag) -> {
                    ps.setLong(1, crewId);
                    ps.setLong(2, hashtag.getId());
                }
        );
    }
}
