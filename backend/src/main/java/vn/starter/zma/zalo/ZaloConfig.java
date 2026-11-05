package vn.starter.zma.zalo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import vn.starter.zma.common.config.AppProperties;

@Slf4j
@Configuration
public class ZaloConfig {
    @Bean
    public ZaloGraphClient zaloGraphClient(AppProperties props, RestClient.Builder builder) {
        if (props.zalo().mock()) {
            log.warn("""
                    ================================================================
                     ZALO MOCK MODE ĐANG BẬT (app.zalo.mock=true / ZALO_MOCK=true)
                     Mọi token dạng 'mock-<id>' đều đăng nhập được. KHÔNG dùng ở production!
                    ================================================================""");
            return new MockZaloGraphClient();
        }
        return new HttpZaloGraphClient(props.zalo(), builder);
    }
}
