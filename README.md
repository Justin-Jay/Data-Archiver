This repository hosts hosts justinmaboshego/data-archiver project

The project is used to back up a MYSQL Database dump file to a Google GCP Storage. 

Spring Application Events is used to trigger the email notification to the admin user after completion of the task. 

The application makes use of Spring Boot & Java to upload a MYSQL database dump (all-databases.sql) file.
This file is retrieved from a running container and uploaded to the cloud for back up. 
