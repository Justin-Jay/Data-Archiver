package za.co.archiver.events;


public class Notification {

    private ContactMessage message;

    public Notification(ContactMessage message) {
        this.message = message;
    }

    public ContactMessage getMessage() {
        return message;
    }

    public void setMessage(ContactMessage message) {
        this.message = message;
    }
}
