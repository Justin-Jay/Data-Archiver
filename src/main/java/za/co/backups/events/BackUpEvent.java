package za.co.backups.events;


public class BackUpEvent {

    private ContactMessage message;

    public BackUpEvent(ContactMessage message) {
        this.message = message;
    }

    public ContactMessage getMessage() {
        return message;
    }

    public void setMessage(ContactMessage message) {
        this.message = message;
    }
}
