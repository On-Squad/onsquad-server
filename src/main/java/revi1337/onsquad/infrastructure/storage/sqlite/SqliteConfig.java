package revi1337.onsquad.infrastructure.storage.sqlite;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@Configuration
public class SqliteConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.sqlite-datasource")
    public DataSourceProperties sqliteDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource sqliteDataSource() {
        return sqliteDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public PlatformTransactionManager sqliteTransactionManager(@Qualifier("sqliteDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
