FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY .mvn ./.mvn
COPY mvnw pom.xml ./

COPY src ./src

RUN --network=host ./mvnw clean package -DskipTests -Dmaven.wagon.http.retryHandler.count=3

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dfile.encoding=UTF-8", "app.jar"]