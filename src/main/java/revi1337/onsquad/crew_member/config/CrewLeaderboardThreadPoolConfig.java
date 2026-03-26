package revi1337.onsquad.crew_member.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CrewLeaderboardThreadPoolConfig {

    @Bean(name = "activityExecutor")
    public Executor activityExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("activity-");
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(2);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(15);
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean(name = "leaderboardProfileExecutor")
    public Executor leaderboardProfileExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("profile-");
        taskExecutor.setCorePoolSize(0);
        taskExecutor.setMaxPoolSize(6);
        taskExecutor.setQueueCapacity(300);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(15);
        taskExecutor.initialize();
        return taskExecutor;
    }
}
