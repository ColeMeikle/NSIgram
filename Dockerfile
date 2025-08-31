# -------- Stage 1: Build the JAR --------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source AFTER dependencies are cached
COPY src ./src

# Build the JAR with Vaadin production build
RUN mvn clean package -Pproduction -DskipTests

# -------- Stage 2: Run the JAR --------
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy built jar from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Run it
ENTRYPOINT ["java", "-jar", "app.jar"]


#This whole Dockerfile is ChatGPT, idk whats happening in this file.