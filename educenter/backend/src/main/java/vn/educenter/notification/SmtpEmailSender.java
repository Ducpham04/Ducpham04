package vn.educenter.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import vn.educenter.config.AppProperties;

@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final AppProperties props;

    @Override
    public void send(String to, String subject, String textBody) {
        if (!props.mail().enabled()) throw new IllegalStateException("Email đang tắt (MAIL_ENABLED=false)");
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(props.mail().from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, false);
            mailSender.send(msg);
        } catch (MessagingException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
