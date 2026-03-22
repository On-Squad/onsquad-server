package revi1337.onsquad.crew.infrastructure;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import revi1337.onsquad.common.config.etc.YamlPropertySourceFactory;
import revi1337.onsquad.common.config.infrastructure.AwsConfiguration;
import revi1337.onsquad.common.container.AwsTestContainerInitializer;
import revi1337.onsquad.infrastructure.aws.cloudfront.CloudFrontCacheInvalidator;
import revi1337.onsquad.infrastructure.aws.cloudfront.CloudFrontProperties;
import revi1337.onsquad.infrastructure.aws.s3.client.CacheableS3StorageManager;
import revi1337.onsquad.infrastructure.aws.s3.client.S3StorageManager;
import revi1337.onsquad.infrastructure.aws.s3.config.S3BucketProperties;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@TestPropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties({S3BucketProperties.class, CloudFrontProperties.class})
@ContextConfiguration(
        initializers = AwsTestContainerInitializer.class,
        classes = {AwsConfiguration.S3Configuration.class, AwsConfiguration.CloudFrontConfiguration.class}
)
@ExtendWith(SpringExtension.class)
class CrewFileStorageManagerTest {

    private final S3Client s3Client;
    private final S3BucketProperties s3BucketProperties;
    private final CloudFrontProperties cloudFrontProperties;
    private final CloudFrontCacheInvalidator cloudFrontCacheInvalidator;
    private final CrewFileStorageManager crewFileStorageManager;
    private final String crewDir;

    @Autowired
    public CrewFileStorageManagerTest(S3Client s3Client, S3BucketProperties s3BucketProperties, CloudFrontProperties cloudFrontProperties) {
        this.s3Client = s3Client;
        this.s3BucketProperties = s3BucketProperties;
        this.cloudFrontProperties = cloudFrontProperties;
        this.cloudFrontCacheInvalidator = mock(CloudFrontCacheInvalidator.class);
        this.crewFileStorageManager = new CrewFileStorageManager(
                new CacheableS3StorageManager(
                        new S3StorageManager(s3Client, s3BucketProperties.bucket()),
                        this.cloudFrontCacheInvalidator,
                        cloudFrontProperties.baseDomain()
                ),
                s3BucketProperties
        );
        this.crewDir = s3BucketProperties.getActualCrewAssets();
    }

    @BeforeEach
    void setUp() {
        AwsTestContainerInitializer.flushAll();
    }

    @Test
    @DisplayName("byte 배열 업로드 시, 크루 전용 경로에 파일을 저장하고 CloudFront 도메인이 결합된 UUID 기반 URL을 반환한다.")
    void uploadBytes() {
        byte[] fileBytes = "text".getBytes();
        String filePathAndName = "text-bytes-file.txt";

        String uploadedUrl = crewFileStorageManager.upload(fileBytes, filePathAndName);

        assertSoftly(softly -> {
            softly.assertThat(uploadedUrl).isNotBlank();
            softly.assertThat(uploadedUrl).startsWith(cloudFrontProperties.baseDomain());

            Path path = Path.of(URI.create(uploadedUrl).getPath());
            String fileName = path.getFileName().toString();
            softly.assertThat(path.getParent().toString()).endsWith(crewDir);
            softly.assertThatCode(() -> UUID.fromString(fileName.substring(0, fileName.lastIndexOf("."))))
                    .doesNotThrowAnyException();
            softly.assertThat(fileName).endsWith(".txt");
        });
    }

    @Test
    @DisplayName("MultipartFile 업로드 시, 크루 전용 디렉터리 내에 UUID 파일명으로 저장된 CloudFront 접근 주소를 반환한다.")
    void uploadMultipart1() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-file.txt",
                "text/plain",
                "text".getBytes()
        );

        String uploadedUrl = crewFileStorageManager.upload(mockFile);

        assertSoftly(softly -> {
            softly.assertThat(uploadedUrl).isNotBlank();
            softly.assertThat(uploadedUrl).startsWith(cloudFrontProperties.baseDomain());

            Path path = Path.of(URI.create(uploadedUrl).getPath());
            String fileName = path.getFileName().toString();
            softly.assertThat(path.getParent().toString()).endsWith(crewDir);
            softly.assertThatCode(() -> UUID.fromString(fileName.substring(0, fileName.lastIndexOf("."))))
                    .doesNotThrowAnyException();
            softly.assertThat(fileName).endsWith(".txt");
        });
    }

    @Test
    @DisplayName("사용자 지정 파일명을 포함하여 업로드할 경우, 크루 경로 하위에 해당 이름의 UUID 파일을 생성한다.")
    void uploadMultipart2() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "mock-name.txt",
                "text/plain",
                "text".getBytes()
        );

        String uploadedUrl = crewFileStorageManager.upload(mockFile, "custom-name.txt");

        assertSoftly(softly -> {
            softly.assertThat(uploadedUrl).isNotBlank();
            softly.assertThat(uploadedUrl).startsWith(cloudFrontProperties.baseDomain());

            Path path = Path.of(URI.create(uploadedUrl).getPath());
            String fileName = path.getFileName().toString();
            softly.assertThat(path.getParent().toString()).endsWith(crewDir);
            softly.assertThatCode(() -> UUID.fromString(fileName.substring(0, fileName.lastIndexOf("."))))
                    .doesNotThrowAnyException();
            softly.assertThat(fileName).endsWith(".txt");
        });
    }

    @Test
    @DisplayName("크루 파일 삭제 시, S3 실제 객체를 제거하고 연동된 CloudFront 캐시에 무효화 요청을 전송한다.")
    void delete() {
        String uploadUrl = crewFileStorageManager.upload(new MockMultipartFile(
                "file",
                "test-file.txt",
                "text/plain",
                "text".getBytes()
        ));
        Path path = Paths.get(URI.create(uploadUrl).getPath());
        String pureS3Key = path.subpath(1, path.getNameCount()).toString();
        doNothing().when(cloudFrontCacheInvalidator).createInvalidation(any());

        crewFileStorageManager.delete(pureS3Key);

        assertSoftly(softly -> {
            softly.assertThat(objectExists(pureS3Key)).isFalse();
            verify(cloudFrontCacheInvalidator, times(1)).createInvalidation(any());
        });
    }

    private boolean objectExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(s3BucketProperties.bucket())
                    .key(key)
                    .build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
