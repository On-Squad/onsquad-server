package revi1337.onsquad.common.container;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

public class MySqlTestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySqlTestContainerInitializer.class);
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withCommand("--lower_case_table_names=1", "--innodb_redo_log_capacity=1G")
            .withReuse(true)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("testcontainers.mysql")));

    static {
        LOGGER.info("Initializing MySQL Test Container...");
        MYSQL.start();
        LOGGER.info("MySQL started");
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Map<String, String> properties = new HashMap<>();
        properties.put("spring.datasource.url", MYSQL.getJdbcUrl() + "?rewriteBatchedStatements=true");
        properties.put("spring.datasource.username", MYSQL.getUsername());
        properties.put("spring.datasource.password", MYSQL.getPassword());
        properties.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
        TestPropertyValues.of(properties).applyTo(applicationContext);
    }
}
