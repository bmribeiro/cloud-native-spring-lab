FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package && cp target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]