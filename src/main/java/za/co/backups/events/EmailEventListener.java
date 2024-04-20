package za.co.backups.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class EmailEventListener {

    Logger log = LoggerFactory.getLogger(EmailEventListener.class);
    private final JavaMailSenderImpl javaMailSender;


    @Value("${spring.mail.username}")
    String rzoneFromAddress;
    @Value("${admin.mail.to.address}")
    String rzoneToAddress;

    @Value("${date.format}")
    String DATE_FORMAT;


    @Value("${bkadmin.name}")
    String admin;


    public EmailEventListener(JavaMailSenderImpl javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @EventListener
    public void onWebsiteQueryReceived(BackUpEvent event) {
        log.info("Executing onWebsiteQueryReceived");

        sendNotification(event);

        log.info("DONE Executing onWebsiteQueryReceived");
    }


    public void sendNotification(BackUpEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        ContactMessage eventMessage = event.getMessage();
        message.setFrom(eventMessage.getFromEmail());
        message.setTo(eventMessage.getToEmail());
        message.setSubject("Back Up Successful");
        String emailContent = "Hello " + eventMessage.getName() + ",\n\n"
                + "Back Up Completed At: " + eventMessage.getCompletionTime() + "\n"
                + "Back Up Response: " + eventMessage.getResponse() + "\n\n"
                + "Best regards,\n"
                + "Back Up Squad";
        message.setText(emailContent);
        try {
            javaMailSender.send(message);
            log.info("Email Notification Sent");
        } catch (Exception e) {
            log.info("Email Notification Failed");
            log.info(e.getMessage());
        }
    }


}
