package vn.bookeasy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
@EnableAsync
public class TimeConfig {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
