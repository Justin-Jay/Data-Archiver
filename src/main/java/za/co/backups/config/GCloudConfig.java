package za.co.backups.config;

import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import com.google.cloud.storage.*;


import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Component
public class GCloudConfig {
    private final static Logger log = LoggerFactory.getLogger(GCloudConfig.class);

    @Value("${KEY_DIR}")
    String gcloudkey;

    @Bean
    public Storage storage() throws IOException {
        log.info("Trying to get key file ");
        FileInputStream file = getKey();
        GoogleCredentials credentials = GoogleCredentials.fromStream(file);
        log.info("Key Found");
        // Create storage client with the loaded credentials
        return StorageOptions.newBuilder().setCredentials(credentials).build().getService();

    }
    public FileInputStream getKey() {
        String directoryPath = gcloudkey;
        log.info("directoryPath: {}",gcloudkey);
        // File extension or name
        String filter = "*.json";
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath), filter)) {
            for (Path path : directoryStream) {
                log.info("Found more than one matching file");
                return new FileInputStream(path.toFile());
            }

        } catch (IOException e) {
            log.info("File not loaded {}",e.getMessage());
        }
        return null;
    }

}
