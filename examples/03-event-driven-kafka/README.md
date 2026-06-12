# 03-event-driven-kafka

Laboratório prático de arquitetura cloud-native orientada a eventos com Spring Boot, microserviços, Docker Compose, API Gateway e Apache Kafka.

Este exemplo é independente e demonstra um fluxo realista de encomendas, pagamentos e notificações usando comunicação assíncrona por eventos.

---

## 1. Objetivo

Tópicos:

- microserviços Spring Boot independentes;
- API Gateway como ponto único de entrada HTTP;
- comunicação REST síncrona para comandos e consultas externas;
- comunicação assíncrona entre serviços usando Kafka;
- eventos versionados;
- consumer groups;
- message keys e ordem por entidade de negócio;
- retries;
- Dead Letter Topic;
- idempotência básica no consumidor;
- observabilidade local com logs e Kafka UI.

---

## 2. Arquitetura

```text
Cliente
  |
  | HTTP POST /api/orders
  v
API Gateway :8080
  |
  v
Order Service :8081
  |
  | OrderCreatedEvent
  v
Kafka topic: order.created.v1
  |
  v
Payment Service :8082
  |
  | PaymentResultEvent
  v
Kafka topic: payment.result.v1
  |
  v
Order Service
  |
  | OrderStatusChangedEvent
  v
Kafka topic: order.status-changed.v1
  |
  v
Notification Service :8083
```

---

## 3. Serviços

| Serviço | Porta | Responsabilidade |
|---|---:|---|
| api-gateway | 8080 | Encaminha `/api/orders/**` para `order-service` |
| order-service | 8081 | Cria encomendas, publica eventos e atualiza estado |
| payment-service | 8082 | Processa pagamentos assincronamente |
| notification-service | 8083 | Simula notificações quando o estado muda |
| kafka-ui | 8090 | Inspeciona tópicos, mensagens e consumer groups |
| kafka | 9092/9094 | Broker Kafka em modo KRaft |

---

## 4. Tópicos Kafka

| Tópico | Produtor | Consumidor |
|---|---|---|
| `order.created.v1` | order-service | payment-service |
| `payment.result.v1` | payment-service | order-service |
| `order.status-changed.v1` | order-service | notification-service |
| `order.created.v1.DLT` | payment-service error handler | inspeção manual |

---

## 5. Requisitos locais

- Docker
- Docker Compose v2
- Porta `8080`, `8081`, `8082`, `8083`, `8090`, `9092` e `9094` livres

Não precisas de Maven nem Java instalados localmente para executar via Docker Compose. O build é feito dentro de containers Maven.

---

## 6. Executar

```bash
docker compose up --build
```

Kafka UI:

```text
http://localhost:8090
```

Gateway:

```text
http://localhost:8080
```

---

## 7. Testar fluxo aprovado

Criar encomenda com valor abaixo ou igual a 1000:

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
  "id": "...",
  "customerId": "customer-001",
  "amount": 49.90,
  "status": "PENDING_PAYMENT",
  "createdAt": "..."
}
```

Consultar estado:

```bash
curl http://localhost:8080/api/orders/{orderId}
```

Após o processamento assíncrono, o estado deve ser:

```json
{
  "status": "PAID"
}
```

---

## 8. Testar fluxo rejeitado

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-002",
    "amount": 1500.00
  }'
```

Resultado esperado depois do processamento:

```json
{
  "status": "REJECTED"
}
```

A regra está no `payment-service`:

```text
amount <= 1000.00 -> AUTHORIZED
amount > 1000.00  -> REJECTED
```

---

## 9. Testar Dead Letter Topic

Criar encomenda com `customerId = "fail"`:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "fail",
    "amount": 10.00
  }'
```

O `payment-service` lança uma exceção controlada para fins didáticos.

O evento será tentado novamente e, se continuar a falhar, será enviado para:

```text
order.created.v1.DLT
```

Verificar em:

```text
http://localhost:8090
```

---

## 10. Ver logs úteis

```bash
docker compose logs -f order-service payment-service notification-service
```

Também podes usar:

```bash
make logs
```

---

## 11. Escalar consumidores

Para estudar consumer groups:

```bash
docker compose up --build --scale payment-service=3
```

Observa no Kafka UI:

```text
Consumer Groups -> payment-service
```

Importante: para escalar realmente o processamento, o tópico precisa de várias partições. Neste laboratório os tópicos são criados com 3 partições.

---

## 12. Decisões arquiteturais

### REST para fronteira externa

O cliente chama REST para criar e consultar encomendas:

```text
POST /api/orders
GET /api/orders/{id}
```

### Eventos para comunicação entre serviços

Os serviços internos comunicam por factos de negócio:

```text
OrderCreatedEvent
PaymentResultEvent
OrderStatusChangedEvent
```

### Tópicos versionados

Os nomes dos tópicos incluem versão:

```text
order.created.v1
payment.result.v1
order.status-changed.v1
```

Isto permite criar `v2` sem quebrar consumidores existentes.

### Key por entidade de negócio

Todos os eventos usam:

```text
Kafka key = orderId
```

Isto ajuda a preservar a ordem relativa dos eventos da mesma encomenda, porque Kafka garante ordem dentro de uma partição.

### Idempotência

Os consumidores guardam `eventId` em memória depois de processar com sucesso para ignorar duplicados.

Em produção, isto deve ser persistido numa tabela, por exemplo:

```sql
CREATE TABLE processed_events (
  event_id UUID NOT NULL,
  consumer_name VARCHAR(120) NOT NULL,
  processed_at TIMESTAMP NOT NULL,
  PRIMARY KEY (event_id, consumer_name)
);
```

### Limitação deliberada

Este laboratório ainda não implementa Transactional Outbox.

Logo, existe uma janela de falha entre:

```text
1. guardar a encomenda
2. publicar OrderCreatedEvent
```

A evolução recomendada é criar o laboratório seguinte:

```text
04-transactional-outbox-debezium
```
