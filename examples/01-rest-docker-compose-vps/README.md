@'
# 01 - REST Docker Compose VPS

Exemplo de microserviços Spring Boot com comunicação REST, Docker Compose e Nginx como reverse proxy/API Gateway básico.

## Serviços

| Serviço | Descrição | Porta |
|---|---|---|
| product-service | API de produtos | 8081 |
| order-service | API de encomendas | 8082 |
| gateway | Nginx reverse proxy | 80 |

## Executar

```bash
docker compose up --build