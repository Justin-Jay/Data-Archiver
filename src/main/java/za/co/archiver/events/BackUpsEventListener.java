package za.co.archiver.events;

import com.google.cloud.storage.Storage;
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
public class BackUpsEventListener {

    Logger log = LoggerFactory.getLogger(BackUpsEventListener.class);
    private final JavaMailSenderImpl javaMailSender;
    private final Storage storage;

    @Value("${spring.mail.username}")
    String rzoneFromAddress;
    @Value("${admin.mail.to.address}")
    String rzoneToAddress;

 /*   @Value("${date.format}")
    String DATE_FORMAT;
*/

    @Value("${bkadmin.name}")
    String admin;

    public BackUpsEventListener(JavaMailSenderImpl javaMailSender, Storage storage) {
        this.javaMailSender = javaMailSender;
        this.storage = storage;
    }

    @EventListener
    public void onBackUpEventCompleted(Notification event) {
        log.info("Executing onBackUpEventReceived");

        sendNotification(event);

        log.info("DONE Executing onBackUpEventReceived");
    }

    public void sendNotification(Notification event) {
        SimpleMailMessage message = new SimpleMailMessage();
        ContactMessage eventMessage = event.getMessage();
        message.setFrom(eventMessage.getFromEmail());
        message.setTo(eventMessage.getToEmail());
        message.setSubject("DB Back Up Successful");
        String emailContent = "Hello " + eventMessage.getName() + ",\n\n"
                + "Back Up Completed At: " + eventMessage.getCompletionTime() + "\n"
                + "Back Up Blob ID: " + eventMessage.getBlobID() + "\n\n"
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
