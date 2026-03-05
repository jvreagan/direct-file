package gov.irs.directfile.emailservice.services;

import java.util.Properties;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("smtp")
public class SmtpEmailSendService {
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String fromAddress;
    private final boolean useTls;

    public SmtpEmailSendService(
            @Value("${direct-file.email.smtp.host:localhost}") String smtpHost,
            @Value("${direct-file.email.smtp.port:587}") int smtpPort,
            @Value("${direct-file.email.smtp.username:}") String smtpUsername,
            @Value("${direct-file.email.smtp.password:}") String smtpPassword,
            @Value("${direct-file.email.from:noreply@directfile.irs.gov}") String fromAddress,
            @Value("${direct-file.email.smtp.tls:true}") boolean useTls) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.fromAddress = fromAddress;
        this.useTls = useTls;
    }

    public void sendEmail(String to, String subject, String htmlBody) {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", String.valueOf(!smtpUsername.isBlank()));
        props.put("mail.smtp.starttls.enable", String.valueOf(useTls));

        Session session;
        if (!smtpUsername.isBlank()) {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");
            Transport.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
