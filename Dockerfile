FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN useradd --system --uid 10001 --create-home appuser \
    && mkdir -p /tmp/file-converter \
    && chown -R appuser:appuser /app /tmp/file-converter

COPY --from=build /build/target/*.jar /app/app.jar

USER 10001

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.io.tmpdir=/tmp/file-converter"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
