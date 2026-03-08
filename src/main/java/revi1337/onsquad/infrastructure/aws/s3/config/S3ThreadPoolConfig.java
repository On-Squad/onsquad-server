package revi1337.onsquad.infrastructure.aws.s3.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import revi1337.onsquad.infrastructure.aws.s3.event.FileDeleteEventListener;

@Configuration
public class S3ThreadPoolConfig {

    /**
     * Worker Thread Used For {@link FileDeleteEventListener}
     */
    @Bean("fileDeletionRecorder")
    public Executor fileDeletionRecorder() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("recorder-fdel-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Worker Thread Used For {@link revi1337.onsquad.infrastructure.aws.s3.cleanup.S3ImageCleanupProcessor}
     */
    @Bean(name = "s3DeletionExecutor")
    public Executor s3DeletionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(0);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("s3-cleaner-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
