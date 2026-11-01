FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
RUN mvn -B -ntp -pl services/api-gateway dependency:go-offline
COPY services/api-gateway/src services/api-gateway/src
RUN mvn -B -ntp -pl services/api-gateway package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/services/api-gateway/target/api-gateway-*.jar /app/app.jar
RUN chown app:app /app/app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
