FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
COPY services/auth-service/pom.xml services/auth-service/pom.xml
COPY services/energy-market-service/pom.xml services/energy-market-service/pom.xml
RUN mvn -B -ntp -pl services/energy-market-service dependency:go-offline
COPY services/energy-market-service/src services/energy-market-service/src
RUN mvn -B -ntp -pl services/energy-market-service package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/services/energy-market-service/target/energy-market-service-*.jar /app/app.jar
RUN chown app:app /app/app.jar
USER app

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
