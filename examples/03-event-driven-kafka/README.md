# 03-event-driven-kafka

Laboratório prático de arquitetura orientada a eventos com **Spring Boot**, **Docker Compose**, **API Gateway** e **Apache Kafka**.

Este projeto simula um fluxo de encomendas, pagamentos e notificações. A fronteira externa é síncrona, via REST, mas a comunicação interna entre serviços é assíncrona, baseada em eventos Kafka.

---

## 1. Objetivo do laboratório

Este módulo demonstra uma evolução natural face a exemplos anteriores baseados em REST e RabbitMQ, introduzindo Kafka como broker/event log distribuído.

O objetivo é estudar, de forma isolada e executável localmente:

- separação entre serviços independentes;
- API Gateway como ponto único de entrada HTTP;
- comunicação REST apenas na fronteira externa;
- comunicação assíncrona entre microserviços via Kafka;
- tópicos versionados;
- uso de `orderId` como message key;
- preservação de ordem por entidade de negócio;
- consumer groups;
- retries no consumidor;
- Dead Letter Topic;
- idempotência básica no processamento de eventos;
- observabilidade local com logs e Kafka UI.

---

## 2. Arquitetura geral

```text
Cliente HTTP
  |
  | POST /api/orders
  | GET  /api/orders/{id}
  v
API Gateway :8080
  |
  | HTTP interno
  v
Order Service :8081
  |
  | publica OrderCreatedEvent
  | topic: order.created.v1
  v
Kafka
  |
  | consome OrderCreatedEvent
  v
Payment Service :8082
  |
  | publica PaymentResultEvent
  | topic: payment.result.v1
  v
Kafka
  |
  | consome PaymentResultEvent
  v
Order Service
  |
  | publica OrderStatusChangedEvent
  | topic: order.status-changed.v1
  v
Kafka
  |
  | consome OrderStatusChangedEvent
  v
Notification Service :8083
```

---

## 3. Serviços

| Serviço | Porta host | Responsabilidade |
|---|---:|---|
| `api-gateway` | `8080` | Expõe a API pública e encaminha `/api/orders/**` para o `order-service` |
| `order-service` | `8081` | Cria encomendas, guarda estado em memória, publica e consome eventos |
| `payment-service` | `8082` | Consome eventos de encomenda criada e simula autorização/rejeição de pagamento |
| `notification-service` | `8083` | Consome alterações de estado e simula envio de notificações |
| `kafka` | `9094` | Broker Kafka acessível pelo host em `localhost:9094` |
| `kafka-ui` | `8090` | Interface web para consultar tópicos, mensagens e consumer groups |

Dentro da rede Docker, os serviços Spring Boot usam Kafka através de:

```text
kafka:9092
```

A partir do host, por exemplo a partir de uma IDE ou ferramenta externa, o Kafka fica acessível em:

```text
localhost:9094
```

---

## 4. Estrutura do projeto

```text
03-event-driven-kafka/
├── api-gateway/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/resources/application.properties
│
├── order-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/bmr/orders/
│       ├── api/
│       ├── domain/
│       ├── events/
│       ├── messaging/
│       ├── service/
│       └── store/
│
├── payment-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/bmr/payments/
│       ├── events/
│       └── messaging/
│
├── notification-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/bmr/notifications/
│       ├── events/
│       └── messaging/
│
├── docker-compose.yml
└── README.md
```

---

## 5. Tópicos Kafka

| Tópico | Produtor | Consumidor | Finalidade |
|---|---|---|---|
| `order.created.v1` | `order-service` | `payment-service` | Comunica que uma encomenda foi criada |
| `payment.result.v1` | `payment-service` | `order-service` | Comunica o resultado do pagamento |
| `order.status-changed.v1` | `order-service` | `notification-service` | Comunica alteração de estado da encomenda |
| `order.created.v1.DLT` | Error handler do `payment-service` | Inspeção manual | Guarda mensagens que falharam após retries |

Os tópicos são criados pelas aplicações através de beans `NewTopic`, com:

```text
partitions = 3
replicas    = 1
```

A configuração do broker mantém `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`, para que os tópicos sejam explicitamente declarados pela aplicação.

---

## 6. Fluxo funcional

### 6.1 Criação da encomenda

O cliente chama:

```text
POST /api/orders
```

O `api-gateway` encaminha o pedido para o `order-service`.

O `order-service`:

1. cria uma encomenda em estado `PENDING_PAYMENT`;
2. guarda a encomenda em memória;
3. publica um `OrderCreatedEvent` no tópico `order.created.v1`;
4. devolve `202 Accepted` ao cliente.

### 6.2 Processamento do pagamento

O `payment-service` consome `OrderCreatedEvent`.

A regra didática aplicada é:

```text
amount <= 1000.00 -> AUTHORIZED
amount > 1000.00  -> REJECTED
```

Depois publica `PaymentResultEvent` em:

