FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy all files
COPY . .

# Ensure mvnw has execute permissions and build the production jar
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Dynamically bind to the platform's assigned PORT with container-aware memory limits
CMD ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8080} -jar app.jar"]
