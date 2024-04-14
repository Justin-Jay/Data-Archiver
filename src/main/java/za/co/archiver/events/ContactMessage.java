package za.co.archiver.events;

public class ContactMessage {
    private String name;
    private String fromEmail;
    private String messageBody;
    private String toEmail;
    String subject;

    String completionTime;

    String blobID;


    public ContactMessage() {
    }

    public ContactMessage(String name, String fromEmail, String messageBody) {
        this.name = name;
        this.fromEmail = fromEmail;
        this.messageBody = messageBody;
    }

    public String getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(String completionTime) {
        this.completionTime = completionTime;
    }

    public String getBlobID() {
        return blobID;
    }

    public void setBlobID(String blobID) {
        this.blobID = blobID;
    }

    public String getToEmail() {
        return toEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    @Override
    public String toString() {
        return "ContactMessage{" +
                "name='" + name + '\'' +
                ", email='" + fromEmail + '\'' +
                ", messageBody='" + messageBody + '\'' +
                '}';
    }
}
