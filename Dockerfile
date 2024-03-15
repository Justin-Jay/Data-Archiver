FROM eclipse-temurin:17-jre AS builder

WORKDIR /DatabaseArchiver
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} DatabaseArchiver.jar
RUN java -Djarmode=layertools -jar DatabaseArchiver.jar extract

FROM eclipse-temurin:17-jre

COPY --from=builder DatabaseArchiver/dependencies/ ./
COPY --from=builder DatabaseArchiver/spring-boot-loader/ ./
COPY --from=builder DatabaseArchiver/snapshot-dependencies/ ./
COPY --from=builder DatabaseArchiver/application/ ./

RUN mkdir -p /Gcloud/Key/

RUN mkdir -p /data/backups/

RUN mkdir -p /DataArchiver/Logs/

# Entry point or command to start your application
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
