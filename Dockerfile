FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY app .
RUN mvn clean package -DskipTests
RUN echo "Verifying JAR contents:" && jar tvf target/*.jar | grep -i application

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
ENV TZ=UTC \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"
EXPOSE 8080
# Run with explicit main class
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.main.banner-mode=off"]