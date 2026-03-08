package revi1337.onsquad.common.container;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

public class AwsTestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final int LOCALSTACK_GATEWAY_PORT = 4566;
    private static final String DEFAULT_ACCESS_KEY = "access-key";
    private static final String DEFAULT_SECRET_KEY = "secret-key";
    private static final String DEFAULT_REGION = "ap-northeast-2";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsTestContainerInitializer.class);
    private static final LocalStackContainer AWS = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4.0"))
            .withExposedPorts(LOCALSTACK_GATEWAY_PORT)
            .withEnv("AWS_ACCESS_KEY_ID", DEFAULT_ACCESS_KEY)
            .withEnv("AWS_SECRET_ACCESS_KEY", DEFAULT_SECRET_KEY)
            .withEnv("AWS_DEFAULT_REGION", DEFAULT_REGION)
            .withEnv("DEBUG", "1")
            .withEnv("SSL_DISABLE", "1")
            .withServices(LocalStackContainer.Service.S3)
            .withReuse(true)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("testcontainers.aws")));

    private static final String[] BUCKET_CREATE_COMMAND =
            {"awslocal", "s3api", "create-bucket", "--bucket", "onsquad-bucket", "--create-bucket-configuration", "LocationConstraint=" + DEFAULT_REGION};
    private static final String[] PUT_ROOT_DIR_COMMAND = {"awslocal", "s3api", "put-object", "--bucket", "onsquad-bucket", "--key", "onsquad/"};
    private static final String[] PUT_MEMBER_DIR_COMMAND = {"awslocal", "s3api", "put-object", "--bucket", "onsquad-bucket", "--key", "onsquad/member/"};
    private static final String[] PUT_CREW_DIR_COMMAND = {"awslocal", "s3api", "put-object", "--bucket", "onsquad-bucket", "--key", "onsquad/crew/"};
    private static final String[] PUT_SQUAD_DIR_COMMAND = {"awslocal", "s3api", "put-object", "--bucket", "onsquad-bucket", "--key", "onsquad/squad/"};
    private static final String[] FLUSH_ALL_COMMAND = {"awslocal", "s3", "rm", "s3://onsquad-bucket", "--recursive"};

    static {
        LOGGER.info("Initializing AWS Test Container...");
        AWS.start();
        createTestBucket(AWS);
        LOGGER.info("AWS LocalStack started");
    }

    public static void flushAll() {
        try {
            AWS.execInContainer(FLUSH_ALL_COMMAND);
        } catch (Exception e) {
            LOGGER.error("Failed to flush all", e);
        }
    }

    public static void execute(String[] command) {
        try {
            AWS.execInContainer(command);
        } catch (Exception e) {
            LOGGER.error("Failed to execute command", e);
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String s3Endpoint = String.format("http://%s:%d", AWS.getHost(), AWS.getMappedPort(4566));

        Map<String, String> properties = new HashMap<>();
        properties.put("onsquad.aws.s3.endpoint", s3Endpoint);

        TestPropertyValues.of(properties).applyTo(applicationContext);
    }

    private static void createTestBucket(LocalStackContainer aws) {
        try {
            aws.execInContainer(BUCKET_CREATE_COMMAND);
            aws.execInContainer(PUT_ROOT_DIR_COMMAND);
            aws.execInContainer(PUT_MEMBER_DIR_COMMAND);
            aws.execInContainer(PUT_CREW_DIR_COMMAND);
            aws.execInContainer(PUT_SQUAD_DIR_COMMAND);
            LOGGER.info("Test bucket created");
        } catch (Exception e) {
            LOGGER.error("Failed to create bucket", e);
        }
    }
}
