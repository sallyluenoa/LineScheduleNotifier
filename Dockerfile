#
# Copyright (c) 2026 SallyLueNoa
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Stage 1: Build stage
# Using Amazon Corretto 21 as the build environment
FROM amazoncorretto:21 AS build
WORKDIR /app

# Copy the Gradle executable and configuration files
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY fr-line-agent-core/build.gradle.kts ./fr-line-agent-core/
COPY fr-line-agent-sample-app/build.gradle.kts ./fr-line-agent-sample-app/

# Grant execution permission to the gradlew script
RUN chmod +x gradlew

# Download dependencies separately to leverage Docker layer caching
RUN ./gradlew --no-daemon dependencies

# Copy the source code
COPY fr-line-agent-core/src ./fr-line-agent-core/src
COPY fr-line-agent-sample-app/src ./fr-line-agent-sample-app/src

# Build the application FAT JAR, skipping tests for faster CI/CD
RUN ./gradlew --no-daemon :fr-line-agent-sample-app:build -x test

# Stage 2: Runtime stage
# Using a lightweight Alpine-based JRE for the final image
FROM amazoncorretto:21-alpine
WORKDIR /app

# Define the Google Cloud Project Number with a default for local development
ARG PROJECT_NUMBER=111111111111
ENV PROJECT_NUMBER=${PROJECT_NUMBER}

# Copy only the built JAR file from the build stage
# Note: Ensure the JAR filename pattern matches your build/libs output
COPY --from=build /app/fr-line-agent-sample-app/build/libs/fr-line-agent-sample-app-*-all.jar /app/app.jar

# Cloud Run injects the PORT environment variable at runtime
# We default to 8080 but the app should listen on $PORT
ENV PORT=8080
EXPOSE 8080

# Run the application with optimized memory settings for container environments
# MaxRAMPercentage ensures the JVM respects the container's memory limits
CMD ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
