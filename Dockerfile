FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon -q

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /workspace/build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --destination extracted && rm app.jar

WORKDIR /app/extracted
RUN java -XX:AOTCacheOutput=app.aot \
         -XX:+UseSerialGC \
         -Dspring.context.exit=onRefresh \
         -Dspring.flyway.enabled=false \
         -Dspring.jpa.hibernate.ddl-auto=none \
         -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
         -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
         -jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-jar", "app.jar"]
