# 01 - REST Docker Compose VPS

Projeto demonstrativo de uma arquitetura baseada em **microserviços REST**, utilizando **Spring Boot**, **Docker Compose** e **Nginx** como ponto único de entrada HTTP.

O objetivo é demonstrar como vários serviços independentes podem ser executados em containers distintos, comunicando entre si através de uma rede interna Docker.

---

## 1. Arquitetura

A aplicação é composta por três containers:

| Serviço           | Tecnologia            | Responsabilidade                                   | Porta          |
| ----------------- | --------------------- | -------------------------------------------------- | -------------- |
| `gateway`         | Nginx                 | Entrada única HTTP e encaminhamento de rotas       | `80`           |
| `product-service` | Spring Boot / Java 21 | API de produtos                                    | `8081` interno |
| `order-service`   | Spring Boot / Java 21 | API de encomendas e chamada ao serviço de produtos | `8082` interno |

---

## 2. Arquitetura física

A solução utiliza:

* 3 containers Docker;
* 1 rede Docker Compose;
* DNS interno por nome de serviço;
* health checks para controlo de readiness;
* Nginx como reverse proxy.

Representação simplificada:

```text
Cliente
   |
   v
+----------------+
| Nginx Gateway  |
| Porta 80       |
+----------------+
   |          |
   |          |
   v          v
/products   /orders
   |          |
   v          v
+-------------------+        REST interno        +-------------------+
| product-service   | <------------------------- | order-service     |
| Spring Boot       |                            | Spring Boot       |
| Porta 8081        |                            | Porta 8082        |
+-------------------+                            +-------------------+
```

O cliente externo comunica apenas com o `gateway`. Os serviços internos comunicam entre si através da rede Docker Compose.

---

## 3. Serviços

### 3.1. `product-service`

Serviço responsável por expor uma API REST de produtos.

Endpoints principais:

```http
GET /products
GET /products/{id}
```

Exemplo de resposta:

```json
{
  "id": 1,
  "name": "Laptop",
  "price": 899.99
}
```

A informação dos produtos é mantida em memória, simulando uma fonte de dados.

---

### 3.2. `order-service`

Serviço responsável por expor uma API REST de encomendas.

Endpoint principal:

```http
GET /orders/{id}
```

Ao consultar uma encomenda, o `order-service` chama internamente o `product-service` para obter informação do produto associado.

Exemplo de resposta:

```json
{
  "orderId": 1,
  "status": "CREATED",
  "createdAt": "2026-06-15T10:00:00Z",
  "product": {
    "id": 1,
    "name": "Laptop",
    "price": 899.99
  }
}
```

---

### 3.3. `gateway`

O `gateway` é implementado com Nginx e funciona como ponto único de entrada HTTP.

Responsabilidades:

* receber pedidos externos;
* encaminhar `/products` para o `product-service`;
* encaminhar `/orders` para o `order-service`;
* esconder a topologia interna dos microserviços.

Rotas configuradas:

```text
/products -> product-service:8081
/orders   -> order-service:8082
```

---

## 4. Comunicação entre serviços

A comunicação entre os containers ocorre através da rede interna criada automaticamente pelo Docker Compose.

O Docker Compose disponibiliza DNS interno, permitindo que um serviço aceda a outro pelo nome definido no `docker-compose.yml`.

Exemplo:

```text
http://product-service:8081
```

Assim, o `order-service` consegue chamar o `product-service` sem conhecer o endereço IP do container.

---

## 5. Health checks

Os serviços Spring Boot expõem endpoints de saúde através do Spring Boot Actuator.

Exemplo:

```http
/actuator/health/readiness
```

O Docker Compose utiliza estes endpoints para verificar se os serviços estão prontos antes de iniciar serviços dependentes.

Exemplo conceptual:

```yaml
depends_on:
  product-service:
    condition: service_healthy
```

Isto garante que o `order-service` apenas é iniciado depois de o `product-service` estar em estado saudável.

---

## 6. Requisitos

Para executar o projeto, é necessário ter instalado:

* Docker;
* Docker Compose.

Verificar instalação:

```bash
docker --version
docker compose version
```

---

## 7. Como executar

Na raiz do projeto, executar:

```bash
docker compose up --build
```

Este comando irá:

1. construir as imagens dos serviços Spring Boot;
2. iniciar o `product-service`;
3. iniciar o `order-service`;
4. iniciar o `gateway`;
5. disponibilizar a aplicação através da porta `80`.

---

## 8. Como testar

### Listar produtos

```bash
curl http://localhost/products
```

### Consultar produto por ID

```bash
curl http://localhost/products/1
```

### Consultar encomenda por ID

```bash
curl http://localhost/orders/1
```

---

## 9. Parar a aplicação

Para parar os containers:

```bash
docker compose down
```

Para parar e remover volumes associados:

```bash
docker compose down -v
```

---

## 10. Estrutura do projeto

```text
.
├── docker-compose.yml
├── gateway
│   └── nginx.conf
├── product-service
│   ├── Dockerfile
│   ├── pom.xml
│   └── src
├── order-service
│   ├── Dockerfile
│   ├── pom.xml
│   └── src
└── README.md
```

---

## 11. Docker Compose

O `docker-compose.yml` é responsável por orquestrar os três serviços.

Principais responsabilidades:

* construir os microserviços Spring Boot;
* criar a rede interna entre containers;
* configurar variáveis de ambiente;
* aplicar health checks;
* expor apenas o gateway para o exterior.

O `product-service` e o `order-service` usam portas internas. O acesso externo é feito através do `gateway`.

---

## 12. Dockerfiles

Cada microserviço Spring Boot utiliza um Dockerfile com multi-stage build.

A primeira fase usa Maven e JDK para compilar a aplicação.

A segunda fase usa apenas JRE para executar o `.jar` gerado.

Esta abordagem reduz o tamanho da imagem final e evita incluir ferramentas de build no container de runtime.

---

## 13. Conclusão

Este projeto demonstra uma arquitetura simples de microserviços REST com Spring Boot, Docker Compose e Nginx.

A arquitetura permite compreender conceitos fundamentais como:

* separação de responsabilidades;
* comunicação HTTP entre serviços;
* gateway como ponto único de entrada;
* rede interna Docker;
* DNS interno por nome de serviço;
* health checks para readiness;
* empacotamento de aplicações com Docker.

É uma base adequada para aprendizagem e evolução para cenários mais robustos.
