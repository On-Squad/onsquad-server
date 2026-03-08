package revi1337.onsquad.infrastructure.aws.s3.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import org.springframework.util.StopWatch;
import revi1337.onsquad.common.config.etc.YamlPropertySourceFactory;
import revi1337.onsquad.common.config.infrastructure.AwsConfiguration;
import revi1337.onsquad.common.container.AwsTestContainerInitializer;
import revi1337.onsquad.infrastructure.aws.s3.cleanup.S3CleanupOrchestratorPerformanceTest.SqliteDataSourcePropertiesConfig;
import revi1337.onsquad.infrastructure.aws.s3.client.S3StorageCleaner;
import revi1337.onsquad.infrastructure.aws.s3.config.S3BucketProperties;
import revi1337.onsquad.infrastructure.aws.s3.config.S3ThreadPoolConfig;
import revi1337.onsquad.infrastructure.aws.s3.notification.S3FailNotificationProvider;
import revi1337.onsquad.infrastructure.storage.sqlite.DeletedImage;
import revi1337.onsquad.infrastructure.storage.sqlite.ImageRecycleBinRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Disabled("Performance 테스트는 속도 문제로 수동 검증 시에만 단독 실행한다. (CI/CD 에서 문제 발생 가능)")
@TestPropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties(S3BucketProperties.class)
@ContextConfiguration(
        initializers = AwsTestContainerInitializer.class,
        classes = {AwsConfiguration.S3Configuration.class, S3ThreadPoolConfig.class, SqliteDataSourcePropertiesConfig.class}
)
@ExtendWith(SpringExtension.class)
class S3CleanupOrchestratorPerformanceTest {

