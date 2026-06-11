# 02-event-driven-rabbitmq-spring-boot

Exemplo prático de arquitetura event-driven com Java, Spring Boot, RabbitMQ, PostgreSQL e Docker Compose.

Este projeto continua o padrão do `01-rest-docker-compose-vps`, mas acrescenta processamento assíncrono:

- `orders-api`: API REST para criar encomendas.
- `outbox_events`: tabela transacional para guardar eventos antes de publicar.
- `RabbitMQ`: broker de mensagens.
- `notifications-worker`: consumidor assíncrono que simula envio de notificação.
- `retry queue`: retry com atraso de 10 segundos.
- `DLQ`: fila final para mensagens que falharam depois do limite de retries.

## Arquitetura

```txt
Cliente
  |
  | POST /orders
  v
orders-api
  |
  | mesma transação SQL
  | grava orders + outbox_events
  v
PostgreSQL
  |
  | scheduler publica eventos pendentes
  v
RabbitMQ exchange: orders.events
  |
  v
queue: orders.created.notifications.q
  |
  v
notifications-worker
  |
  | grava processed_messages + notification_log
  v
PostgreSQL
```

## Requisitos

- Docker
- Docker Compose

## Arranque

```bash
cp .env.example .env
docker compose up -d --build
```

## Criar uma order válida

```bash
curl -X POST http://localhost:8080/orders \
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
  "orderId": "...",
  "eventId": "...",
  "status": "ACCEPTED"
}
```

## Testar retry e DLQ

Este email força falha no worker:

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerEmail": "test@fail.local",
    "amountCents": 2500,
    "currency": "EUR"
  }'
```

Ver logs:

```bash
docker compose logs -f notifications-worker
```

Depois de `MAX_RETRIES`, a mensagem vai para:

```txt
orders.created.notifications.dlq
```

## RabbitMQ Management UI

A porta de management está vinculada só a `127.0.0.1`.

Local:

```txt
http://localhost:15672
```

Na VPS:

```bash
ssh -L 15672:localhost:15672 user@your-vps-ip
```

Credenciais vêm do `.env`.

## Consultar PostgreSQL

```bash
docker compose exec postgres psql -U orders_user -d orders_db
```

Queries úteis:

```sql
SELECT * FROM orders ORDER BY created_at DESC;

SELECT id, event_type, routing_key, published_at, attempts, last_error, created_at
FROM outbox_events
ORDER BY created_at DESC;

SELECT * FROM processed_messages ORDER BY processed_at DESC;

SELECT * FROM notification_log ORDER BY created_at DESC;
```

## Escalar workers

```bash
docker compose up -d --scale notifications-worker=3
```

## Notas de produção

- Não expor `5672` publicamente.
- Não expor `15672` publicamente; usar SSH tunnel.
- Trocar passwords do `.env`.
- Monitorizar:
  - tamanho da main queue;
  - tamanho da retry queue;
  - tamanho da DLQ;
  - número de eventos pendentes em `outbox_events`;
  - falhas de publisher confirm.
- Para throughput alto, separar reserva de eventos outbox e publish em batch.
