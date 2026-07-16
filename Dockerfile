FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/ai-chat-app.jar app.jar

RUN useradd --system --uid 10001 --home-dir /app --shell /usr/sbin/nologin appuser \
    && chown appuser /app

ENV SERVER_PORT=8080
EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
