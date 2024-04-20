package za.co.backups.service;


import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);

    private final Storage storage;

    @Value("${storage.bucket}")
    private String storageBucket;


    @Value("${db.file.location}")
    private String backupFileDir;

    @Value("${db.file.name}")
    private String backupFileName;

    @Value("${date.format}")
    private String dateFormat;

    @Value("${spring.mail.username}")
    private String rzoneFromAddress;

    @Value("${admin.mail.to.address}")
    private String rzoneToAddress;

    private final String cronTimer = "0 0 0/1 1/1 * ? *";

    private String filePathSplit = "/";

    @Value("${base.bucket.folder}")
    private String baseBucketBackupFolder;

    @Value("${bkadmin.name}")
    private String adminName;

    @Value("${backup.file.filter}")
    private String filter;

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
        LocalTime time = LocalTime.now();

        // Define the pattern for formatting
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH_mm_ss");
        String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat));
        String formattedTime = time.format(formatter);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(backupFileDir), filter)) {
            for (Path path : directoryStream) {
                inputFile = new File(path.toUri());
                log.info("Found matching file: {}", path.getFileName());
                // Format the time using the formatter

                String destinationFileName = baseBucketBackupFolder + filePathSplit + formattedDate + filePathSplit + formattedTime + filePathSplit + backupFileName;

                BlobId id = BlobId.of(storageBucket, destinationFileName);
                BlobInfo info = BlobInfo.newBuilder(id).build();

                log.info("BACKUP_FILE_DIR: {}", backupFileDir);
                log.info("BACKUP_FILE_NAME: {}", backupFileName);
                log.info("inputFile: {}", inputFile.getAbsolutePath());
                log.info("STORAGE_BUCKET: {}", storageBucket);
                log.info("destinationFileName: {}", destinationFileName);

                byte[] arr = readBytesFromFile(Paths.get(inputFile.toURI()));
                Blob backUp = storage.create(info, arr);


                log.info("Back up file {} Completed....", backUp.getBlobId());

                try {
                    Files.delete(Paths.get(inputFile.toURI()));
                    log.info("Input file deleted: {}", true);
                    triggerNotication(formattedDate, backUp.getBlobId().toString() + " Input File deleted");


                } catch (Exception e) {
                    log.info("Failed to delete Input File \n {}", e.getMessage());
                    triggerNotication(formattedDate, backUp.getBlobId().toString() + " Failed to delete existing file");
                }

            }
        } catch (IOException e) {
            log.info("File not loaded {}", e.getMessage());
            triggerNotication(formattedDate, "Backup file not loaded");
        }

    }

    @Override
    public void triggerNotication(String formattedDate, String response) {
        log.info("trigger Notification");
        ContactMessage message = new ContactMessage();
        message.setName(adminName);
        log.info("Notification From: {}", adminName);
        message.setFromEmail(rzoneFromAddress);
        log.info("Using Mail Box: {}", rzoneFromAddress);
        message.setSubject("Back Up Notification");
        message.setToEmail(rzoneToAddress);
        log.info("Sending To Mail Box: {}", rzoneToAddress);
        message.setCompletionTime(formattedDate);
        message.setResponse(response);
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

                log.info("Contents written to file successfully.")

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