```text
payment.result.v1
```

### 6.3 Atualização do estado da encomenda

O `order-service` consome `PaymentResultEvent`.

Se o pagamento for autorizado, a encomenda passa para:

```text
PAID
```

Se o pagamento for rejeitado, a encomenda passa para:

```text
REJECTED
```

Depois publica `OrderStatusChangedEvent` em:

```text
order.status-changed.v1
```

### 6.4 Notificação

O `notification-service` consome `OrderStatusChangedEvent` e simula a notificação através de logs.

---

## 7. Requisitos locais

Antes de executar, confirmar que tens disponível:

- Docker;
- Docker Compose v2;
- portas livres: `8080`, `8081`, `8082`, `8083`, `8090`, `9094`;
- opcionalmente, `curl` para testar a API;
- opcionalmente, `jq` para extrair campos JSON nos testes.

---

## 8. Executar o laboratório

Na raiz do projeto:

```bash
docker compose up --build
```

Para executar em segundo plano:

```bash
docker compose up --build -d
```

Ver containers em execução:

```bash
docker compose ps
```

Ver logs dos serviços principais:

```bash
docker compose logs -f order-service payment-service notification-service
```

Aceder ao Kafka UI:

```text
http://localhost:8090
```

Aceder ao gateway:

```text
http://localhost:8080
```

---

## 9. Health checks

Verificar o gateway:

```bash
curl http://localhost:8080/actuator/health
```

Verificar o `order-service` diretamente:

```bash
curl http://localhost:8081/actuator/health
```

Verificar o `payment-service` diretamente:

```bash
curl http://localhost:8082/actuator/health
```

Verificar o `notification-service` diretamente:

```bash
curl http://localhost:8083/actuator/health
```

---

## 10. Testar fluxo aprovado

Criar uma encomenda com valor inferior ou igual a `1000.00`:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-001",
    "amount": 49.90
  }'
```

Resposta esperada:

```http
HTTP/1.1 202 Accepted
```

Exemplo de corpo:

```json
{
  "id": "9f51f62a-8c37-4d7a-9b0f-2c8c6b3f0a62",
  "customerId": "customer-001",
  "amount": 49.90,
  "status": "PENDING_PAYMENT",
  "createdAt": "2026-06-16T12:00:00Z"
}
```

Consultar o estado da encomenda, substituindo `{orderId}` pelo `id` devolvido:

```bash
curl http://localhost:8080/api/orders/{orderId}
```

Após alguns instantes, o estado esperado é:

```json
{
  "status": "PAID"
}
```

Também podes confirmar o fluxo nos logs:

```bash
docker compose logs -f order-service payment-service notification-service
```

---

## 11. Testar fluxo rejeitado

Criar uma encomenda com valor superior a `1000.00`:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-002",
    "amount": 1500.00
  }'
```

A encomenda é criada inicialmente em:

```text
PENDING_PAYMENT
```

Depois do processamento assíncrono, o estado esperado é:

```text
REJECTED
```

Consultar:

```bash
curl http://localhost:8080/api/orders/{orderId}
```

---

## 12. Testar Dead Letter Topic

Para forçar uma falha controlada no `payment-service`, criar uma encomenda com `customerId = "fail"`:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "fail",
    "amount": 10.00
  }'
