package za.co.recruitmentzone.service;


import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;
import za.co.recruitmentzone.events.ContactMessage;
import za.co.recruitmentzone.events.EmailEventPublisher;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.io.IOException;

@Component
public class BackupService {
    private final Storage storage;
    private final Logger log = LoggerFactory.getLogger(BackupService.class);

    @Value("${storage.bucket}")
    private String STORAGE_BUCKET;

    @Value("${db.file.location}")
    String BASE_DIR;

    @Value("${db.file.name}")
    String DB_FILE_NAME;

    @Value("${date.format}")
    String DATE_FORMAT;

    @Value("${spring.mail.username}")
    String rzoneFromAddress;
    @Value("${admin.mail.to.address}")
    String rzoneToAddress;
    private final String CRON_TIMER = "0 0 0/1 1/1 * ? *";
    String FILE_PATH_SPLIT = "/";
    String DB_BACK_UP = "DB-BackUps";

    @Value("${bkadmin.name}")
    String admin_name;



    private final EmailEventPublisher eventPublisher;

    public BackupService(Storage storage, EmailEventPublisher eventPublisher) {
        this.storage = storage;
        this.eventPublisher = eventPublisher;
    }


    // @Scheduled(cron = CRON_TIMER) // This cron expression triggers the method every midnight
    public void startBackUp() throws Exception {
        log.info("Back Up Started....");
        String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        LocalTime time = LocalTime.now();

        // Define the pattern for formatting
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH_mm_ss");

        // Format the time using the formatter
        String formattedTime = time.format(formatter);

        String destinationFileName = DB_BACK_UP + FILE_PATH_SPLIT + formattedDate + FILE_PATH_SPLIT + formattedTime + FILE_PATH_SPLIT + DB_FILE_NAME;
        BlobId id = BlobId.of(STORAGE_BUCKET, destinationFileName);
        BlobInfo info = BlobInfo.newBuilder(id).build();
        File inputFile = new File(BASE_DIR, DB_FILE_NAME);
        log.info("BASE_DIR: {}", BASE_DIR);
        log.info("DB_FILE_NAME: {}", DB_FILE_NAME);
        log.info("inputFile: {}", inputFile.getAbsolutePath());
        log.info("STORAGE_BUCKET: {}", STORAGE_BUCKET);
        log.info("destinationFileName: {}", destinationFileName);

        byte[] arr = readBytesFromFile(Paths.get(inputFile.toURI()));
        Blob backUp = storage.create(info, arr);

        log.info("Back up file {} Completed....", backUp.getBlobId());
        triggerNotication(formattedDate, backUp.getBlobId().toString());
        boolean d = inputFile.delete();
        log.info("File Deleted {}....", d);
    }

    public void triggerNotication(String formattedDate, String blobID) {
        log.info("triggerNotication");
        ContactMessage message = new ContactMessage();
        message.setName(admin_name);
        log.info("Notification To: {}", admin_name);
        message.setFromEmail(rzoneFromAddress);
        log.info("Using Mail Box: {}", rzoneFromAddress);
        message.setSubject("Back Up Notification");
        message.setToEmail(rzoneToAddress);
        log.info("Sending To Mail Box: {}", rzoneToAddress);
        message.setCompletionTime(formattedDate);
        message.setBlobID(blobID);
        eventPublisher.publishBackUpEvent(message);
    }

    public String downloadBackUp(String fileDate) {
        StringBuffer sb = new StringBuffer();

        String destinationFileName = DB_BACK_UP + FILE_PATH_SPLIT + fileDate + FILE_PATH_SPLIT + DB_FILE_NAME;
        try (ReadChannel channel = storage.reader(STORAGE_BUCKET, destinationFileName)) {
            ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);

            while (channel.read(bytes) > 0) {
                bytes.flip();
                String data = new String(bytes.array(), 0, bytes.limit());
                sb.append(data);
                bytes.clear();
            }

            String fileName = "all-databases.sql";
            String downloadPath = System.getProperty("user.home") + File.separator + "downloads";

            String filePath = downloadPath + File.separator + fileName;

            try (FileWriter writer = new FileWriter(filePath, false)){

                writer.write(sb.toString());
                writer.close();
                System.out.println("Contents written to file successfully.");

            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Download from GCP complete";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public byte[] readBytesFromFile(Path filePath) throws IOException {

        return Files.readAllBytes(filePath);
    }

    private void writeBytesToFile(byte[] data, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Files.write(path, data);
    }

}
