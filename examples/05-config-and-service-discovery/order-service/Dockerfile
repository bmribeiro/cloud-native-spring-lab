FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY config-server/pom.xml config-server/pom.xml
COPY service-registry/pom.xml service-registry/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY order-service/src order-service/src
RUN mvn -pl order-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/order-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
