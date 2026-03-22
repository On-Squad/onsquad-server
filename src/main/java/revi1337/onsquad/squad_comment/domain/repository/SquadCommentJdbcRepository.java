package revi1337.onsquad.squad_comment.domain.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import revi1337.onsquad.member.domain.model.SimpleMember;
import revi1337.onsquad.member.domain.vo.Introduce;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.member.domain.vo.Nickname;
import revi1337.onsquad.squad_comment.domain.model.SquadCommentDetail;

@Deprecated
@Repository
@RequiredArgsConstructor
public class SquadCommentJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<SquadCommentDetail> fetchAllChildrenByParentIdIn(Collection<Long> parentIds, int childrenSize) {
        String sql = "SELECT * FROM (" +
                "    SELECT " +
                "        squad_comment.parent_id, " +
                "        squad_comment.id, " +
                "        squad_comment.content, " +
                "        squad_comment.deleted, " +
                "        squad_comment.created_at, " +
                "        squad_comment.updated_at, " +
                "        squad_comment.member_id, " +
                "        member.id AS comment_creator_id, " +
                "        member.nickname AS comment_creator_nickname, " +
                "        member.introduce AS comment_creator_introduce, " +
                "        member.mbti AS comment_creator_mbti, " +
                "        ROW_NUMBER() OVER (PARTITION BY squad_comment.parent_id ORDER BY squad_comment.created_at DESC) AS rn "
                +
                "    FROM squad_comment " +
                "    INNER JOIN member ON squad_comment.member_id = member.id " +
                "    WHERE squad_comment.parent_id IN (:parentIds) " +
                ") AS subquery " +
                " WHERE subquery.rn <= (:childLimit)" +
                " ORDER BY subquery.rn ASC";

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("parentIds", parentIds)
                .addValue("childLimit", childrenSize);

        return jdbcTemplate.query(sql, sqlParameterSource, crewCommentRowMapper());
    }

    private RowMapper<SquadCommentDetail> crewCommentRowMapper() {
        return (rs, rowNum) -> {
            String mbtiText = rs.getString("comment_creator_mbti");
            Mbti mbti = mbtiText != null ? Mbti.valueOf(mbtiText) : null;

            return new SquadCommentDetail(
                    rs.getLong("parent_id"),
                    rs.getLong("id"),
                    rs.getString("content"),
                    rs.getBoolean("deleted"),
                    rs.getObject("created_at", LocalDateTime.class),
                    rs.getObject("updated_at", LocalDateTime.class),
                    new SimpleMember(
                            rs.getLong("comment_creator_id"),
                            new Nickname(rs.getString("comment_creator_nickname")),
                            new Introduce(rs.getString("comment_creator_introduce")),
                            mbti
                    )
            );
        };
    }
}
