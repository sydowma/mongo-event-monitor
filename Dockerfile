# Use an image that includes Maven and OpenJDK 17
FROM maven:3.8-openjdk-17 as builder

# Set the working directory in the container
WORKDIR /app

# Copy only the POM file and source code (to cache dependencies)
COPY pom.xml /app/
COPY src /app/src

# Build the application without running tests to speed up the build
RUN mvn package -DskipTests

# Use a JDK 17 slim image for the final image to reduce size
FROM openjdk:17-slim

# Set the working directory for the runtime environment
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=builder /app/target/*.jar /app/app.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "/app/app.jar"]