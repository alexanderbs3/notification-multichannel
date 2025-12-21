# 🔔 Notification Multichannel System

> Sistema robusto de notificações multicanal (Email, SMS, Push) com padrão Transactional Outbox e arquitetura orientada a eventos usando RabbitMQ.

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.x-orange.svg)](https://www.rabbitmq.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Arquitetura](#-arquitetura)
- [Funcionalidades](#-funcionalidades)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Pré-requisitos](#-pré-requisitos)
- [Instalação e Configuração](#-instalação-e-configuração)
- [Uso da API](#-uso-da-api)
- [Fluxo de Execução](#-fluxo-de-execução)
- [Padrões Implementados](#-padrões-implementados)
- [Tratamento de Erros](#-tratamento-de-erros)
- [Monitoramento](#-monitoramento)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Contribuindo](#-contribuindo)

---

## 🎯 Visão Geral

O **Notification Multichannel System** é uma aplicação Spring Boot projetada para gerenciar o envio de notificações através de múltiplos canais de comunicação (Email, SMS, Push Notifications) de forma assíncrona, confiável e escalável.

### Principais Diferenciais

- **Transactional Outbox Pattern**: Garante consistência entre banco de dados e fila de mensagens
- **Idempotência**: Evita processamento duplicado de mensagens
- **Retry Automático**: Reprocessamento inteligente em caso de falhas temporárias
- **Dead Letter Queue (DLQ)**: Isolamento de mensagens com falhas permanentes
- **Strategy Pattern**: Extensibilidade para novos canais de notificação
- **Arquitetura em Camadas**: Separação clara de responsabilidades (API, Application, Domain, Infrastructure)

---

## 🏗️ Arquitetura

### Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT (REST API)                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    NOTIFICATION CONTROLLER                       │
│                   POST /v1/notifications                         │
│                   GET /v1/notifications/{id}                     │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    NOTIFICATION SERVICE                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ 1. Salva Notificação (CREATED)                           │   │
│  │ 2. Cria registro no Outbox (PENDING)                     │   │
│  │ 3. Commit da Transação (atomicidade garantida)           │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                         DATABASE (MySQL)                         │
│  ┌─────────────────────┐      ┌──────────────────────────┐      │
│  │   notifications     │      │  notification_outbox     │      │
│  │ ─────────────────── │      │ ────────────────────────  │      │
│  │ id (UUID)           │      │ id (UUID)                │      │
│  │ recipient           │      │ payload (JSON)           │      │
│  │ content             │      │ status (PENDING/SENT)    │      │
│  │ channel (ENUM)      │      │ destination (routing)    │      │
│  │ status (ENUM)       │      │ created_at               │      │
│  │ created_at          │      │                          │      │
│  └─────────────────────┘      └──────────────────────────┘      │
└────────────────────────────┬───────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      OUTBOX RELAY (Scheduler)                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ @Scheduled(fixedDelay = 5s)                              │   │
│  │ 1. Busca registros PENDING do Outbox                     │   │
│  │ 2. Publica no RabbitMQ (por routing key)                 │   │
│  │ 3. Atualiza status para SENT                             │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    RABBITMQ (Message Broker)                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │        notification.exchange (DirectExchange)            │   │
│  │                                                          │   │
│  │  Routing Key: EMAIL  ──────▶  email.queue               │   │
│  │  Routing Key: SMS    ──────▶  sms.queue (futuro)        │   │
│  │  Routing Key: PUSH   ──────▶  push.queue (futuro)       │   │
│  │                                                          │   │
│  │  email.queue ──(retry 3x)──▶ email.queue.dlq (DLQ)      │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    NOTIFICATION CONSUMER                         │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ @RabbitListener(queues = "email.queue")                  │   │
│  │ 1. Valida idempotência (já processado?)                  │   │
│  │ 2. Seleciona Strategy pelo canal                         │   │
│  │ 3. Executa envio (EmailNotificationStrategy)             │   │
│  │ 4. Atualiza status para PROCESSED                        │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   NOTIFICATION STRATEGIES                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │ Email Strategy   │  │  SMS Strategy    │  │ Push Strategy│  │
│  │ (implementado)   │  │ (implementado)   │  │  (stub)      │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Fluxo de Dados

1. **Request (Cliente → API)**: Cliente envia POST com dados da notificação
2. **Persistência Dual (API → DB)**: Service salva notificação + outbox (mesma transação)
3. **Relay (Scheduler → RabbitMQ)**: Job busca outbox PENDING e publica na fila
4. **Consumo (RabbitMQ → Consumer)**: Listener consome, valida idempotência e executa strategy
5. **Atualização (Consumer → DB)**: Status atualizado para PROCESSED

---

## ✨ Funcionalidades

### Implementadas

- ✅ Registro de notificações via API REST
- ✅ Suporte a múltiplos canais (EMAIL, SMS, PUSH)
- ✅ Padrão Transactional Outbox para consistência eventual
- ✅ Publicação assíncrona via RabbitMQ
- ✅ Retry automático (3 tentativas com backoff exponencial)
- ✅ Dead Letter Queue para mensagens falhadas
- ✅ Idempotência no processamento de mensagens
- ✅ Strategy Pattern para canais extensíveis
- ✅ Consulta de status de notificação por ID
- ✅ Tratamento centralizado de exceções
- ✅ Validação de entrada com Bean Validation

### Roadmap

- 🔲 Integração com provedores reais (SendGrid, Twilio, FCM)
- 🔲 Dashboard de monitoramento de notificações
- 🔲 Circuit Breaker para provedores externos
- 🔲 Rate Limiting por destinatário
- 🔲 Templates de notificação dinâmicos
- 🔲 Webhooks para notificações de status

---

## 🛠️ Tecnologias Utilizadas

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Java** | 17+ | Linguagem base |
| **Spring Boot** | 3.x | Framework principal |
| **Spring Data JPA** | 3.x | Abstração de persistência |
| **Hibernate** | 6.x | ORM (Object-Relational Mapping) |
| **MySQL** | 8.0.44 | Banco de dados relacional |
| **RabbitMQ** | 3.x | Message broker AMQP |
| **Spring AMQP** | 3.x | Integração com RabbitMQ |
| **Jackson** | 2.x | Serialização JSON |
| **Lombok** | 1.18+ | Redução de boilerplate |
| **Docker** | 20+ | Containerização de dependências |
| **Bean Validation** | 3.x | Validação de dados |

---

## 📦 Pré-requisitos

- **Java 17** ou superior
- **Maven 3.8+** (ou use o wrapper incluído `./mvnw`)
- **Docker** e **Docker Compose** (para MySQL e RabbitMQ)
- **Git** (para clonar o repositório)

---

## 🚀 Instalação e Configuração

### 1. Clone o Repositório

```bash
git clone https://github.com/seu-usuario/notification-multichannel.git
cd notification-multichannel
```

### 2. Suba as Dependências com Docker

```bash
docker-compose up -d
```

Isso irá iniciar:
- **MySQL** na porta `3306`
- **RabbitMQ** na porta `5672` (AMQP) e `15672` (Management UI)

**Acesse o RabbitMQ Management**: [http://localhost:15672](http://localhost:15672)
- Usuário: `guest`
- Senha: `guest`

### 3. Configure o Banco de Dados

O Hibernate criará as tabelas automaticamente na primeira execução (`ddl-auto: update`).

**Opcional**: Se preferir criar manualmente:

```sql
CREATE DATABASE IF NOT EXISTS multichannel;
USE multichannel;

CREATE TABLE notifications (
    id BINARY(16) PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification_outbox (
    id BINARY(16) PRIMARY KEY,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    destination VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 4. Compile e Execute a Aplicação

```bash
# Usando Maven Wrapper (recomendado)
./mvnw clean install
./mvnw spring-boot:run

# Ou usando Maven instalado
mvn clean install
mvn spring-boot:run
```

A aplicação estará disponível em: **http://localhost:8080**

---

## 📡 Uso da API

### Endpoints Disponíveis

#### 1. Criar Nova Notificação

**POST** `/v1/notifications`

**Request Body:**

```json
{
  "recipient": "usuario@example.com",
  "content": "Sua compra foi aprovada! Código: #12345",
  "channel": "EMAIL"
}
```

**Canais Disponíveis**: `EMAIL`, `SMS`, `PUSH`

**Response (202 Accepted):**

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "CREATED",
  "channel": "EMAIL"
}
```

**Exemplo com cURL:**

```bash
curl -X POST http://localhost:8080/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "teste@email.com",
    "content": "Bem-vindo ao sistema!",
    "channel": "EMAIL"
  }'
```

#### 2. Consultar Status da Notificação

**GET** `/v1/notifications/{id}`

**Response (200 OK):**

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "PROCESSED",
  "channel": "EMAIL"
}
```

**Possíveis Status**:
- `CREATED`: Notificação registrada, aguardando processamento
- `PROCESSED`: Notificação enviada com sucesso
- `FAILED`: Falha permanente no envio

**Exemplo com cURL:**

```bash
curl -X GET http://localhost:8080/v1/notifications/3fa85f64-5717-4562-b3fc-2c963f66afa6
```

### Exemplos de Uso

#### Envio de Email

```bash
curl -X POST http://localhost:8080/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "cliente@empresa.com",
    "content": "Seu pedido #5678 foi enviado e chegará em 3 dias úteis.",
    "channel": "EMAIL"
  }'
```

#### Envio de SMS

```bash
curl -X POST http://localhost:8080/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "+5511999998888",
    "content": "Código de verificação: 123456",
    "channel": "SMS"
  }'
```

---

## 🔄 Fluxo de Execução

### Fluxo Completo (Caso de Sucesso)

```
1. Cliente envia POST /v1/notifications
        ↓
2. NotificationController recebe requisição
        ↓
3. NotificationService (Transação iniciada)
   a. Salva Notification (status: CREATED)
   b. Cria NotificationOutbox (status: PENDING)
   c. Commit da transação (AMBOS persistidos atomicamente)
        ↓
4. Response 202 retornado ao cliente
        ↓
5. OutboxRelay (a cada 5 segundos)
   a. Busca registros PENDING no Outbox
   b. Publica no RabbitMQ (exchange: notification.exchange, routing: EMAIL)
   c. Atualiza Outbox para SENT
        ↓
6. RabbitMQ roteia para email.queue
        ↓
7. NotificationConsumer recebe mensagem
   a. Deserializa JSON para NotificationEventDTO
   b. Busca notificação no banco pelo ID
   c. Verifica idempotência (já processado?)
   d. Seleciona EmailNotificationStrategy
   e. Executa envio (log simulado)
   f. Atualiza status para PROCESSED
   g. ACK para RabbitMQ (mensagem removida da fila)
        ↓
8. Cliente pode consultar GET /v1/notifications/{id} e ver status: PROCESSED
```

### Fluxo com Falha Temporária (Retry)

```
1-6. [Mesmo fluxo até Consumer receber mensagem]
        ↓
7. NotificationConsumer tenta processar
   ❌ FALHA (ex: timeout na API externa)
        ↓
8. Exception lançada → RabbitMQ NÃO recebe ACK
        ↓
9. RabbitMQ aguarda 3 segundos (initial-interval)
        ↓
10. RETRY 1: Consumer tenta novamente
    ❌ FALHA novamente
        ↓
11. RabbitMQ aguarda 6 segundos (multiplier: 2.0)
        ↓
12. RETRY 2: Consumer tenta novamente
    ❌ FALHA novamente
        ↓
13. RabbitMQ aguarda 12 segundos
        ↓
14. RETRY 3 (último): Consumer tenta novamente
    ❌ FALHA definitiva (max-attempts: 3 atingido)
        ↓
15. Mensagem movida para email.queue.dlq (Dead Letter Queue)
        ↓
16. Status da notificação permanece CREATED (pode ser marcado como FAILED manualmente)
```

### Fluxo com Idempotência

```
1-7. [Consumer recebe mensagem e processa com sucesso]
        ↓
8. Status atualizado para PROCESSED
        ↓
9. RabbitMQ recebe ACK, mas por algum bug, mensagem é reprocessada
        ↓
10. Consumer recebe MESMA mensagem novamente
        ↓
11. Busca notificação no banco
        ↓
12. ✅ Verifica: status == PROCESSED
        ↓
13. Log: "Idempotência: Notificação já processada"
        ↓
14. return (não reprocessa)
        ↓
15. ACK enviado ao RabbitMQ
```

---

## 🎨 Padrões Implementados

### 1. Transactional Outbox Pattern

**Problema Resolvido**: Garantir consistência entre banco de dados e message broker (evitar situações onde o registro é salvo mas a mensagem não é publicada, ou vice-versa).

**Como Funciona**:
- Notificação e Outbox salvos na **mesma transação**
- Relay assíncrono lê o Outbox e publica no RabbitMQ
- Se RabbitMQ falhar, o Relay tenta novamente na próxima execução

**Código Relevante**:
```java
@Transactional
public NotificationResponseDTO createNotification(NotificationRequestDTO request) {
    // Salva notificação
    Notification saved = notificationRepository.save(notification);
    
    // Salva no outbox (mesma transação!)
    NotificationOutbox outbox = new NotificationOutbox();
    outbox.setStatus(OutboxStatus.PENDING);
    outboxRepository.save(outbox);
    
    // Se qualquer operação falhar, ambas são revertidas (rollback)
    return response;
}
```

### 2. Strategy Pattern

**Problema Resolvido**: Adicionar novos canais de notificação sem modificar código existente (Open/Closed Principle).

**Como Funciona**:
- Interface `NotificationStrategy` define contrato
- Cada canal (Email, SMS, Push) implementa sua própria estratégia
- Consumer seleciona dinamicamente a estratégia correta

**Código Relevante**:
```java
// Interface
public interface NotificationStrategy {
    NotificationChannel getChannel();
    void send(NotificationEventDTO message);
}

// Implementações
@Component
public class EmailNotificationStrategy implements NotificationStrategy {
    public NotificationChannel getChannel() { return NotificationChannel.EMAIL; }
    public void send(NotificationEventDTO message) { /* lógica de envio */ }
}

// Seleção dinâmica no Consumer
NotificationStrategy strategy = strategies.stream()
    .filter(s -> s.getChannel().equals(message.channel()))
    .findFirst()
    .orElseThrow();
```

### 3. Repository Pattern

**Problema Resolvido**: Abstrair a camada de persistência, facilitando testes e mudanças de tecnologia.

**Como Funciona**:
- Spring Data JPA gera implementações automaticamente
- Métodos customizados podem ser adicionados sem boilerplate

### 4. DTO Pattern

**Problema Resolvido**: Desacoplar representação da API da estrutura interna de domínio.

**Como Funciona**:
- `NotificationRequestDTO`: Entrada da API
- `NotificationEventDTO`: Payload para RabbitMQ
- `NotificationResponseDTO`: Saída da API

---

## ⚠️ Tratamento de Erros

### Camadas de Resiliência

#### 1. Validação de Entrada

```java
public record NotificationRequestDTO(
    @NotBlank String recipient,
    @NotBlank String content,
    @NotNull NotificationChannel channel
) {}
```

**Response 400 Bad Request:**
```json
{
  "validation_error": "content: não deve estar em branco"
}
```

#### 2. Exception Handler Global

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }
}
```

#### 3. Retry Automático (RabbitMQ)

Configurado em `application.yml`:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 3000ms
          multiplier: 2.0
```

**Backoff Exponencial**:
- Tentativa 1: imediata
- Tentativa 2: após 3s
- Tentativa 3: após 6s
- Tentativa 4: após 12s → **DLQ**

#### 4. Dead Letter Queue (DLQ)

Mensagens com falhas permanentes são movidas para `email.queue.dlq`.

**Monitoramento DLQ**:
```bash
# Via RabbitMQ Management UI
http://localhost:15672/#/queues/%2F/email.queue.dlq

# Via CLI
docker exec notimultichannel_rabbit rabbitmqctl list_queues name messages
```

#### 5. Idempotência

```java
if (notification.getStatus() == NotificationStatus.PROCESSED) {
    log.warn("Idempotência: Notificação {} já processada.", message.notificationId());
    return; // ACK sem reprocessar
}
```

---

## 📊 Monitoramento

### Logs Importantes

```bash
# Aplicação Spring Boot
tail -f logs/spring-boot-application.log

# Ver processamento do Outbox Relay
grep "Processando.*itens do outbox" logs/spring-boot-application.log

# Ver consumo de mensagens
grep "Mensagem recebida do broker" logs/spring-boot-application.log

# Ver idempotência em ação
grep "Idempotência" logs/spring-boot-application.log
```

### RabbitMQ Management UI

Acesse: [http://localhost:15672](http://localhost:15672)

**Métricas Importantes**:
- **Queues → email.queue**: Mensagens aguardando processamento
- **Queues → email.queue.dlq**: Mensagens com falhas permanentes
- **Exchanges → notification.exchange**: Taxa de publicação
- **Connections**: Conexões ativas da aplicação

### Queries Úteis no MySQL

```sql
-- Ver todas as notificações e seus status
SELECT id, recipient, channel, status, created_at 
FROM notifications 
ORDER BY created_at DESC 
LIMIT 10;

-- Ver itens pendentes no Outbox
SELECT id, destination, status, created_at 
FROM notification_outbox 
WHERE status = 'PENDING';

-- Contar notificações por status
SELECT status, COUNT(*) as total 
FROM notifications 
GROUP BY status;

-- Ver notificações falhadas
SELECT * FROM notifications WHERE status = 'FAILED';
```

---

## 📁 Estrutura do Projeto

```
notification-multichannel/
│
├── src/main/java/br/leetjourney/notificationmultichannel/
│   │
│   ├── api/                                    # Camada de Apresentação (Controllers, DTOs)
│   │   ├── controller/
│   │   │   └── NotificationController.java     # Endpoints REST
│   │   ├── dto/
│   │   │   ├── NotificationRequestDTO.java     # DTO de entrada (API)
│   │   │   ├── NotificationResponseDTO.java    # DTO de saída (API)
│   │   │   └── NotificationEventDTO.java       # DTO para RabbitMQ
│   │   └── exception/
│   │       └── GlobalExceptionHandler.java     # Tratamento centralizado de erros
│   │
│   ├── application/                            # Camada de Aplicação (Orquestração)
│   │   └── service/
│   │       └── NotificationService.java        # Lógica de negócio principal
│   │
│   ├── domain/                                 # Camada de Domínio (Entidades, Regras)
│   │   ├── entity/
│   │   │   ├── Notification.java               # Entidade principal
│   │   │   └── NotificationOutbox.java         # Tabela de outbox
│   │   ├── enums/
│   │   │   ├── NotificationChannel.java        # EMAIL, SMS, PUSH
│   │   │   ├── NotificationStatus.java         # CREATED, PROCESSED, FAILED
│   │   │   └── OutboxStatus.java               # PENDING, SENT, FAILED
│   │   └── repository/
│   │       ├── NotificationRepository.java     # Acesso a notifications
│   │       └── OutboxRepository.java           # Acesso ao outbox
│   │
│   ├── infrastructure/                         # Camada de Infraestrutura (Integrações)
│   │   ├── messaging/
│   │   │   ├── rabbitmq/
│   │   │   │   ├── RabbitMQConfig.java         # Configuração de filas/exchanges
│   │   │   │   └── NotificationPublisher.java  # Publicador no RabbitMQ
│   │   │   └── strategy/
│   │   │       ├── NotificationStrategy.java   # Interface do padrão Strategy
│   │   │       ├── EmailNotificationStrategy.java
│   │   │       └── SmsNotificationStrategy.java
│   │   └── scheduler/
│   │       └── OutboxRelay.java                # Job que publica outbox no RabbitMQ
│   │
│   └── consumers/
│       └── NotificationConsumer.java           # Listener do RabbitMQ
│
├── src/main/resources/
│   ├── application.yml                         # Configurações da aplicação
│   └── logback-spring.xml                      # (opcional) Config de logs
│
├── docker-compose.yml                          # MySQL + RabbitMQ
├── pom.xml                                     # Dependências Maven
└── README.md                                   # Este arquivo
```

### Responsabilidades por Camada

| Camada | Responsabilidade | Exemplos |
|--------|------------------|----------|
| **API** | Exposição de endpoints, validação de entrada, formatação de resposta | Controllers, DTOs, Exception Handlers |
| **Application** | Orquestração de casos de uso, transações, lógica de aplicação | NotificationService |
| **Domain** | Regras de negócio, entidades, contratos de repositório | Notification, Enums, Repository Interfaces |
| **Infrastructure** | Implementação de integrações externas (DB, MQ, APIs) | RabbitMQ Config, Strategies, Scheduler |
| **Consumers** | Consumo de mensagens assíncronas | RabbitMQ Listeners |

---

## 🤝 Contribuindo

Contribuições são bem-vindas! Siga os passos:

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade X'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

### Padrões de Código

- Siga as convenções do Java (camelCase para variáveis, PascalCase para classes)
- Use Lombok para reduzir boilerplate
- Adicione testes unitários para novos services
- Documente métodos públicos com Javadoc
- Commits em português, seguindo [Conventional Commits](https://www.conventionalcommits.org/pt-br)

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

## 👥 Autores

- **Alexander Costa** - *Desenvolvimento Inicial* - (https://github.com/alexanderbs3)

---

## 🙏 Agradecimentos

- Spring Boot Team pela excelente documentação
- RabbitMQ pela robustez do message broker
- Comunidade Java por bibliotecas open-source de qual