FROM maven:3.9.14-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM azul-zulu:21-jdk
WORKDIR /app
COPY --from=builder /app/app-web/target/app-web-0.0.1-SNAPSHOT.jar /app/app-web-0.0.1-SNAPSHOT.jar
EXPOSE 8080
CMD ["java", "-jar", "app-web-0.0.1-SNAPSHOT.jar"]
