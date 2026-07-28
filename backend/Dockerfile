FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S socialcontent && adduser -S socialcontent -G socialcontent
WORKDIR /app
COPY --from=build --chown=socialcontent:socialcontent \
    /workspace/target/sscm-0.0.1-SNAPSHOT.jar /app/app.jar

USER socialcontent
ENV PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -q -O - "http://127.0.0.1:${PORT}/api/health" >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
