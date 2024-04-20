package za.co.backups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean;
import za.co.backups.service.BackupServiceImpl;

@SpringBootApplication
public class DataArchiver {

    Logger log = LoggerFactory.getLogger(DataArchiver.class);

    @Autowired
    BackupServiceImpl backupServiceImpl;

    public static void main(String[] args) {
        SpringApplication.run(DataArchiver.class, args);
    }


    @Bean
    public CommandLineRunner appStarter() {
        return (args) -> {
            log.info("Application Started...");
            backupServiceImpl.startBackUp();
        };
    }
}
