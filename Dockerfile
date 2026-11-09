# Stage 1: build the jar with the JDK and the Maven wrapper.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN CI=true ./mvnw -B -q package -DskipTests

# Stage 2: run it with only the JRE, as a normal (non-root) user.
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/target/qr-asset-manager-*.jar app.jar
USER app
EXPOSE 8080
# Use at most 75% of the container's memory (free hosting gives 512 MB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
