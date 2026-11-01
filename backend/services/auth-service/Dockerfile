FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY services/auth-service/pom.xml services/auth-service/pom.xml
RUN mvn -B -ntp -pl services/auth-service dependency:go-offline
COPY services/auth-service/src services/auth-service/src
RUN mvn -B -ntp -pl services/auth-service package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/services/auth-service/target/auth-service-*.jar /app/app.jar
RUN chown app:app /app/app.jar
USER app

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
