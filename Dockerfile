# Stage 1: Build (No changes needed here)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run (Optimized for 512MB RAM)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/nixathon-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Xss256k", "-XX:+UseSerialGC", "-jar", "app.jar"]