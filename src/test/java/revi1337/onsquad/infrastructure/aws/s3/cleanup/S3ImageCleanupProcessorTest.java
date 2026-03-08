package revi1337.onsquad.infrastructure.aws.s3.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.zaxxer.hikari.HikariDataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import revi1337.onsquad.common.config.etc.YamlPropertySourceFactory;
import revi1337.onsquad.common.config.infrastructure.AwsConfiguration;
import revi1337.onsquad.common.container.AwsTestContainerInitializer;
import revi1337.onsquad.infrastructure.aws.s3.cleanup.model.FilePaths;
import revi1337.onsquad.infrastructure.aws.s3.client.S3StorageCleaner;
import revi1337.onsquad.infrastructure.aws.s3.config.S3BucketProperties;
import revi1337.onsquad.infrastructure.aws.s3.config.S3ThreadPoolConfig;
import revi1337.onsquad.infrastructure.storage.sqlite.DeletedImage;
import revi1337.onsquad.infrastructure.storage.sqlite.ImageRecycleBinRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@TestPropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties(S3BucketProperties.class)
@ContextConfiguration(
        initializers = AwsTestContainerInitializer.class,
        classes = {AwsConfiguration.S3Configuration.class, S3ThreadPoolConfig.class, S3ImageCleanupProcessorTest.SqliteDataSourcePropertiesConfig.class}
)
@ExtendWith(SpringExtension.class)
class S3ImageCleanupProcessorTest {

    private final DataSource dataSource;
    private final S3Client s3Client;
    private final S3BucketProperties s3BucketProperties;
    private final ImageRecycleBinRepository imageRecyclebinRepository;
    private final S3ImageCleanupProcessor s3ImageCleanupProcessor;

    @Autowired
    public S3ImageCleanupProcessorTest(
            @Qualifier("s3DeletionExecutor") Executor s3DeletionExecutor,
            @Qualifier("sqliteDataSource") DataSource dataSource,
            S3Client s3Client,
            S3BucketProperties s3BucketProperties
    ) {
        this.dataSource = dataSource;
        this.s3Client = s3Client;
        this.s3BucketProperties = s3BucketProperties;
        this.imageRecyclebinRepository = new ImageRecycleBinRepository(dataSource);
        this.s3ImageCleanupProcessor = new S3ImageCleanupProcessor(
                s3DeletionExecutor,
                this.imageRecyclebinRepository,
                new S3StorageCleaner(this.s3Client, s3BucketProperties.bucket())
        );
    }

