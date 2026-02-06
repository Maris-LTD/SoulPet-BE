FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw; ./mvnw -B dependency:go-offline -DskipTests

COPY src src
RUN ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -g 1000 appgroup; adduser -u 1000 -G appgroup -D appuser
WORKDIR /app

COPY --from=builder /app/target/SoulPetBackEnd-0.0.1-SNAPSHOT.jar app.jar

USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
