# Fase de build: usa Maven + JDK 21 para compilar o projeto e gerar o .jar
FROM maven:3.9-eclipse-temurin-21 AS build

# Define /app como diretoria de trabalho dentro do container
WORKDIR /app

# Copia o ficheiro Maven e o código-fonte para a imagem de build
COPY pom.xml .
COPY src ./src

# Compila o projeto sem executar testes e copia o JAR gerado para /app/app.jar
RUN mvn -q -DskipTests package && cp target/*.jar app.jar

# Fase de runtime: usa apenas JRE 21 em Alpine para executar a aplicação
FROM eclipse-temurin:21-jre-alpine

# Define /app como diretoria de trabalho na imagem final
WORKDIR /app

# Instala curl, normalmente usado pelo healthcheck do Docker Compose
RUN apk add --no-cache curl

# Copia o JAR gerado na fase de build para a imagem final
COPY --from=build /app/app.jar app.jar

# Documenta que a aplicação escuta na porta 8082 dentro do container
EXPOSE 8082

# Comando executado ao iniciar o container; arranca a aplicação Spring Boot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]