    @BeforeEach
    void setUp() {
        AwsTestContainerInitializer.flushAll();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS image_recycle_bin");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS image_recycle_bin (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    path TEXT NOT NULL,
                    retry_count INTEGER NOT NULL DEFAULT 0,
                    deleted_at DATETIME NOT NULL
                )
                """);
    }

    @Test
    @DisplayName("커서(lastId)를 기준으로 다음 배치 대상을 정확히 조회한다")
    void findTargetsWithNoOffsetCursor() {
        LocalDateTime now = LocalDateTime.now();
        List<String> paths = IntStream.rangeClosed(1, 10).mapToObj(i -> "member/file-" + i + ".txt").toList();
        insertMockData(paths, 0, now.minusDays(1));
        long lastId = 4L;
        int limit = 3;
        LocalDateTime cutOff = now.plusMinutes(1);

        FilePaths result = s3ImageCleanupProcessor.findTargets(lastId, cutOff, limit);

        assertSoftly(softly -> {
            softly.assertThat(result.size()).isEqualTo(3);
            softly.assertThat(result.getFileIds()).containsExactly(5L, 6L, 7L);
            softly.assertThat(result.getLastFileId()).isEqualTo(7L);
        });
    }

    @Test
    @DisplayName("비동기 삭제 후 성공한 파일과 실패한 파일을 구분하여 응답한다")
    void executeS3DeletionResultMapping() {
        LocalDateTime now = LocalDateTime.now();
        List<String> paths = IntStream.rangeClosed(1, 5).mapToObj(i -> "member/file-" + i + ".txt").toList();
        insertMockData(paths, 0, now.minusDays(1));
        uploadMockFile(paths);
        LocalDateTime cutOff = now.plusMinutes(1);
        FilePaths targets = s3ImageCleanupProcessor.findTargets(0L, cutOff, 100);

        S3ImageCleanupProcessor.CleanupResult result = s3ImageCleanupProcessor.executeS3Deletion(targets);

        assertSoftly(softly -> {
            softly.assertThat(result.success().size()).isEqualTo(5);
            softly.assertThat(result.failure().isEmpty()).isTrue();
            softly.assertThat(isBucketEmpty("member/")).isTrue();
        });
    }

    @Test
    @DisplayName("실패 건의 재시도 횟수를 1 증가시키고, 임계치(5회)에 도달한 건들을 추출한다")
    void updateRetryCountAndDetectionExceeded() {
        LocalDateTime now = LocalDateTime.now();
        List<String> failPaths = List.of("fail-1.txt", "fail-2.txt");
        List<String> normalPaths = List.of("normal-1.txt");
        insertMockData(failPaths, 4, now.minusDays(1));
        insertMockData(normalPaths, 0, now.minusDays(1));
        LocalDateTime cutOff = now.plusMinutes(1);
        FilePaths allTargets = s3ImageCleanupProcessor.findTargets(0L, cutOff, 100);

        FilePaths exceeded = s3ImageCleanupProcessor.updateRetryCountAndGetExceeded(allTargets);

        assertSoftly(softly -> {
            softly.assertThat(exceeded.size()).isEqualTo(2);
            softly.assertThat(exceeded.pathValues()).containsExactlyInAnyOrderElementsOf(failPaths);

            FilePaths updated = s3ImageCleanupProcessor.findTargets(0L, cutOff, 100);
            softly.assertThat(updated.filterByPaths(failPaths).stream().allMatch(f -> f.getRetryCount() == 5)).isTrue();
            softly.assertThat(updated.filterByPaths(normalPaths).stream().allMatch(f -> f.getRetryCount() == 1)).isTrue();
        });
    }

    @Test
    @DisplayName("정리가 완료된 데이터를 휴지통 DB에서 영구 삭제한다")
    void deleteFromRecycleBin() {
        LocalDateTime now = LocalDateTime.now();
        List<String> paths = List.of("del-1.txt", "del-2.txt", "del-3.txt");
        insertMockData(paths, 0, now.minusDays(1));
        LocalDateTime cutOff = now.plusMinutes(1);
        FilePaths targets = s3ImageCleanupProcessor.findTargets(0L, cutOff, 10);

        s3ImageCleanupProcessor.deleteFromRecycleBin(targets);

        assertThat(s3ImageCleanupProcessor.findTargets(0L, cutOff, 10).size()).isZero();
    }

    private void insertMockData(List<String> paths, int retryCount, LocalDateTime deletedAt) {
        List<DeletedImage> deletedImages = paths.stream()
                .map(path -> new DeletedImage(path, retryCount, deletedAt))
                .toList();

        imageRecyclebinRepository.insertBatch(deletedImages);
    }

    private void uploadMockFile(List<String> paths) {
        paths.forEach(path -> s3Client.putObject(PutObjectRequest.builder()
                        .bucket(s3BucketProperties.bucket())
                        .key(path)
                        .build(),
                RequestBody.fromString("test-content")));
    }

    private boolean isBucketEmpty(String prefix) {
        return !s3Client.listObjectsV2(req -> req.bucket(s3BucketProperties.bucket()).prefix(prefix))
                .hasContents();
    }

    @TestConfiguration
    static class SqliteDataSourcePropertiesConfig {

        @Bean
        @ConfigurationProperties(prefix = "spring.sqlite-datasource")
        public DataSourceProperties sqliteDataSourceProperties() {
            return new DataSourceProperties();
        }

        @Bean
        public DataSource sqliteDataSource(DataSourceProperties sqliteDataSourceProperties) {
            return sqliteDataSourceProperties
                    .initializeDataSourceBuilder()
                    .driverClassName(sqliteDataSourceProperties.getDriverClassName())
                    .url("jdbc:sqlite:test-file.db?foreign_keys=on&busy_timeout=3000&journal_mode=WAL&synchronous=NORMAL")
                    .type(HikariDataSource.class)
                    .build();
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource sqliteDataSource) {
            return new JdbcTransactionManager(sqliteDataSource);
        }
    }
}
