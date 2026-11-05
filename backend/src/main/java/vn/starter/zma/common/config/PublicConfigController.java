package vn.starter.zma.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicConfigController {
    private final AppProperties props;

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of(
                "zaloMock", props.zalo().mock(),
                "paymentProvider", props.payment().provider(),
                "notificationProvider", props.notification().provider());
    }
}
