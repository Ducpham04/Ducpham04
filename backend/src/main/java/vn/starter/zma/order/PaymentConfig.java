package vn.starter.zma.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.starter.zma.common.config.AppProperties;

@Configuration
public class PaymentConfig {
    @Bean
    public PaymentProvider paymentProvider(AppProperties props, ZaloCheckoutMacService mac, ObjectMapper om) {
        ZaloCheckoutPaymentProvider zalo = new ZaloCheckoutPaymentProvider(mac, om, props.payment().method());
        return props.payment().isMock() ? new MockPaymentProvider(zalo) : zalo;
    }
}
