# Start with a Java runtime base image
FROM eclipse-temurin:21-jdk

# Set workdir inside the image
WORKDIR /app

# Copy your built JAR file into the container
COPY target/*.jar app.jar

# Run the JAR when container starts
ENTRYPOINT ["java", "-jar", "app.jar"]