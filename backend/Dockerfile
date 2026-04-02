FROM gradle:8-jdk21 AS build

COPY --chmod=755 /scripts/setup-k8s.sh /scripts/setup-k8s.sh
RUN /scripts/setup-k8s.sh

WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y python3 && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/build/libs/*.jar app.jar
COPY backend/grading/ backend/grading/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
