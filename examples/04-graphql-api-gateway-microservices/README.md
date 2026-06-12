# 04 - GraphQL API Gateway com Microserviços Spring Boot

Este projeto demonstra uma arquitetura de microserviços usando **Java 21**, **Spring Boot 4.1.0**, **Spring for GraphQL**, **Spring Web**, **PostgreSQL** e **Docker Compose**.

O objetivo é mostrar como o **GraphQL pode funcionar como API Gateway / Backend for Frontend**, agregando dados de múltiplos microserviços REST independentes.

---

## Arquitetura

```text
Cliente / Browser / Postman / GraphiQL
        |
        | GraphQL
        v
+-------------------+
| graphql-gateway   |
| Spring GraphQL    |
| Porta 4000        |
+-------------------+
      |         |
      | REST    | REST
      v         v
+-------------+ +---------------+
| users       | | orders        |
| service     | | service       |
| Porta 3001  | | Porta 3002    |
+-------------+ +---------------+
      |               |
      v               v
+-------------+ +---------------+
| users-db    | | orders-db     |
| PostgreSQL  | | PostgreSQL    |
+-------------+ +---------------+
```

### Componentes

| Serviço | Responsabilidade | Porta interna |
|---|---|---:|
| `graphql-gateway` | Expõe a API GraphQL e compõe dados dos microserviços | `4000` |
| `users-service` | Gere utilizadores através de REST | `3001` |
| `orders-service` | Gere encomendas através de REST | `3002` |
| `users-db` | Base de dados PostgreSQL dos utilizadores | `5432` |
| `orders-db` | Base de dados PostgreSQL das encomendas | `5432` |

O cliente comunica apenas com o **GraphQL Gateway**. Os microserviços REST e as bases de dados ficam protegidos na rede interna Docker.

---

## Conceitos demonstrados

Este projeto demonstra:

- GraphQL como camada de agregação sobre microserviços REST.
- Separação de responsabilidades por serviço.
- Base de dados independente por microserviço.
- Composição de dados entre `users-service` e `orders-service`.
- Queries e mutations GraphQL.
- Health checks com Spring Boot Actuator.
- Execução local com Docker Compose.
- Preparação para execução em VPS.
- Inicialização automática de tabelas com `schema.sql` e `data.sql`.

---

## Estrutura do projeto

```text
04-graphql-api-gateway-microservices/
├── docker-compose.yml
├── users-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── orders-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
└── graphql-gateway/
    ├── Dockerfile
    ├── pom.xml
    └── src/
```

Cada serviço é um projeto Spring Boot independente, com o seu próprio `pom.xml`.

Não existe `pom.xml` raiz. Por isso, no `docker-compose.yml`, cada serviço deve usar a sua própria pasta como contexto de build:

```yaml
users-service:
  build:
    context: ./users-service
    dockerfile: Dockerfile

orders-service:
  build:
    context: ./orders-service
    dockerfile: Dockerfile

graphql-gateway:
  build:
    context: ./graphql-gateway
    dockerfile: Dockerfile
```

---

## Como correr localmente

A partir da raiz do projeto:

```bash
docker compose down -v
docker compose up --build
```

O comando `down -v` remove os volumes PostgreSQL antigos. Isto é útil quando se quer recriar as tabelas a partir dos ficheiros `schema.sql` e `data.sql`.

Depois de iniciar, confirmar o estado dos containers:

```bash
docker compose ps
```

Os serviços devem aparecer como `healthy`.

---

## Endpoints principais

| Endpoint | Descrição |
|---|---|
| `http://localhost:4000/graphql` | Endpoint GraphQL |
| `http://localhost:4000/graphiql` | Interface visual GraphiQL |
| `http://localhost:4000/actuator/health` | Health check do gateway |

Os microserviços REST não precisam de estar expostos diretamente no host, porque são consumidos pelo `graphql-gateway` dentro da rede Docker.

---

## Testar health check

```bash
curl http://localhost:4000/actuator/health
```

Resposta esperada:

```json
{
  "status": "UP"
}
```

---

## Testar no GraphiQL

Abrir no browser:

```text
http://localhost:4000/graphiql
```

### Query de health lógico

```graphql
query {
  health {
    gateway
    usersService
    ordersService
  }
}
```

Resposta esperada:

```json
{
  "data": {
    "health": {
      "gateway": "ok",
      "usersService": "ok",
      "ordersService": "ok"
    }
  }
}
```

---

## Queries GraphQL úteis

### Listar utilizadores

```graphql
query {
  users {
    id
    name
    email
    role
    createdAt
  }
}
```

### Listar encomendas

```graphql
query {
  orders {
    id
    userId
    total
    status
    createdAt
  }
}
```

### Listar encomendas com dados do utilizador

Esta query demonstra a composição de dados entre microserviços.

