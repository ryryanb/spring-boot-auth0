# Use an official Java runtime as a parent image
FROM eclipse-temurin:17-jdk

# Set the working directory in the container
WORKDIR /app

# Copy the build JAR file into the container
COPY target/*.jar app.jar

# Expose the port (matching your `server.port` in Spring Boot)
EXPOSE 3000

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
