# Dockerfile

# Use official maven image as the base image
FROM maven:3.6.3-openjdk-11 as build

# Set the working directory in the image
WORKDIR /app

# Copy the pom.xml file
COPY pom.xml .

# Download all required dependencies into one layer
RUN mvn dependency:go-offline -B

# Copy your other files
COPY src ./src

# Build the project
RUN mvn clean package

# Use openjdk image for running the app
FROM openjdk:11-jre-slim

# Set the working directory
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/target/*.jar ./app.jar

# Expose the port
EXPOSE 3000

# Run the jar file 
ENTRYPOINT ["java","-jar","/app/app.jar"]