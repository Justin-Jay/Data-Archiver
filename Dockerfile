FROM eclipse-temurin:17-jre AS builder

WORKDIR /DataArchiver
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} DataArchiver.jar
RUN java -Djarmode=layertools -jar DataArchiver.jar extract

FROM eclipse-temurin:17-jre

COPY --from=builder DataArchiver/dependencies/ ./
COPY --from=builder DataArchiver/spring-boot-loader/ ./
COPY --from=builder DataArchiver/snapshot-dependencies/ ./
COPY --from=builder DataArchiver/application/ ./

RUN mkdir -p /Gcloud/Key/

RUN mkdir -p /data/backups/

RUN mkdir -p /DataArchiver/Logs/

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
