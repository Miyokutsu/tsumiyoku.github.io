# --- Build stage (Maven) ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY app/pom.xml app/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -f app/pom.xml -q -DskipTests dependency:go-offline
COPY app /app
RUN --mount=type=cache,target=/root/.m2 mvn -f app/pom.xml -q -DskipTests package

# --- Run stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=UTC \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8" \
    SERVER_PORT=8080
EXPOSE 8080
COPY --from=build /app/target/app-*.jar /app/app.jar
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s CMD bash -lc "</dev/tcp/127.0.0.1/${SERVER_PORT}" || exit 1
ENTRYPOINT ["bash","-lc","java $JAVA_OPTS -jar /app/app.jar"]