```graphql
query {
  orders {
    id
    total
    status
    user {
      id
      name
      email
    }
  }
}
```

Fluxo interno:

```text
Cliente
  -> graphql-gateway
      -> orders-service
      -> users-service
```

### Listar utilizadores com encomendas

```graphql
query {
  users {
    id
    name
    email
    orders {
      id
      total
      status
    }
  }
}
```

---

## Mutations GraphQL úteis

### Criar utilizador

```graphql
mutation {
  createUser(input: {
    name: "Diogo Ramos"
    email: "diogo@example.com"
    role: CUSTOMER
  }) {
    id
    name
    email
    role
  }
}
```

### Criar encomenda

```graphql
mutation {
  createOrder(input: {
    userId: "1"
    total: 89.90
    status: PENDING
  }) {
    id
    total
    status
    user {
      id
      name
      email
    }
  }
}
```

### Atualizar estado de uma encomenda

```graphql
mutation {
  updateOrderStatus(id: "1", status: SHIPPED) {
    id
    status
    total
  }
}
```

Estados possíveis:

```text
PENDING
PAID
SHIPPED
CANCELLED
```

---

## Testar via terminal

No PowerShell, usar `curl.exe`:

```powershell
curl.exe -X POST http://localhost:4000/graphql `
  -H "Content-Type: application/json" `
  -d "{\"query\":\"query { users { id name email role } }\"}"
```

Query com composição entre encomendas e utilizadores:

```powershell
curl.exe -X POST http://localhost:4000/graphql `
  -H "Content-Type: application/json" `
  -d "{\"query\":\"query { orders { id total status user { id name email } } }\"}"
```

---

## Consultar as bases de dados

### Users DB

```bash
docker exec -it cloudgraphql-users-db psql -U app -d usersdb
```

Dentro do `psql`:

```sql
\dt
SELECT * FROM users;
```

Ou diretamente:

```bash
docker exec -it cloudgraphql-users-db psql -U app -d usersdb -c "SELECT * FROM users;"
```

### Orders DB

```bash
docker exec -it cloudgraphql-orders-db psql -U app -d ordersdb
```

Dentro do `psql`:

```sql
\dt
SELECT * FROM orders;
```

Ou diretamente:

```bash
docker exec -it cloudgraphql-orders-db psql -U app -d ordersdb -c "SELECT * FROM orders;"
```

---

## Inicialização das tabelas

As tabelas são criadas pelos ficheiros SQL dentro de cada serviço:

```text
users-service/src/main/resources/schema.sql
users-service/src/main/resources/data.sql

orders-service/src/main/resources/schema.sql
orders-service/src/main/resources/data.sql
```

A propriedade abaixo força o Spring Boot a executar os scripts SQL no arranque:

```properties
spring.sql.init.mode=always
```

Se a base de dados já tiver sido criada antes sem tabelas, remover os volumes:

```bash
docker compose down -v
docker compose up --build
```

---

## Execução numa VPS

Na VPS, os passos principais são:

```bash
git clone <url-do-repositorio>
cd 04-graphql-api-gateway-microservices
docker compose up -d --build
```

Confirmar containers:

```bash
docker compose ps
```

Ver logs:

```bash
docker compose logs -f
```

O gateway fica disponível em:

```text
http://<IP_DA_VPS>:4000/graphql
http://<IP_DA_VPS>:4000/graphiql
```

Em produção, recomenda-se colocar um reverse proxy, como Nginx, à frente do gateway e expor apenas as portas HTTP/HTTPS públicas.

---

## Troubleshooting

### Erro: `COPY pom.xml: not found`

Causa provável: o `docker-compose.yml` está a usar o contexto de build errado.

Como este projeto não tem `pom.xml` raiz, cada serviço deve usar a sua própria pasta como contexto:

```yaml
build:
  context: ./users-service
  dockerfile: Dockerfile
```

### Erro: `COPY src: not found`

Causa provável: a pasta `src` não existe dentro do serviço que está a ser construído.

Verificar:

```bash
ls users-service/src
ls orders-service/src
ls graphql-gateway/src
```

### Erro: `relation "users" does not exist`

Causa provável: a base foi criada antes de existirem `schema.sql` e `data.sql`.

Resolver com:

```bash
docker compose down -v
docker compose up --build
```

### Ver logs de um serviço específico

```bash
docker compose logs -f graphql-gateway
docker compose logs -f users-service
docker compose logs -f orders-service
```

---

## Resumo técnico

Este projeto usa GraphQL como ponto único de entrada para o cliente, mantendo os microserviços REST independentes. O `graphql-gateway` não possui dados próprios; ele apenas orquestra chamadas aos serviços internos e compõe a resposta final.

A principal vantagem demonstrada é a possibilidade de o cliente pedir exatamente os dados necessários numa única operação GraphQL, mesmo quando esses dados estão distribuídos por vários serviços.
