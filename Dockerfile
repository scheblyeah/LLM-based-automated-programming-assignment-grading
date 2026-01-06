FROM openjdk:17-jdk-slim
WORKDIR /usr/src/myapp

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
# Download the JUnit JAR
RUN curl -L -o junit-platform-console-standalone.jar \
    https://search.maven.org/remotecontent?filepath=org/junit/platform/junit-platform-console-standalone/1.9.3/junit-platform-console-standalone-1.9.3.jar

# Keep the container running
CMD ["tail", "-f", "/dev/null"]