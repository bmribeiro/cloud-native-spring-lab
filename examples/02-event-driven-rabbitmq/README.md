# 02-event-driven-rabbitmq

Microprojeto académico de arquitetura **event-driven** com **Java 21**, **Spring Boot**, **RabbitMQ**, **PostgreSQL** e **Docker Compose**.

O projeto demonstra como desacoplar a criação de uma encomenda do processamento posterior de uma notificação usando o **Transactional Outbox Pattern**. A API grava a encomenda e o evento na mesma transação SQL; depois, um publicador assíncrono lê a tabela de outbox e publica o evento no RabbitMQ. Um worker separado consome o evento e simula o envio de uma notificação.

---

## Objetivo académico

O objetivo deste microprojeto é demonstrar, de forma prática, os seguintes conceitos:

- comunicação assíncrona entre serviços;
- integração entre Spring Boot, PostgreSQL e RabbitMQ;
- uso do **Transactional Outbox Pattern** para evitar perda de eventos;
- entrega de mensagens segundo o princípio **At-Least-Once Delivery**;
- processamento idempotente no consumidor;
- retries com atraso através de fila com TTL;
- encaminhamento final para **DLQ** quando o processamento falha repetidamente;
- execução local isolada com Docker Compose.

---

## Serviços da solução

| Serviço | Responsabilidade | Porta |
|---|---|---:|
| `orders-api` | API REST para criação de encomendas e geração de eventos de domínio | `8080` |
| `notifications-worker` | Consumidor assíncrono que processa eventos `order.created.v1` e simula notificações | `8081` dentro do container |
| `postgres` | Base de dados relacional usada para encomendas, outbox e logs de processamento | não exposta no host |
| `rabbitmq` | Broker de mensagens usado para comunicação assíncrona | UI em `127.0.0.1:15672` |

---

## Funcionalidades implementadas

### `orders-api`

- Expõe o endpoint `POST /orders`.
- Valida o corpo da requisição com Bean Validation.
- Cria uma encomenda com estado inicial `PENDING`.
- Cria um evento de domínio `order.created.v1`.
- Guarda a encomenda e o evento na mesma transação SQL.
- Guarda o evento na tabela `outbox_events` antes de qualquer publicação no RabbitMQ.
- Publica eventos pendentes de forma assíncrona através de um scheduler.
- Usa RabbitMQ Publisher Confirms antes de marcar o evento como publicado.
- Marca eventos publicados através da coluna `published_at`.
- Regista falhas de publicação em `attempts` e `last_error`.

### `notifications-worker`

- Consome mensagens da fila `orders.created.notifications.q`.
- Usa confirmação manual de mensagens com `basicAck` e `basicReject`.
- Processa apenas eventos do tipo `order.created.v1`.
- Simula o envio de uma notificação para o email do cliente.
- Guarda o resultado em `notification_log`.
- Guarda o `event_id` em `processed_messages` para garantir idempotência.
- Evita duplicar trabalho quando a mesma mensagem é entregue mais de uma vez.
- Força falha quando o email termina em `@fail.local`, permitindo testar retry e DLQ.

---

## Resumo da arquitetura

```txt
Cliente HTTP
   |
   | POST /orders
   v
orders-api
   |
   | Transação SQL única
   | - INSERT orders
   | - INSERT outbox_events
   v
PostgreSQL
   |
   | Scheduler lê eventos com published_at IS NULL
   v
OutboxPublisher
   |
   | Publica mensagem persistente com Publisher Confirm
   v
RabbitMQ
   |
   | exchange: orders.events
   | routing key: order.created.v1
   v
queue: orders.created.notifications.q
   |
   v
notifications-worker
   |
   | Processamento idempotente
   | - INSERT processed_messages
   | - INSERT notification_log
   v
PostgreSQL
```

---

## Arquitetura RabbitMQ

| Componente | Nome | Tipo / função |
|---|---|---|
| Exchange principal | `orders.events` | `direct exchange` durável para eventos de encomendas |
| Exchange de retry | `orders.retry` | `direct exchange` usado para reencaminhar mensagens falhadas |
| Exchange de DLQ | `orders.dlx` | `direct exchange` usado para mensagens definitivamente falhadas |
| Routing key principal | `order.created.v1` | Encaminha eventos de encomenda criada |
| Routing key de retry | `order.created.retry.10s` | Encaminha mensagens para a fila de retry |
| Routing key de falha final | `order.created.failed` | Encaminha mensagens para a DLQ |
| Fila principal | `orders.created.notifications.q` | Fila consumida pelo worker |
| Fila de retry | `orders.created.notifications.retry.10s.q` | Fila com TTL de 10 segundos |
| Fila final | `orders.created.notifications.dlq` | Dead Letter Queue final |

