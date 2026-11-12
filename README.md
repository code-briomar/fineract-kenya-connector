# 🇰🇪 Fineract Kenya Connector

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Apache Fineract](https://img.shields.io/badge/Core_Banking-Apache_Fineract-00758F?style=flat-square)](https://fineract.apache.org/)
[![Africa's Talking](https://img.shields.io/badge/Telecom_API-Africa's_Talking-F58220?style=flat-square)](https://africastalking.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![Author](https://img.shields.io/badge/Author-code--briomar-181717?style=flat-square&logo=github)](https://github.com/code-briomar)

Hooks up M-Pesa Paybill and Africa's Talking (USSD + SMS) straight into Apache Fineract core banking.

When someone pays your Paybill or dials \*384# on a phone, it automatically updates their savings account in Fineract and fires an instant SMS receipt.

## The Architecture

```mermaid
sequenceDiagram
    autonumber
    actor Member as SACCO Member
    participant MPesa as Safaricom M-Pesa
    participant AT as Africa's Talking Gateway
    participant Connector as Kenya Connector (Spring Boot)
    participant Fineract as Apache Fineract (Core Banking)
    %% Scenario 1: M-Pesa C2B Paybill Flow
    rect rgb(240, 248, 255)
    Note over Member, Fineract: 1. M-Pesa C2B Paybill Deposit Flow
    Member->>MPesa: Paybill Deposit (e.g. KES 1,500 to Acc 001)
    MPesa->>Connector: POST /api/v1/mpesa/validation
    Connector-->>MPesa: 200 OK (ResultCode: 0)
    MPesa->>Connector: POST /api/v1/mpesa/confirmation
    Connector->>Fineract: POST /savingsaccounts/{id}/transactions (Deposit)
    Fineract-->>Connector: 200 OK (Ledger updated)
    Connector->>AT: Dispatch SMS confirmation
    AT-->>Member: SMS: "KES 1,500 received for Acc 001. Ref: RKT..."
    Connector-->>MPesa: 200 OK (ResultCode: 0)
    end
    %% Scenario 2: USSD Self-Service Flow
    rect rgb(255, 250, 240)
    Note over Member, Fineract: 2. USSD Session (*384# Feature Phone Banking)
    Member->>AT: Dials *384*100#
    AT->>Connector: POST /api/v1/ussd (text="")
    Connector-->>AT: CON 1. Balance 2. Mini-statement
    Member->>AT: Selects "1"
    AT->>Connector: POST /api/v1/ussd (text="1")
    Connector->>Fineract: GET /savingsaccounts/{id}
    Fineract-->>Connector: Returns real-time ledger balance
    Connector-->>AT: END Chama Balance: KES 15,250.00
    AT-->>Member: Displays balance screen
    end
```

---

## Key Features

- **Safaricom C2B Webhook Handlers:** Instant `validation` and `confirmation` lifecycle handling with standard Safaricom JSON responses.
- **Apache Fineract Ledger Integration:** Maps mobile deposits directly into member savings accounts using Fineract's authenticated REST APIs.
- **Africa's Talking SMS Automation:** Sends immediate automated transaction receipts to members upon confirmed core banking ledger posting.
- **Interactive USSD Menu (`*384#`):** Low-bandwidth feature phone banking supporting self-service balance checks and mini-statements using the `CON`/`END` protocol.
- **Plug-and-Play Local Core Banking:** Pre-configured Docker Compose environment for Apache Fineract + PostgreSQL with automatic schema provisioning.

---

## Tech Stack

| Layer            | Technology                        |
| ---------------- | --------------------------------- |
| **Framework**    | Spring Boot 3.3.4 (Java 17)       |
| **Core Banking** | Apache Fineract (Containerized)   |
| **Database**     | PostgreSQL 15                     |
| **Telecom APIs** | Africa's Talking Java SDK 3.5.4   |
| **Mobile Money** | Safaricom Daraja C2B OpenAPI Spec |

---

## Quickstart

### 1. Clone & Set Up

```bash
git clone https://github.com/code-briomar/fineract-kenya-connector.git
cd fineract-kenya-connector
```

### 2. Launch Core Banking Engine

Start PostgreSQL and Apache Fineract:

```bash
docker compose up -d
```

_(Fineract takes ~1-2 minutes on first boot to run Liquibase migrations. Check logs with `docker logs -f fineract-server`)._

### 3. Run the Connector

```bash
./gradlew bootRun
```

The connector boots up at `http://localhost:8080`.

---

## Testing the APIs

### 1. Simulate an M-Pesa Paybill Deposit

Send a mock Safaricom confirmation webhook:

```powershell
$body = @{
    TransactionType   = "Pay Bill"
    TransID           = "RKT978HH7K"
    TransTime         = "20261112195700"
    TransAmount       = "1500.00"
    BusinessShortCode = "600000"
    BillRefNumber     = "ACC-CHAMA-001"
    MSISDN            = "254712345678"
    FirstName         = "Briane"
} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/mpesa/confirmation" -Method Post -Body $body -ContentType "application/json"
```

### 2. Simulate USSD Dialing (`*384#`)

Dial the USSD gateway:

```powershell
# Initial Menu Dial
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/ussd" -Method Post -Body "sessionId=s1&serviceCode=*384*100#&phoneNumber=+254712345678&text=" -ContentType "application/x-www-form-urlencoded"
# Select 1: Check Balance
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/ussd" -Method Post -Body "sessionId=s1&serviceCode=*384*100#&phoneNumber=+254712345678&text=1" -ContentType "application/x-www-form-urlencoded"
```

---

## Project Structure

```text
src/main/java/com/kenyafintech/connector/
├── controller/
│   ├── MpesaWebhookController.java    # Handles C2B Validation & Confirmation
│   └── UssdController.java            # Africa's Talking USSD session state
├── model/
│   └── MpesaC2BPayload.java           # Safaricom payload mapping
├── service/
│   ├── FineractClient.java            # Fineract REST client & SSL setup
│   └── NotificationService.java       # Africa's Talking SMS integration
└── ConnectorApplication.java          # Spring Boot Main
```

---

## 📄 License

MIT License. Feel free to fork and build on top of this!
