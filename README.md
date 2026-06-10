# Cloud Native Spring Lab

Laboratório prático com exemplos de arquitetura cloud-native usando Spring Boot, microserviços, Docker, Docker Compose, API Gateway, comunicação REST, arquiteturas orientadas a eventos, frontend e deployment em VPS ou Kubernetes.

## Objetivo

Este repositório organiza vários exemplos progressivos de sistemas distribuídos e aplicações cloud-native. Cada exemplo é independente e demonstra um conjunto específico de conceitos técnicos.

## Exemplos

| Exemplo                      | Descrição                                                                      | Tecnologias                |
| ---------------------------- | ------------------------------------------------------------------------------ | -------------------------- |
| `01-rest-docker-compose-vps` | Microserviços Spring Boot com comunicação REST, Nginx Gateway e Docker Compose | Spring Boot, Docker, Nginx |
| `02-event-driven-rabbitmq`   | Exemplo futuro com comunicação assíncrona via RabbitMQ                         | Spring Boot, RabbitMQ      |
| `03-event-driven-kafka`      | Exemplo futuro com eventos distribuídos via Kafka                              | Spring Boot, Kafka         |
| `04-fullstack-react-spring`  | Exemplo futuro com frontend integrado com microserviços                        | React, Spring Boot         |
| `05-kubernetes-deployment`   | Exemplo futuro com deployment em Kubernetes                                    | Kubernetes, Docker         |

## Estrutura do repositório

```text
cloud-native-spring-lab/
├── examples/
│   └── 01-rest-docker-compose-vps/
│       ├── product-service/
│       ├── order-service/
│       ├── gateway/
│       ├── docker-compose.yml
│       └── k8s/
├── docs/
├── .github/
│   └── workflows/
│       └── ci.yml
├── README.md
└── .gitignore
```

## Exemplo atual: `01-rest-docker-compose-vps`

Este exemplo demonstra uma arquitetura de microserviços simplificada com dois serviços Spring Boot e um gateway Nginx.

### Componentes

| Componente        | Responsabilidade                                     | Porta interna |
| ----------------- | ---------------------------------------------------- | ------------- |
| `product-service` | Expõe produtos através de uma API REST               | `8081`        |
| `order-service`   | Expõe encomendas e consulta o `product-service`      | `8082`        |
| `gateway`         | Encaminha pedidos externos para os serviços internos | `80`          |

### Arquitetura

```text
Client
  |
  v
Nginx Gateway :80
  |----------------------|
  v                      v
product-service :8081    order-service :8082
                          |
                          v
                    product-service
```

## Executar o primeiro exemplo

Entrar na pasta do exemplo:

```bash
cd examples/01-rest-docker-compose-vps
```

Subir os containers:

```bash
docker compose up --build
```

Testar os endpoints:

```bash
curl http://localhost/products
curl http://localhost/orders/1
```

Parar:

```bash
docker compose down
```

## Executar numa VPS

Copiar o projeto para a VPS:

```bash
scp -r cloud-native-spring-lab root@IP_DA_VPS:/root/
```

Entrar na VPS:

```bash
ssh root@IP_DA_VPS
```

Executar:

```bash
cd /root/cloud-native-spring-lab/examples/01-rest-docker-compose-vps
docker compose up --build -d
```

Testar:

```bash
curl http://localhost/products
curl http://localhost/orders/1
```

No browser:

```text
http://IP_DA_VPS/products
http://IP_DA_VPS/orders/1
```

## Nota de segurança

Este repositório é um laboratório técnico. Para produção seriam necessários, pelo menos:

* HTTPS
* autenticação e autorização
* gestão segura de secrets
* bases de dados persistentes por serviço
* observabilidade
* health checks
* logs estruturados
* backups
* hardening da VPS
* CI/CD completo

## Licença

Projeto criado para fins de estudo e demonstração técnica.
