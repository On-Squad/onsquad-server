package revi1337.onsquad.infrastructure.storage.sqlite;

import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class FileRecycleBinRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FileRecycleBinRepository(@Qualifier("sqliteDataSource") DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void insert(DeletedFile deletedFile) {
        String sql = "INSERT INTO file_recycle_bin (path, retry_count, deleted_at) VALUES (:path, :retryCount, :deletedAt)";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("path", deletedFile.getPath())
                .addValue("retryCount", deletedFile.getRetryCount())
                .addValue("deletedAt", deletedFile.getDeletedAt());

        jdbcTemplate.update(sql, sqlParameterSource);
    }

    public void insertBatch(List<DeletedFile> deletedFiles) {
        String sql = "INSERT INTO file_recycle_bin (path, retry_count, deleted_at) VALUES (:path, :retryCount, :deletedAt)";
        SqlParameterSource[] batchArgs = deletedFiles.stream()
                .map(deletedFile -> new MapSqlParameterSource()
                        .addValue("path", deletedFile.getPath())
                        .addValue("retryCount", deletedFile.getRetryCount())
                        .addValue("deletedAt", deletedFile.getDeletedAt()))
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    public List<DeletedFile> findByCursor(long lastId, LocalDateTime cutOff, int batchSize) {
        String sql = """
                SELECT id, path, retry_count, deleted_at \
                FROM file_recycle_bin \
                WHERE id > :lastId AND deleted_at <= :cutOff \
                ORDER BY id ASC \
                LIMIT :batchSize
                """;

        SqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("lastId", lastId)
                .addValue("cutOff", cutOff)
                .addValue("batchSize", batchSize);

        return jdbcTemplate.query(
                sql,
                sqlParameterSource,
                (rs, rowNum) -> new DeletedFile(
                        rs.getLong("id"),
                        rs.getString("path"),
                        rs.getInt("retry_count"),
                        rs.getObject("deleted_at", LocalDateTime.class)
                )
        );
    }

    public List<DeletedFile> findByRetryCountLargerThan(int retryCount) {
        String sql = "SELECT id, path, retry_count, deleted_at FROM file_recycle_bin WHERE retry_count >= (:retryCount)";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource("retryCount", retryCount);

        return jdbcTemplate.query(
                sql,
                sqlParameterSource,
                (rs, rowNum) -> new DeletedFile(
                        rs.getLong("id"),
                        rs.getString("path"),
                        rs.getInt("retry_count"),
                        rs.getObject("deleted_at", LocalDateTime.class)
                )
        );
    }

    public int incrementRetryCount(List<Long> ids) {
        String sql = "UPDATE file_recycle_bin SET retry_count = retry_count + 1 WHERE id IN (:ids)";
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource("ids", ids);

        return jdbcTemplate.update(sql, sqlParameterSource);
    }

    public int deleteByIdIn(List<Long> ids) {
        String sql = "DELETE FROM file_recycle_bin WHERE id IN (:ids)";
        SqlParameterSource param = new MapSqlParameterSource("ids", ids);

        return jdbcTemplate.update(sql, param);
    }

    public void deleteAllInBatch() {
        jdbcTemplate.getJdbcOperations().execute("DELETE FROM file_recycle_bin");
        jdbcTemplate.getJdbcOperations().execute("UPDATE SQLITE_SEQUENCE SET seq = 0 WHERE name = 'file_recycle_bin'");
    }
}
