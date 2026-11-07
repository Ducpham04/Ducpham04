package vn.bookeasy.notification;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import vn.bookeasy.config.AppProperties;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class EmailChannel implements NotificationChannel {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties props;

    @Override
    public String name() {
        return "EMAIL";
    }

    @Override
    public String recipient(NotificationMessage m) {
        return props.mail().enabled() && m.email() != null && !m.email().isBlank() ? m.email() : null;
    }

    @Override
    public void send(NotificationMessage m) throws Exception {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) throw new IllegalStateException("Chưa cấu hình SMTP");
        MimeMessage msg = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, false, StandardCharsets.UTF_8.name());
        helper.setFrom(props.mail().from());
        helper.setTo(m.email());
        helper.setSubject(m.subject());
        helper.setText(m.html(), true);
        sender.send(msg);
    }
}
