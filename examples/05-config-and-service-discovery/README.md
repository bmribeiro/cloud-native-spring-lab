> Nota: os módulos Spring usam `application.properties` em `src/main/resources`. O `config-repo` também usa ficheiros `.properties` para manter o exemplo consistente.

# 05 - Config and Service Discovery

Projeto didático do repositório `cloud-native-spring-lab` para demonstrar dois padrões fundamentais em arquiteturas cloud-native com Spring:

- configuração centralizada com Spring Cloud Config Server;
- descoberta de serviços com Eureka;
- comunicação entre microserviços usando nome lógico do serviço em vez de URL fixa.

## Arquitetura

```text
                  ┌────────────────────┐
                  │   config-server    │
                  │      :8888         │
                  └─────────┬──────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ↓                   ↓                   ↓
┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│service-registry│  │ product-service│  │  order-service │
│ Eureka :8761   │  │     :8081      │  │     :8082      │
└────────────────┘  └────────────────┘  └───────┬────────┘
                                                 │
                                                 ↓
                                      calls product-service
                                      through Eureka
```

## Módulos

```text
05-config-and-service-discovery/
├── config-server/
├── service-registry/
├── product-service/
├── order-service/
├── config-repo/
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Tecnologias

- Java 21
- Maven
- Spring Boot 4.0.7
- Spring Cloud 2025.1.2
- Spring Cloud Config Server
- Spring Cloud Netflix Eureka
- Docker Compose

## Portas

| Serviço | Porta |
|---|---:|
| config-server | 8888 |
| service-registry | 8761 |
| product-service | 8081 |
| order-service | 8082 |

## Como executar com Docker Compose

Na raiz do projeto:

```bash
docker compose up --build
```

Depois acede a:

```text
Eureka Dashboard:
http://localhost:8761

Config Server:
http://localhost:8888/product-service/dev
http://localhost:8888/order-service/dev
```

## Como executar localmente sem Docker

Terminal 1:

```bash
mvn -pl config-server spring-boot:run
```

Terminal 2:

```bash
mvn -pl service-registry spring-boot:run
```

Terminal 3:

```bash
SPRING_PROFILES_ACTIVE=dev mvn -pl product-service spring-boot:run
```

Terminal 4:

```bash
SPRING_PROFILES_ACTIVE=dev mvn -pl order-service spring-boot:run
```

No Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"; mvn -pl product-service spring-boot:run
$env:SPRING_PROFILES_ACTIVE="dev"; mvn -pl order-service spring-boot:run
```

## Endpoints

### product-service

```http
GET http://localhost:8081/products
GET http://localhost:8081/products/1
GET http://localhost:8081/config-info
```

### order-service

```http
POST http://localhost:8082/orders
Content-Type: application/json

{
  "productId": 1,
  "quantity": 2
}
```

```http
GET http://localhost:8082/orders/1
GET http://localhost:8082/config-info
```

## Testes com curl

Ver configuração externa do product-service:

```bash
curl http://localhost:8081/config-info
```

Resposta esperada:

```json
{
  "service": "product-service",
  "message": "DEV configuration loaded from Spring Cloud Config Server - product-service",
  "activeProfiles": ["dev"]
}
```

Criar uma encomenda:

```bash
curl -X POST http://localhost:8082/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'
```

Resposta esperada:

```json
{
  "id": 1,
  "product": {
    "id": 1,
    "name": "Laptop",
    "price": 1299.99
  },
  "quantity": 2,
  "totalPrice": 2599.98
}
```

## Conceitos demonstrados

### 1. Configuração centralizada

Os ficheiros em `config-repo/` deixam de estar embutidos dentro dos microserviços. O `product-service` e o `order-service` carregam configuração a partir do `config-server`.

### 2. Configuração por perfil

Os ficheiros:

```text
product-service.properties
product-service-dev.properties
order-service.properties
order-service-dev.properties
```

permitem demonstrar configuração base e configuração específica do perfil `dev`.

### 3. Service discovery

O `product-service` e o `order-service` registam-se no Eureka. O `order-service` chama o `product-service` através do nome lógico:

```text
http://product-service/products/{id}
```

### 4. Comunicação entre microserviços

Fluxo principal:

```text
Cliente → order-service → product-service
```

O `order-service` consulta o `product-service` antes de criar uma encomenda.

## Próximas melhorias possíveis

- adicionar Spring Cloud Gateway;
- adicionar Resilience4j para circuit breaker;
- adicionar Spring Cloud Bus para refresh distribuído de configuração;
- mover `config-repo/` para um repositório Git externo;
- adicionar testes de integração com Testcontainers;
- adicionar observabilidade com Prometheus e Grafana.