```

O `payment-service` lança uma exceção de propósito para demonstrar retries e DLT.

O consumidor tenta processar a mensagem novamente. Depois de esgotadas as tentativas configuradas, o evento é enviado para:

```text
order.created.v1.DLT
```

Consultar no Kafka UI:

```text
http://localhost:8090
```

Caminho sugerido no Kafka UI:

```text
Topics -> order.created.v1.DLT -> Messages
```

---

## 13. Observar tópicos e consumer groups

No Kafka UI, consultar:

```text
Topics
```

Tópicos esperados:

```text
order.created.v1
payment.result.v1
order.status-changed.v1
order.created.v1.DLT
```

Consultar também:

```text
Consumer Groups
```

Consumer groups esperados:

```text
payment-service
order-service
notification-service
```

---

## 14. Debug remoto

O `docker-compose.yml` expõe portas de debug Java apenas em `127.0.0.1`:

| Serviço | Porta local de debug | Porta no container |
|---|---:|---:|
| `api-gateway` | `5005` | `5005` |
| `order-service` | `5006` | `5005` |
| `payment-service` | `5007` | `5005` |
| `notification-service` | `5008` | `5005` |

Exemplo de configuração na IDE:

```text
Host: localhost
Port: 5006
Mode: Attach to remote JVM
```

---

## 15. Escalar consumidores

Kafka distribui mensagens por partições dentro de um consumer group. Neste laboratório, os tópicos são criados com três partições, o que permite estudar concorrência de consumo.

Atenção: o `docker-compose.yml` atual usa `container_name`. O Docker Compose não permite escalar serviços que tenham `container_name` fixo, porque os nomes dos containers colidem.

Para testar escala horizontal do `payment-service`, remover ou comentar esta linha no serviço `payment-service`:

```yaml
container_name: lab-payment-service
```

Depois executar:

```bash
docker compose up --build --scale payment-service=3
```

Observar no Kafka UI:

```text
Consumer Groups -> payment-service
```

Com três instâncias e três partições, cada instância pode ficar responsável por uma partição, dependendo do rebalanceamento do grupo.

---

## 16. Decisões arquiteturais

### 16.1 REST na fronteira externa

O cliente não publica diretamente em Kafka. A entrada externa continua a ser HTTP:

```text
POST /api/orders
GET  /api/orders/{id}
```

Isto mantém uma API simples para clientes externos e evita expor detalhes do broker.

### 16.2 Eventos para comunicação interna

Os serviços internos comunicam por eventos de negócio:

```text
OrderCreatedEvent
PaymentResultEvent
OrderStatusChangedEvent
```

Cada evento representa um facto que já ocorreu, não uma chamada direta para executar lógica noutro serviço.

### 16.3 Tópicos versionados

Os tópicos incluem versão no nome:

```text
order.created.v1
payment.result.v1
order.status-changed.v1
```

Isto permite criar uma futura versão `v2` sem quebrar consumidores existentes.

### 16.4 Message key por entidade de negócio

Todos os eventos são publicados com:

```text
Kafka key = orderId
```

Kafka garante ordem dentro de uma partição. Usar o `orderId` como key ajuda a manter os eventos da mesma encomenda na mesma partição.

### 16.5 Idempotência básica

Os consumidores guardam `eventId` em memória para ignorar duplicados já processados.

Esta abordagem é suficiente para fins didáticos, mas não é robusta em produção porque a memória é perdida quando o serviço reinicia.

Numa implementação real, seria preferível persistir os eventos processados, por exemplo:

```sql
CREATE TABLE processed_events (
  event_id UUID NOT NULL,
  consumer_name VARCHAR(120) NOT NULL,
  processed_at TIMESTAMP NOT NULL,
  PRIMARY KEY (event_id, consumer_name)
);
```

### 16.6 Dead Letter Topic

O `payment-service` usa `DefaultErrorHandler` e `DeadLetterPublishingRecoverer`.

Quando uma mensagem falha repetidamente, é enviada para:

```text
<topic-original>.DLT
```

Neste laboratório:

```text
order.created.v1.DLT
```

Isto evita que uma mensagem problemática bloqueie indefinidamente o processamento normal do tópico principal.

---

## 17. Comandos úteis

Subir tudo com rebuild:

```bash
docker compose up --build
```

Subir em background:

```bash
docker compose up --build -d
```

Ver estado dos containers:

```bash
docker compose ps
```

Ver logs de todos os serviços:

```bash
docker compose logs -f
```

Ver logs apenas dos serviços aplicacionais:

```bash
docker compose logs -f api-gateway order-service payment-service notification-service
```

Parar os containers:

```bash
docker compose down
```

Parar e remover volumes:

```bash
docker compose down -v
```

Rebuild sem cache:

```bash
docker compose build --no-cache
```

---

## 18. Troubleshooting

### Porta já ocupada

Se alguma porta estiver ocupada, o Docker Compose pode falhar ao arrancar.

Verificar processos a usar uma porta, por exemplo `8080`:

```bash
lsof -i :8080
```

Ou alterar o mapeamento de portas no `docker-compose.yml`.

### Kafka UI não mostra tópicos

Confirmar que o Kafka está saudável:

```bash
docker compose ps
```

Ver logs do broker:

```bash
docker compose logs -f kafka
```

Confirmar que os serviços Spring Boot arrancaram e criaram os tópicos:

```bash
docker compose logs -f order-service payment-service notification-service
```

### Encomenda fica em `PENDING_PAYMENT`

Verificar se o `payment-service` está ativo:

```bash
docker compose logs -f payment-service
```

Verificar no Kafka UI se existem mensagens em:

```text
order.created.v1
```

Confirmar também se o consumer group `payment-service` está a consumir mensagens.

### Não consigo escalar o `payment-service`

Remover `container_name` do serviço antes de usar:

```bash
docker compose up --scale payment-service=3
```

Com `container_name` fixo, o Docker Compose não consegue criar múltiplos containers para o mesmo serviço.

---

## 19. Resumo técnico

Este laboratório demonstra um fluxo event-driven completo:

```text
REST command -> OrderCreatedEvent -> PaymentResultEvent -> OrderStatusChangedEvent -> Notification
```

O ponto principal não é apenas trocar chamadas REST internas por Kafka. O objetivo é modelar a comunicação entre serviços como uma sequência de factos de negócio, com consumidores independentes, tópicos versionados, grupos de consumo, retries e tratamento explícito de falhas através de DLT.
