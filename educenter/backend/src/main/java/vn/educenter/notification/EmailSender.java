package vn.educenter.notification;

/** Cổng gửi email (tách interface để test và đổi nhà cung cấp: SMTP, SES, SendGrid...). */
public interface EmailSender {
    void send(String to, String subject, String textBody);
}
