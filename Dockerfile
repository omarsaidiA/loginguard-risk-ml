# 1. Frontend: static files built with the exact versions of package-lock.json
FROM node:24.22.2-alpine@sha256:a34ab6d7f6f4813f4d0c5469d74685604b962fbdbf5793a7bc7b4998ecb867c8 AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
# --ignore-scripts: no dependency gets to run code during installation (a common malware vector)
RUN npm ci --no-fund --no-audit --ignore-scripts
COPY frontend/ ./
RUN npm run build

# 2. Backend: the frontend is embedded as static resources, one artifact serves both
FROM eclipse-temurin:21.0.13_9-jdk-alpine AS backend
WORKDIR /workspace
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src src
COPY --from=frontend /app/dist src/main/resources/static
# Tests run in CI against a real PostgreSQL (Testcontainers), not inside the image build
RUN ./mvnw -B -q -DskipTests package && cp target/loginguard-*.jar app.jar

# 3. Runtime: JRE only, non-root, read-only binary
FROM eclipse-temurin:21.0.13_9-jre-alpine
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app -H -s /sbin/nologin app
COPY --from=backend /workspace/app.jar /app/app.jar
USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.io.tmpdir=/tmp"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
