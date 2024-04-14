package za.co.archiver.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import za.co.archiver.service.BackupServiceImpl;

import java.util.List;

@Controller
@RequestMapping("/Archive")
public class ArchiveController {
    private final Logger log = LoggerFactory.getLogger(ArchiveController.class);

    //  backupService.startBackUp();

    private  final BackupServiceImpl backupService;

    public ArchiveController(BackupServiceImpl backupService) {
        this.backupService = backupService;
    }


    @GetMapping("/listObjects")
    public void listObjects() {
        try {
            backupService.listObjects();
            //  , downloadObject
        } catch (Exception e){
            log.info("Failed");
        }/*
        return new ResponseEntity<>("Success",HttpStatusCode.valueOf(200));*/
    }


    @GetMapping("/listObjectsWithPrefix")
    public void listObjectsWithPrefix() {
        try {
            backupService.listObjectsWithPrefix();
            //  , downloadObject
        } catch (Exception e){
            log.info("Failed");
        }/*
        return new ResponseEntity<>("Success",HttpStatusCode.valueOf(200));*/
    }



    @GetMapping("/downloadObject")
    public String downloadObject() {
        try {
            String date = "2024_03_15";
            //log.info("Date File selected {}", date);
           // backupService.downloadObject(date);
            backupService.downloadBackUp(date);
            //  , downloadObject

        } catch (Exception e){
            log.info(e.getMessage());
            return "Download Failed";
        }
        return "File Downloaded";
    }

}
