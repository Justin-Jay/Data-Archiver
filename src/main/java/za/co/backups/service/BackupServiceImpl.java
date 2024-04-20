package za.co.backups.service;


import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;
import za.co.backups.events.ContactMessage;
import za.co.backups.events.EmailEventPublisher;


import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.io.IOException;

@Component
public class BackupServiceImpl implements BackUpService {
    private final Storage storage;
    private final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);

    @Value("${storage.bucket}")
    private String STORAGE_BUCKET;


    @Value("${db.file.location}")
    String BACKUP_FILE_DIR;

    @Value("${db.file.name}")
    String BACKUP_FILE_NAME;

    @Value("${date.format}")
    String DATE_FORMAT;

    @Value("${spring.mail.username}")
    String rzoneFromAddress;
    @Value("${admin.mail.to.address}")
    String rzoneToAddress;
    private final String CRON_TIMER = "0 0 0/1 1/1 * ? *";

    String FILE_PATH_SPLIT = "/";

    @Value("${base.bucket.folder}")
    String BASE_BUCKET_BACKUP_FOLDER;

    @Value("${bkadmin.name}")
    String admin_name;

    @Value("${backup.file.filter}")
    String filter;

    private final EmailEventPublisher eventPublisher;

    public BackupServiceImpl(Storage storage, EmailEventPublisher eventPublisher) {
        this.storage = storage;
        this.eventPublisher = eventPublisher;
    }


    // @Scheduled(cron = CRON_TIMER) // This cron expression triggers the method every midnight
    @Override
    public void startBackUp() {
        log.info("Back Up Started....");

        File inputFile = null;

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(BACKUP_FILE_DIR), filter)) {
            for (Path path : directoryStream) {
                inputFile = new File(path.toUri());
                System.out.println("Found matching file: " + path.getFileName());

                String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
                LocalTime time = LocalTime.now();

                // Define the pattern for formatting
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH_mm_ss");

                // Format the time using the formatter
                String formattedTime = time.format(formatter);

                String destinationFileName = BASE_BUCKET_BACKUP_FOLDER + FILE_PATH_SPLIT + formattedDate + FILE_PATH_SPLIT + formattedTime + FILE_PATH_SPLIT + BACKUP_FILE_NAME;

                BlobId id = BlobId.of(STORAGE_BUCKET, destinationFileName);
                BlobInfo info = BlobInfo.newBuilder(id).build();


                log.info("BACKUP_FILE_DIR: {}", BACKUP_FILE_DIR);
                log.info("BACKUP_FILE_NAME: {}", BACKUP_FILE_NAME);
                log.info("inputFile: {}", inputFile.getAbsolutePath());
                log.info("STORAGE_BUCKET: {}", STORAGE_BUCKET);
                log.info("destinationFileName: {}", destinationFileName);

                byte[] arr = readBytesFromFile(Paths.get(inputFile.toURI()));
                Blob backUp = storage.create(info, arr);

                log.info("Back up file {} Completed....", backUp.getBlobId());

                triggerNotication(formattedDate, backUp.getBlobId().toString());

                boolean d = false;
                try {
                    //Files.delete(Paths.get(inputFile.toURI()));
                    d = inputFile.delete();
                    log.info("Input file deleted: {}", d);
                } catch (Exception e){
                    log.info("Failed to delete Input File \n {}", e.getMessage());
                }

                }
        } catch (IOException e) {
            log.info("File not loaded {}",e.getMessage());
            e.printStackTrace();
        }

    }

    @Override
    public void triggerNotication(String formattedDate, String blobID) {
        log.info("triggerNotication");
        ContactMessage message = new ContactMessage();
        message.setName(admin_name);
        log.info("Notification From: {}", admin_name);
        message.setFromEmail(rzoneFromAddress);
        log.info("Using Mail Box: {}", rzoneFromAddress);
        message.setSubject("Back Up Notification");
        message.setToEmail(rzoneToAddress);
        log.info("Sending To Mail Box: {}", rzoneToAddress);
        message.setCompletionTime(formattedDate);
        message.setBlobID(blobID);
        eventPublisher.publishBackUpEvent(message);
    }

/*    @Override
    public String downloadBackUp(String fileDate,String bucket) {
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

            try (FileWriter writer = new FileWriter(filePath, false)) {

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

    }*/

    @Override
    public byte[] readBytesFromFile(Path filePath) {

        byte[] returnArray = null;

        try {
            returnArray = Files.readAllBytes(filePath);
        } catch (IOException ioException) {
            log.info("Could not read to file: {}", ioException.getMessage());
        }

        return returnArray;

    }

    @Override
    public void writeBytesToFile(byte[] data, String filePath) {

        try {
            Path path = Paths.get(filePath);
            Files.write(path, data);
        } catch (IOException ioException) {
            log.info("Could not write to file: {}", ioException.getMessage());
        }

    }

}