Fluxo de retry:

```txt
orders.created.notifications.q
   |
   | basicReject(requeue=false)
   v
orders.retry
   |
   v
orders.created.notifications.retry.10s.q
   |
   | TTL 10 segundos
   v
orders.events
   |
   v
orders.created.notifications.q
```

Quando o número máximo de tentativas é atingido, a mensagem é enviada para:

```txt
orders.created.notifications.dlq
```

---

## Transactional Outbox Pattern

O **Transactional Outbox Pattern** é usado para resolver o problema de consistência entre a base de dados e o broker de mensagens.

Sem este padrão, a aplicação teria de fazer duas operações separadas:

1. gravar a encomenda na base de dados;
2. publicar o evento no RabbitMQ.

Esse modelo cria um problema de **dual write**. Por exemplo, a base de dados pode confirmar a encomenda, mas a publicação no RabbitMQ pode falhar logo a seguir. Nesse caso, a encomenda existe, mas nenhum serviço externo recebe o evento.

Neste projeto, a solução é:

1. o cliente chama `POST /orders`;
2. o método `OrderService.createOrder()` inicia uma transação com `@Transactional`;
3. a encomenda é guardada na tabela `orders`;
4. o evento `order.created.v1` é criado com um `eventId` único;
5. o evento é serializado como JSON dentro de um envelope;
6. o evento é guardado na tabela `outbox_events` na mesma transação da encomenda;
7. a transação SQL é confirmada;
8. o `OutboxPublisher` executa periodicamente;
9. o publisher procura eventos com `published_at IS NULL`;
10. os eventos são bloqueados com `FOR UPDATE SKIP LOCKED`, permitindo segurança em cenários com mais de um publicador;
11. o evento é publicado no RabbitMQ;
12. a aplicação espera o Publisher Confirm;
13. apenas depois do `ack` do RabbitMQ o evento é marcado com `published_at`.

Fluxo simplificado:

```txt
BEGIN TRANSACTION
  INSERT INTO orders (...)
  INSERT INTO outbox_events (...)
COMMIT

Scheduler:
  SELECT eventos não publicados
  PUBLISH para RabbitMQ
  aguarda Publisher Confirm
  UPDATE outbox_events SET published_at = now()
```

A consequência principal é que a criação da encomenda e o registo do evento passam a ser atómicos do ponto de vista da base de dados.

---

## Envelope do evento

O evento publicado no RabbitMQ é serializado com um envelope semelhante a este:

```json
{
  "eventId": "uuid-do-evento",
  "eventType": "order.created.v1",
  "eventVersion": 1,
  "occurredAt": "2026-06-16T10:00:00Z",
  "payload": {
    "orderId": "uuid-da-encomenda",
    "customerEmail": "miguel@example.com",
    "amountCents": 2500,
    "currency": "EUR"
  }
}
```

O `eventId` é usado como identificador global da mensagem e também como chave de idempotência no consumidor.

---

## At-Least-Once Delivery

Este projeto segue o princípio **At-Least-Once Delivery**: cada evento deve ser entregue e processado pelo menos uma vez, desde que os serviços dependentes voltem a ficar disponíveis.

Isso significa que a arquitetura privilegia **não perder mensagens**, aceitando que uma mensagem possa ser entregue mais de uma vez.

Duplicações podem acontecer em vários cenários:

- o evento é publicado no RabbitMQ, mas a aplicação falha antes de atualizar `published_at`;
- o RabbitMQ redelivera uma mensagem não confirmada;
- o worker processa a mensagem, mas falha antes de executar `basicAck`;
- a mensagem passa por retry e volta à fila principal;
- existem múltiplas instâncias de workers a consumir da mesma fila.

Por isso, o consumidor precisa ser idempotente.

Neste projeto, a idempotência é implementada com a tabela `processed_messages`:

```sql
CREATE TABLE processed_messages (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Antes de processar uma notificação, o worker verifica se o `event_id` já existe. Se existir, o evento já foi tratado e a mensagem pode ser confirmada sem repetir a operação.

Portanto, a garantia deste projeto é:

```txt
At-Least-Once Delivery + consumidor idempotente
```

Não é uma arquitetura **Exactly-Once Delivery**. A duplicação é possível, mas os efeitos duplicados são controlados pelo desenho do consumidor.

---

## Modelo de dados

### `orders`

Guarda as encomendas criadas pela API.

| Coluna | Função |
|---|---|
| `id` | Identificador da encomenda |
| `customer_email` | Email do cliente |
| `amount_cents` | Valor em cêntimos |
| `currency` | Moeda da encomenda |
| `status` | Estado da encomenda |
| `created_at` | Data de criação |

### `outbox_events`

Guarda os eventos que ainda precisam ser publicados, ou que já foram publicados e ficam disponíveis para auditoria.

| Coluna | Função |
|---|---|
| `id` | Identificador do evento |
| `aggregate_type` | Tipo do agregado, neste caso `order` |
| `aggregate_id` | Identificador da encomenda |
| `event_type` | Tipo do evento, por exemplo `order.created.v1` |
| `routing_key` | Routing key usada no RabbitMQ |
| `payload` | JSON do evento |
| `published_at` | Data de publicação confirmada |
| `attempts` | Número de falhas de publicação |
| `last_error` | Último erro de publicação |
| `created_at` | Data de criação do evento |

### `processed_messages`

Guarda os eventos já processados pelo worker.

| Coluna | Função |
|---|---|
| `event_id` | Chave primária e chave de idempotência |
| `processed_at` | Data de processamento |

### `notification_log`

Guarda o resultado simulado do envio de notificação.

| Coluna | Função |
|---|---|
| `id` | Identificador do log |
| `order_id` | Encomenda associada |
| `event_id` | Evento processado |
| `recipient_email` | Destinatário |
| `status` | Estado da notificação simulada |
| `message` | Mensagem registada |
| `created_at` | Data do registo |

---

## Estrutura do projeto

```txt
02-event-driven-rabbitmq/
├── docker-compose.yml
├── README.md
├── orders-api/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/bmr/orders_api/
│       │   ├── api/
│       │   ├── domain/
│       │   ├── events/
│       │   ├── messaging/
│       │   ├── outbox/
│       │   └── service/
│       └── resources/
│           ├── application.properties
│           └── db/migration/V1__init.sql
└── notifications-worker/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/
        ├── java/com/bmr/notifications_worker/
        │   ├── consumer/
        │   ├── domain/
        │   ├── events/
        │   ├── messaging/
        │   └── service/
        └── resources/
            ├── application.properties
            └── db/migration/V1__init.sql
```

---

## Como executar

### 1. Subir os containers

O `docker-compose.yml` já define valores padrão para PostgreSQL e RabbitMQ. Por isso, o projeto pode ser iniciado diretamente:

```bash
docker compose up -d --build
```

Verificar o estado dos serviços:

```bash
docker compose ps
```

Ver logs:

```bash
docker compose logs -f orders-api notifications-worker
```

### 2. Aceder ao RabbitMQ Management

A interface de gestão fica disponível apenas no localhost:

```txt
http://localhost:15672
```

Credenciais padrão, caso não sejam sobrescritas por variáveis de ambiente:

```txt
Username: app_user
Password: app_password
```

### 3. Criar uma encomenda válida

```bash
curl -i -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerEmail": "miguel@example.com",
    "amountCents": 2500,
    "currency": "EUR"
  }'
```

Resposta esperada:

```json
{
  "orderId": "uuid-da-encomenda",
  "eventId": "uuid-do-evento",
  "status": "ACCEPTED"
}
```

O status HTTP esperado é `202 Accepted`.

---

## Testar retry e DLQ

Para forçar uma falha no worker, usar um email terminado em `@fail.local`:

```bash
curl -i -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerEmail": "teste@fail.local",
    "amountCents": 2500,
    "currency": "EUR"
  }'
