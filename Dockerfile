FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src/main ./src/main
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system ctutech && useradd --system --gid ctutech --home-dir /app ctutech && mkdir -p /app/.local && chown -R ctutech:ctutech /app
COPY --from=build --chown=ctutech:ctutech /build/target/luma-tech-0.1.0-SNAPSHOT.jar /app/app.jar
USER ctutech
ENV SPRING_PROFILES_ACTIVE=render
EXPOSE 10000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-jar", "/app/app.jar"]
