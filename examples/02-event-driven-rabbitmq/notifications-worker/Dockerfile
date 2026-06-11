# Imagem base usada para compilar a aplicação.
# Inclui Maven 3.9.9 e JDK 21, numa imagem Alpine mais leve.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

# Define o diretório de trabalho dentro do container de build.
WORKDIR /app

# Copia o ficheiro pom.xml para o container.
# Permite ao Maven identificar o projeto e resolver as dependências.
COPY pom.xml .

# Copia o código-fonte da aplicação para o container.
COPY src ./src

# Compila e empacota a aplicação num ficheiro .jar.
# -q reduz o output do Maven.
# -DskipTests evita a execução dos testes durante o build.
RUN mvn -q -DskipTests package


# Imagem final usada para executar a aplicação.
# Contém apenas o JRE 21, suficiente para correr o .jar.
FROM eclipse-temurin:21-jre-alpine

# Define o diretório de trabalho no container final.
WORKDIR /app

# Copia o .jar gerado na fase de build para a imagem final.
# O ficheiro é renomeado para app.jar.
COPY --from=build /app/target/*.jar app.jar

# Comando executado quando o container arranca.
# Inicia a aplicação Java/Spring Boot.
ENTRYPOINT ["java", "-jar", "app.jar"]
