#FROM eclipse-temurin:21-jdk as builder
FROM maven:3.8.5-openjdk-17 as builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ ./src/
RUN mvn clean package -DskipTests=true

FROM eclipse-temurin:17 as prod
RUN mkdir /app
COPY --from=builder /app/target/*.jar /app/tr4-group-project-check-tickets-1.0-SNAPSHOT.jar
#COPY --from=builder target/tr4-group-project-check-tickets-1.0-SNAPSHOT.jar tr4-group-project-check-tickets-1.0-SNAPSHOT.jar
ENV SERVER_PORT=8080
WORKDIR /tr4-group-project-check-tickets
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/tr4-group-project-check-tickets-1.0-SNAPSHOT.jar"]

#COPY target/tr4-group-project-check-tickets-1.0-SNAPSHOT.jar tr4-group-project-check-tickets-1.0-SNAPSHOT.jar
#ENV GOOGLE_APPLICATION_CREDENTIALS '~/.config/gcloud/application_default_credentials.json'
#EXPOSE 8080:8080
#ENTRYPOINT ["java","-jar","/tr4-group-project-check-tickets-1.0-SNAPSHOT.jar"]