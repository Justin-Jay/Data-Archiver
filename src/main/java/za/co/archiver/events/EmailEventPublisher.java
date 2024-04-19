package za.co.archiver.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class EmailEventPublisher {
    private final ApplicationEventPublisher eventPublisher;
    private final Logger log = LoggerFactory.getLogger(EmailEventPublisher.class);

    public EmailEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    public boolean publishBackUpEvent(ContactMessage message) {
        BackUpEvent bk = new BackUpEvent(message);
        log.info("Executing publishBackUpEvent");
        try {
            eventPublisher.publishEvent(bk);
            log.info("EVENT publishBackUpEvent POSTED");
            return true;
        } catch (Exception e) {
            log.info("Unable to post event");
            return false;
        }

    }











}
