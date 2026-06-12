FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 3001 5005
ENTRYPOINT ["java", "-jar", "/app/app.jar"]