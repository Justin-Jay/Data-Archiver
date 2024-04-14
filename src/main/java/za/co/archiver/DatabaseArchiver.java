package za.co.archiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean;
import za.co.archiver.service.BackupServiceImpl;

@SpringBootApplication
public class DatabaseArchiver {

    Logger log = LoggerFactory.getLogger(DatabaseArchiver.class);

    @Autowired
    BackupServiceImpl backupService;

    public static void main(String[] args) {
        SpringApplication.run(DatabaseArchiver.class, args);
    }


    @Bean
    public CommandLineRunner appStarter() {
        return (args) -> {
            log.info("Application Started...");
           // backupService.initBackUpJob();
        };
    }
}