    private static final String CREATE_RECYCLE_BIN_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS image_recycle_bin (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                path TEXT NOT NULL,
                retry_count INTEGER NOT NULL DEFAULT 0,
                deleted_at DATETIME NOT NULL
            )
            """;

    private static final String DROP_RECYCLE_BIN_TABLE_SQL = "DROP TABLE IF EXISTS image_recycle_bin";

    private static final String INSERT_MOCK_DATA_SQL = """
            INSERT INTO image_recycle_bin (path, retry_count, deleted_at)
            WITH RECURSIVE cnt(n) AS (
                SELECT 1
                UNION ALL
                SELECT n + 1 FROM cnt WHERE n < 100000
            )
            SELECT 'member/file-' || n || '.txt', 0, datetime('now', 'localtime') FROM cnt;
            """;

    private static final String[] S3_MOCK_SETUP_COMMAND = {
            "sh", "-c",
            "mkdir -p /tmp/mock && " +
                    "seq 1 100000 | xargs -I{} -P 10 touch /tmp/mock/file-{}.txt && " +
                    "awslocal s3 sync /tmp/mock s3://onsquad-bucket/member/ --quiet && " +
                    "rm -rf /tmp/mock"
    };

    private final S3Client s3Client;
    private final S3BucketProperties s3BucketProperties;
    private final JdbcTemplate jdbcTemplate;
    private final ImageRecycleBinRepository imageRecyclebinRepository;
    private final S3ImageCleanupProcessor s3ImageCleanupProcessor;
    private final S3FailNotificationProvider notificationProvider;
    private final S3CleanupOrchestrator s3CleanupOrchestrator;

    @Autowired
    public S3CleanupOrchestratorPerformanceTest(
            @Qualifier("s3DeletionExecutor") Executor s3DeletionExecutor,
            @Qualifier("sqliteDataSource") DataSource dataSource,
            S3Client s3Client,
            S3BucketProperties s3BucketProperties
    ) {
        this.s3Client = s3Client;
        this.s3BucketProperties = s3BucketProperties;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.imageRecyclebinRepository = new ImageRecycleBinRepository(dataSource);
        this.s3ImageCleanupProcessor = new S3ImageCleanupProcessor(
                s3DeletionExecutor,
                this.imageRecyclebinRepository,
                new S3StorageCleaner(this.s3Client, s3BucketProperties.bucket())
        );
        this.notificationProvider = mock(S3FailNotificationProvider.class);
        this.s3CleanupOrchestrator = new S3CleanupOrchestrator(this.s3ImageCleanupProcessor, notificationProvider);
    }

    @BeforeEach
    void setUp() {
        AwsTestContainerInitializer.flushAll();
        jdbcTemplate.execute(DROP_RECYCLE_BIN_TABLE_SQL);
        jdbcTemplate.execute(CREATE_RECYCLE_BIN_TABLE_SQL);
        jdbcTemplate.execute(INSERT_MOCK_DATA_SQL);
        AwsTestContainerInitializer.execute(S3_MOCK_SETUP_COMMAND);
    }

    @Test
    @DisplayName("""
            [Async + Before Mem Optimize] Speed: 1549ms, Max Mem Peak: 121MB
            [Async + After Mem Optimize] Speed: 7416ms, Max Mem Peak: 102
            """)
    void execute100000() {
        // given
        assertThat(isRecycleBinEmpty()).isFalse();
        assertThat(isBucketEmpty("member/")).isFalse();
        MemoryMonitor memoryMonitor = new MemoryMonitor();
        memoryMonitor.reset();
        memoryMonitor.start();

        // when
        stopWatch(TimeUnit.MILLISECONDS, () -> s3CleanupOrchestrator.execute(LocalDateTime.now()));

        // then
        memoryMonitor.stop();
        memoryMonitor.log();
        assertSoftly(softly -> {
            softly.assertThat(isRecycleBinEmpty()).isTrue();
            softly.assertThat(isBucketEmpty("member/")).isTrue();
        });
    }

    private void insertBatchToRecycleBin(List<DeletedImage> deletedImages) {
        imageRecyclebinRepository.insertBatch(deletedImages);
    }

    private void uploadFileParallel(List<String> paths) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<CompletableFuture<PutObjectResponse>> setupFutures = paths.stream()
                .map(path -> CompletableFuture.supplyAsync(() ->
                        s3Client.putObject(PutObjectRequest.builder()
                                        .bucket(s3BucketProperties.bucket())
                                        .key(path)
                                        .build(),
                                RequestBody.fromString("test-content")), executorService))
                .toList();

        CompletableFuture.allOf(setupFutures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();
    }

    private boolean isRecycleBinEmpty() {
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM image_recycle_bin", Integer.class);
        return count == 0;
    }

    private boolean isBucketEmpty(String prefix) {
        ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(s3BucketProperties.bucket())
                .prefix(prefix)
                .maxKeys(1)
                .build());

        return !response.hasContents();
    }

    private void stopWatch(TimeUnit timeUnit, Runnable supplier) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            supplier.run();
        } finally {
            stopWatch.stop();
            double totalTime = stopWatch.getTotalTime(timeUnit);
            System.out.printf("Total Time: %d%s%n", (long) totalTime, getAbbreviation(timeUnit));
        }
    }

    private <T> T stopWatch(TimeUnit timeUnit, Supplier<T> supplier) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            return supplier.get();
        } finally {
            stopWatch.stop();
            double totalTime = stopWatch.getTotalTime(timeUnit);
            System.out.printf("Total Time: %d%s%n", (long) totalTime, getAbbreviation(timeUnit));
        }
    }

    private String getAbbreviation(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> "ns";
            case MICROSECONDS -> "µs";
            case MILLISECONDS -> "ms";
            case SECONDS -> "s";
            case MINUTES -> "m";
            default -> unit.name().toLowerCase();
        };
    }

    private static class MemoryMonitor {

        private volatile long peakHeapMb = 0;
        private volatile boolean monitoring = false;

        void reset() {
            try {
                System.gc();
                Thread.sleep(300);
            } catch (InterruptedException e) {
            }
        }

        void start() {
            peakHeapMb = 0;
            monitoring = true;
            Thread monitor = new Thread(() -> {
                while (monitoring) {
                    long used = ManagementFactory.getMemoryMXBean()
                            .getHeapMemoryUsage().getUsed() / 1024 / 1024;
                    if (used > peakHeapMb) {
                        peakHeapMb = used;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            monitor.setDaemon(true);
            monitor.start();
        }

        void stop() {
            monitoring = false;
        }

        void log() {
            System.out.println("------------------------------------------");
            System.out.printf("[PEAK HEAP USAGE] : %d MB%n", peakHeapMb);
            System.out.println("------------------------------------------");
        }
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
