package za.co.backups.service;

import java.nio.file.Path;

public interface BackUpService {
    abstract void startBackUp();

    void triggerNotication(String formattedDate, String blobID);


   // String downloadBackUp(String fileDate,String bucket);

    byte[] readBytesFromFile(Path filePath);

    void writeBytesToFile(byte[] data, String filePath);

}
