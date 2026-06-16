# Cloud Native Spring Lab

Laboratório prático de arquitetura cloud-native com **Spring Boot**, **Docker Compose**, **microservices**, **mensageria**, **API Gateway**, **GraphQL**, **Kafka**, **RabbitMQ**, **Service Discovery** e **Config Server**.

Este repositório está organizado como uma sequência progressiva de exemplos. Cada pasta representa um módulo independente, com o seu próprio `README.md`, serviços, configuração e instruções de execução.

---

## Objetivo

O objetivo deste laboratório é demonstrar, de forma incremental, padrões comuns em sistemas distribuídos modernos usando o ecossistema Spring.

Ao longo dos módulos são explorados conceitos como:

* APIs REST com Spring Boot;
* deploy com Docker Compose;
* comunicação síncrona entre serviços;
* comunicação assíncrona orientada a eventos;
* RabbitMQ;
* Apache Kafka;
* API Gateway;
* GraphQL;
* configuração centralizada;
* service discovery;
* observabilidade básica;
* execução local próxima de um ambiente real.

---

## Estrutura do Repositório

```text
cloud-native-spring-lab/
├── 01-rest-docker-compose-vps/
├── 02-event-driven-rabbitmq/
├── 03-event-driven-kafka/
├── 04-graphql-api-gateway-microservices/
└── 05-config-and-service-discovery/
```

---

## Módulos

| Módulo                                 | Tema                              | Descrição                                                                                      |
| -------------------------------------- | --------------------------------- | ---------------------------------------------------------------------------------------------- |
| `01-rest-docker-compose-vps`           | REST + Docker Compose             | Primeiro exemplo com serviços Spring Boot expostos via REST e orquestrados com Docker Compose. |
| `02-event-driven-rabbitmq`             | Event-driven com RabbitMQ         | Introduz comunicação assíncrona entre microservices usando RabbitMQ.                           |
| `03-event-driven-kafka`                | Event-driven com Kafka            | Evolui o modelo assíncrono para Apache Kafka, com produtores, consumidores, tópicos e DLT.     |
| `04-graphql-api-gateway-microservices` | GraphQL + API Gateway             | Demonstra agregação de dados via GraphQL numa camada de gateway sobre microservices.           |
| `05-config-and-service-discovery`      | Config Server + Service Discovery | Introduz configuração centralizada e descoberta de serviços em ambiente distribuído.           |

---

## Pré-requisitos

Para executar os exemplos localmente, é recomendado ter instalado:

* Java 21 ou superior;
* Maven 3.9 ou superior;
* Docker;
* Docker Compose;
* Git;
* curl, Postman ou Insomnia para testar APIs.

Verificar versões:

```bash
java --version
mvn --version
docker --version
docker compose version
```

---

## Como Usar

Clonar o repositório:

```bash
git clone <url-do-repositorio>
cd cloud-native-spring-lab
```

Entrar no módulo pretendido:

```bash
cd 03-event-driven-kafka
```

Executar o ambiente:

```bash
docker compose up -d --build
```

Ver containers ativos:

```bash
docker compose ps
```

Ver logs:

```bash
docker compose logs -f
```

Parar o ambiente:

```bash
docker compose down
```

Parar e remover volumes:

```bash
docker compose down -v
```

---

## Convenções dos Módulos

Cada módulo segue, sempre que possível, a mesma organização:

```text
<modulo>/
├── docker-compose.yml
├── README.md
├── <service-a>/
├── <service-b>/
└── <service-c>/
```

Cada serviço Spring Boot pode conter:

```text
src/
├── main/
│   ├── java/
│   └── resources/
└── test/
```

---

## Tecnologias Utilizadas

Principais tecnologias usadas ao longo do laboratório:

* Spring Boot;
* Spring Web;
* Spring Data JPA;
* Spring AMQP;
* Spring Kafka;
* Spring GraphQL;
* Spring Cloud Config;
* Spring Cloud Netflix Eureka;
* PostgreSQL;
* RabbitMQ;
* Apache Kafka;
* Docker;
* Docker Compose.

---

## Visão Arquitetural

O laboratório cobre vários estilos de comunicação entre serviços:

### Comunicação síncrona

Usada principalmente nos módulos iniciais e em cenários de API Gateway.

```text
Client → API Gateway / REST API → Microservice
```

### Comunicação assíncrona com RabbitMQ

Usada para desacoplar produtores e consumidores através de filas e exchanges.

```text
Producer → RabbitMQ Exchange → Queue → Consumer
```

### Comunicação assíncrona com Kafka

Usada para eventos persistentes, consumidores independentes e processamento distribuído.

```text
Producer → Kafka Topic → Consumer Group → Consumer
```

### Configuração e descoberta de serviços

Usada para centralizar configuração e permitir que serviços encontrem uns aos outros dinamicamente.

```text
Microservice → Config Server
Microservice → Service Registry
```

---

## Portas

As portas podem variar por módulo. Consultar sempre o `README.md` de cada pasta.

Exemplos comuns:

| Serviço             |           Porta típica |
| ------------------- | ---------------------: |
| API Gateway         |                 `8080` |
| Spring Boot service | `8081`, `8082`, `8083` |
| PostgreSQL          |                 `5432` |
| RabbitMQ Management |                `15672` |
| Kafka UI            |                 `8088` |
| Eureka Server       |                 `8761` |
| Config Server       |                 `8888` |

---

## Recomendações de Execução

Antes de iniciar um módulo, garantir que não existem containers antigos a ocupar as mesmas portas:

```bash
docker compose down -v
```

Também pode ser útil limpar containers parados:

```bash
docker container prune
```

E imagens antigas:

```bash
docker image prune
```

---

## Branches e Evolução

Este repositório pode ser usado como trilho de aprendizagem incremental.

Sugestão de evolução:

1. Começar com REST e Docker Compose.
2. Introduzir comunicação assíncrona com RabbitMQ.
3. Comparar RabbitMQ com Kafka.
4. Adicionar um gateway GraphQL.
5. Introduzir Config Server e Service Discovery.
6. Evoluir para observabilidade, tracing, resiliência e deployment em Kubernetes.