```

A API aceita a encomenda, mas o worker falha ao processar a notificação. A mensagem entra no ciclo de retry:

```txt
fila principal -> fila de retry com TTL -> fila principal
```

Depois de atingir `MAX_RETRIES`, a mensagem é enviada para a DLQ:

```txt
orders.created.notifications.dlq
```

Ver logs do worker:

```bash
docker compose logs -f notifications-worker
```

---

## Consultar a base de dados

Abrir o `psql` dentro do container PostgreSQL:

```bash
docker compose exec postgres psql -U orders_user -d orders_db
```

Queries úteis:

```sql
SELECT *
FROM orders
ORDER BY created_at DESC;

SELECT id, aggregate_type, aggregate_id, event_type, routing_key,
       published_at, attempts, last_error, created_at
FROM outbox_events
ORDER BY created_at DESC;

SELECT *
FROM processed_messages
ORDER BY processed_at DESC;

SELECT *
FROM notification_log
ORDER BY created_at DESC;
```

---

## Escalar workers

É possível executar múltiplas instâncias do consumidor:

```bash
docker compose up -d --scale notifications-worker=3
```

A fila RabbitMQ distribui mensagens entre os workers. A tabela `processed_messages` protege contra processamento duplicado do mesmo `event_id`.

---

## Configurações principais

### `orders-api`

| Propriedade | Valor padrão | Função |
|---|---:|---|
| `app.outbox.batch-size` | `25` | Quantidade máxima de eventos lidos por ciclo |
| `app.outbox.publish-delay-ms` | `2000` | Intervalo entre ciclos do publisher |
| `app.outbox.confirm-timeout-ms` | `5000` | Timeout para Publisher Confirm |

### `notifications-worker`

| Variável / propriedade | Valor padrão | Função |
|---|---:|---|
| `MAX_RETRIES` / `app.worker.max-retries` | `3` | Número máximo de tentativas antes da DLQ |
| `spring.rabbitmq.listener.simple.acknowledge-mode` | `manual` | O worker confirma ou rejeita explicitamente a mensagem |
| `spring.rabbitmq.listener.simple.prefetch` | `10` | Quantidade de mensagens pré-carregadas por consumidor |

---

## Observações importantes

- A API responde `202 Accepted` porque a encomenda é aceite e o processamento posterior ocorre de forma assíncrona.
- O envio de notificação é apenas simulado; não existe integração com servidor de email real.
- A porta AMQP `5672` do RabbitMQ não é exposta no host; os serviços comunicam dentro da rede Docker.
- A UI do RabbitMQ é exposta apenas em `127.0.0.1:15672`.
- O PostgreSQL não é exposto no host; o acesso deve ser feito via `docker compose exec`.
- O schema é criado pela migração Flyway da `orders-api`. O worker usa validação de schema e não executa migrações em runtime.
- A tabela `outbox_events` mantém histórico dos eventos publicados; os registos não são apagados automaticamente.

---

## Limitações assumidas

Este é um microprojeto académico. Algumas decisões foram tomadas para simplificar a demonstração:

- os dois serviços usam a mesma base de dados PostgreSQL;
- o envio de notificação é simulado;
- não existe autenticação na API;
- não existe endpoint de consulta de encomendas;
- não existe limpeza automática da tabela `outbox_events`;
- não existe monitorização avançada para lag da outbox, filas RabbitMQ ou DLQ;
- a garantia de entrega é **At-Least-Once**, não **Exactly-Once**.

Num cenário de produção, cada microserviço deveria normalmente possuir a sua própria base de dados, métricas operacionais, tracing distribuído, alertas para DLQ, política de retenção da outbox e tratamento mais robusto de falhas permanentes.

---

## Comandos úteis

Parar os containers:

```bash
docker compose down
```

Parar e remover volumes:

```bash
docker compose down -v
```

Reconstruir as imagens:

```bash
docker compose build --no-cache
```

Ver logs de todos os serviços:

```bash
docker compose logs -f
```

---

## Conclusão

Este projeto demonstra uma arquitetura event-driven simples, mas realista, baseada em RabbitMQ e PostgreSQL.

O ponto central é o uso do **Transactional Outbox Pattern**, que garante que a criação da encomenda e o registo do evento ocorrem na mesma transação. A publicação é feita posteriormente por um scheduler, com Publisher Confirms e mensagens persistentes.

A solução segue o princípio **At-Least-Once Delivery**: o sistema é desenhado para evitar perda de mensagens, aceitando duplicações controladas. Por isso, o consumidor implementa idempotência através da tabela `processed_messages`.