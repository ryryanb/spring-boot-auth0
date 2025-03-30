# Use official Gradle image with JDK 17
FROM gradle:8.3-jdk17

# Set working directory
WORKDIR /app

# Copy everything into the container
COPY . .

# Build the application
RUN gradle build --no-daemon

# Expose the correct port (default for Spring Boot is 8080)
EXPOSE 3000

# Run the application
CMD ["java", "-jar", "build/libs/mvc-login-0.0.1-SNAPSHOT.jar"]
