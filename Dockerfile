FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /opt/app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY ./src ./src
RUN ./mvnw clean install

FROM eclipse-temurin:25-jre-noble
WORKDIR /opt/app
EXPOSE 8080
COPY --from=builder /opt/app/target/*.jar /opt/app/depot.jar

# Create application user and group
RUN groupadd -r depot && useradd -r -g depot depot

# Create application directories
RUN mkdir -p /var/local/depot

# Set ownership of application directories
RUN chown -R depot:depot /var/local/depot /opt/app

# Switch to non-privileged user
USER depot

ENTRYPOINT ["java", "-jar", "/opt/app/depot.jar"]
LABEL org.opencontainers.image.source="https://github.com/unibas-medfak/depot"
LABEL org.opencontainers.image.description="Simple and secure file storage service"

