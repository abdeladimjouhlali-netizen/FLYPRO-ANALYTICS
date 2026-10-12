package com.flypro.analytics.notification.channel;

import com.flypro.analytics.notification.enums.NotificationChannelType;
import com.flypro.analytics.notification.exception.NotificationException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Email delivery channel via JavaMailSender.
 */
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String from;

    public EmailNotificationChannel(JavaMailSender mailSender,
                                    @Value("${flypro.mail.enabled:false}") boolean mailEnabled,
                                    @Value("${flypro.mail.from:noreply@flypro.local}") String from) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.from = from;
    }

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public boolean supports(NotificationChannelType channelType) {
        return channelType == NotificationChannelType.EMAIL;
    }

    @Override
    public void send(String recipient, String subject, String htmlOrTextBody) {
        if (!mailEnabled) {
            log.info("Mail disabled – skipping SMTP send to={} subject={}", recipient, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlOrTextBody, true);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new NotificationException("Failed to send email to " + recipient + ": " + ex.getMessage());
        }
    }
}
