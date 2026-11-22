# ---- Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package

# ---- Run
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 zoe
COPY --from=build /app/target/zoe-backend-*.jar /app/app.jar
USER zoe
EXPOSE 8080
ENV TZ=Europe/Paris
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
