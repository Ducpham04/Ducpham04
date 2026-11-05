package vn.starter.zma.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import vn.starter.zma.common.config.AppProperties;

@Configuration
public class NotificationConfig {
    @Bean
    public NotificationGateway notificationGateway(AppProperties props, RestClient.Builder builder) {
        return switch (props.notification().provider().toLowerCase()) {
            case "zns" -> new ZnsNotificationGateway(props.notification(), builder);
            default -> new MockNotificationGateway();
        };
    }
}
