package za.co.archiver.service;


import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import za.co.archiver.events.Notification;
import za.co.archiver.events.ContactMessage;
import za.co.archiver.events.EmailEventPublisher;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class BackupServiceImpl {
    private final Storage storage;
    private final JavaMailSenderImpl javaMailSender;
    private final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);

    @Value("${storage.bucket}")
    String STORAGE_BUCKET;

    @Value("${db.prefix}")
    String DB_PREFIX;


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

    @Value("${destination.file.name}")
    String DESTINATIONFILENAME;

    @Value("${download.folder}")
    String DOWNLOADFOLDER;


    private final String CRON_TIMER = "0 0 0/1 1/1 * ? *";
    String FILE_PATH_SPLIT = "/";


    @Value("${bkadmin.name}")
    String admin_name;

    static final String projectID = "recruitmentzone";

    private final EmailEventPublisher eventPublisher;

    public BackupServiceImpl(Storage storage, JavaMailSenderImpl javaMailSender, EmailEventPublisher eventPublisher) {
        this.storage = storage;
        this.javaMailSender = javaMailSender;
        this.eventPublisher = eventPublisher;
    }


    // @Scheduled(cron = CRON_TIMER) // This cron expression triggers the method every midnight
    public void initBackUpJob() throws Exception {
        log.info("Back Up Started....");
        String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        LocalTime time = LocalTime.now();

        // Define the pattern for formatting
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH_mm_ss");

        // Format the time using the formatter
        String formattedTime = time.format(formatter);

        String bucketFileStructure = DESTINATIONFILENAME + FILE_PATH_SPLIT + formattedDate + FILE_PATH_SPLIT + formattedTime + FILE_PATH_SPLIT + DB_FILE_NAME;
        BlobId id = BlobId.of(STORAGE_BUCKET, bucketFileStructure);
        BlobInfo info = BlobInfo.newBuilder(id).build();
        File inputFile = new File(BASE_DIR, DB_FILE_NAME);
        log.info("BASE_DIR: {}", BASE_DIR);
        log.info("DB_FILE_NAME: {}", DB_FILE_NAME);
        log.info("inputFile: {}", inputFile.getAbsolutePath());
        log.info("STORAGE_BUCKET: {}", STORAGE_BUCKET);
        log.info("bucketFileStructure: {}", bucketFileStructure);
        byte[] arr = readBytesFromFile(Paths.get(inputFile.toURI()));
        Blob backUp = storage.create(info, arr);

        log.info("Back up file {} Completed....", backUp.getBlobId());
        boolean d = inputFile.delete();
        if (d) {
            //triggerNotication(formattedDate, backUp.getBlobId().toString());
            sendNotification(formattedDate, backUp.getBlobId().toString());
        }
        log.info("File Deleted {}....", d);
    }

    public byte[] readBytesFromFile(Path filePath) throws IOException {

        return Files.readAllBytes(filePath);
    }


    public void sendNotification(String formattedDate, String blobID) {
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


        Notification bk = new Notification(message);
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        ContactMessage notificationMessage = bk.getMessage();
        simpleMailMessage.setFrom(notificationMessage.getFromEmail());
        simpleMailMessage.setTo(notificationMessage.getToEmail());
        simpleMailMessage.setSubject("DB Back Up Successful");
        String emailContent = "Hello " + notificationMessage.getName() + ",\n\n"
                + "Back Up Completed At: " + notificationMessage.getCompletionTime() + "\n"
                + "Back Up Blob ID: " + notificationMessage.getBlobID() + "\n\n"
                + "Best regards,\n"
                + "Back Up Squad";
        simpleMailMessage.setText(emailContent);
        try {
            javaMailSender.send(simpleMailMessage);
            log.info("Email Notification Sent");
        } catch (Exception e) {
            log.info("Email Notification Failed");
            log.info(e.getMessage());
        }
    }

    public String downloadBackUp(String fileDate) {
        StringBuffer sb = new StringBuffer();
        String fileOne = "DB-BackUps/2024_03_15/04_00_07/all-databases.sql";
        //String storageFileStructure = DESTINATIONFILENAME + FILE_PATH_SPLIT + fileDate + FILE_PATH_SPLIT + DB_FILE_NAME;
        try (ReadChannel channel = storage.reader(STORAGE_BUCKET, fileOne)) {
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

            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(sb.toString());
                writer.close();
                log.info("Contents written to file successfully.");

            } catch (IOException e) {
                log.info("Failed to write contents to file.");
                log.info(e.getMessage());
            }
            return "Download from GCP complete";
        } catch (IOException e) {
            log.info(e.getMessage());
            throw new RuntimeException(e);
        }

    }


    private void writeBytesToFile(byte[] data, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Files.write(path, data);
    }

    public void listObjects() {
        log.info(" <-- listObjects: STORAGE_BUCKET = {}--> ", STORAGE_BUCKET);
        Page<Blob> blobs = storage.list(STORAGE_BUCKET);
        int count = 1;
        for (Blob blob : blobs.iterateAll()) {
            log.info("Blob name: {}", blob.getName());
            log.info(" <-- blob {} \n {} --> ", count, blob);
            count++;
        }
    }

    public void listObjectsWithPrefix() {

        // The directory prefix to search for
        // String directoryPrefix = "myDirectory/"

        // Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        /**
         * Using the Storage.BlobListOption.currentDirectory() option here causes the results to display
         * in a "directory-like" mode, showing what objects are in the directory you've specified, as
         * well as what other directories exist in that directory. For example, given these blobs:
         *
         * <p>a/1.txt a/b/2.txt a/b/3.txt
         *
         * <p>If you specify prefix = "a/" and don't use Storage.BlobListOption.currentDirectory(),
         * you'll get back:
         *
         * <p>a/1.txt a/b/2.txt a/b/3.txt
         *
         * <p>However, if you specify prefix = "a/" and do use
         * Storage.BlobListOption.currentDirectory(), you'll get back:
         *
         * <p>a/1.txt a/b/
         *
         * <p>Because a/1.txt is the only file in the a/ directory and a/b/ is a directory inside the
         * /a/ directory.
         */
        log.info(" <-- listObjectsWithPrefix: STORAGE_BUCKET = {}--> ", STORAGE_BUCKET);
        log.info(" <-- listObjectsWithPrefix: prefix = {} --> ", DB_PREFIX);
        Page<Blob> blobs =
                storage.list(
                        STORAGE_BUCKET,
                        Storage.BlobListOption.prefix(DB_PREFIX),
                        Storage.BlobListOption.currentDirectory());
        int count = 1;
        for (Blob blob : blobs.iterateAll()) {
            log.info("Blob name: {}", blob.getName());
            log.info(" <-- blob {} \n {} --> ", count, blob);
            count++;
        }
    }

    public void downloadObject(String dbFileDate) {
        String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        // The ID of your GCS object
        // name=DB-BackUps/2024_03_15/04_00_07/all-databases.sql


        String fileOne = "DB-BackUps/2024_03_15/04_00_07/all-databases.sql";
        String fileThee = "Database/2024_03_13/all-databases.sql";
        String fileSix = "DB-BackUps/2024_03_13/14_48_22/all-databases.sql";

        String fileFour = "DB-BackUps/2024_03_15/all-databases.sql";

        // The path to which the file should be downloaded
        // String destFilePath = "/local/path/to/file.txt";

        String destFilePath = DOWNLOADFOLDER + FILE_PATH_SPLIT + formattedDate + FILE_PATH_SPLIT + DB_FILE_NAME;
        //Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

        Blob blob = storage.get(BlobId.of(STORAGE_BUCKET, fileOne));
        log.info("Blob to be downloaded: {}", blob.getName());
        log.info("Download path: {}", destFilePath);
        blob.downloadTo(Paths.get(destFilePath));

        log.info("Downloaded object {} from bucket name {} to {} ", fileOne, STORAGE_BUCKET, destFilePath);
    }//

    public static void setObjectMetadata(String projectId, String bucketName, String objectName) {
        // The ID of your GCP project
        // String projectId = "your-project-id";

        // The ID of your GCS bucket
        // String bucketName = "your-unique-bucket-name";

        // The ID of your GCS object
        // String objectName = "your-object-name";

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        Map<String, String> newMetadata = new HashMap<>();
        // size / MB_SIZE = size in MB

        newMetadata.put("keyToAddOrUpdate", "value");
        BlobId blobId = BlobId.of(bucketName, objectName);
        Blob blob = storage.get(blobId);
        if (blob == null) {
            System.out.println("The object " + objectName + " was not found in " + bucketName);
            return;
        }

        // Optional: set a generation-match precondition to avoid potential race
        // conditions and data corruptions. The request to upload returns a 412 error if
        // the object's generation number does not match your precondition.
        Storage.BlobTargetOption precondition = Storage.BlobTargetOption.generationMatch();

        // Does an upsert operation, if the key already exists it's replaced by the new value, otherwise
        // it's added.
        blob.toBuilder().setMetadata(newMetadata).build().update(precondition);

    }


}
