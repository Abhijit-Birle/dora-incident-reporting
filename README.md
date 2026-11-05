# DORA Auditor

> **Automated ICT Incident Classification & Regulatory Platform**  
> *Deterministic rules decide. Advisory AI drafts. Humans approve. The cryptographic audit log proves it.*

---

## Overview

**DORA Auditor** is an event-driven system being developed to streamline major ICT incident classification, statutory deadline tracking, and compliance reporting in accordance with:
* **EU DORA (Regulation (EU) 2022/2554)** — Articles 17 to 23
* **Commission Delegated Regulation (EU) 2024/1772** (Major Incident Classification Criteria)
* **Implementing Standards (EU 2025/301 & EU 2025/302)** (Staged Regulatory Reporting)
* **Australian APRA CPS 230** (Operational Risk Management)

The platform enforces a strict boundary between **deterministic compliance logic** (Java) and **probabilistic advisory AI** (Python / LangGraph), ensuring regulatory decisions remain 100% deterministic, audit-traceable, and verifiable.

---

## Project Status & Roadmap

```
[Phase 1: In Progress] ──────► [Phase 2: Planned] ──────► [Phase 3: Planned]
incident-svc (Java 21)         compliance-svc (Audit)     agent-svc (AI / HITL)
```

### Current Status (Phase 1 — Core Backend Foundation)
- [x] **Architecture Specification**: Complete microservices design, event flows, state matrices, and threat models documented in [design/ARCHITECTURE.md](design/ARCHITECTURE.md).
- [x] **Service Scaffolding**: Java 21, Spring Boot 3.x, Spring Data JPA, and Flyway migration setup.
- [x] **Database Schema**: Initial Flyway migration (`V1__init.sql`) establishing:
  - `incident.incident` with statutory deadline tracking (`awareness_at`, `classified_at`, `classification_deadline`) and `@Version` concurrency support.
  - `incident.idempotency_key` for deterministic request deduplication.
- [x] **Local Infrastructure**: Docker Compose container configuration for PostgreSQL 16.
- [ ] **In Active Development**:
  - RTS (EU 2024/1772) deterministic 7-criteria rules engine
  - Orchestrated Finite State Machine (`@Version` optimistic locking)
  - Statutory 4h/24h dual-clock countdown engine
  - Idempotent REST API intake (`POST /api/v1/incidents`)

### Future Phases
* **Phase 2 — `compliance-svc`**: Single-writer SHA-256 tamper-evident hash-chained audit ledger, periodic verifier worker, and GDPR Art. 17 crypto-shredding via KMS envelope encryption.
* **Phase 3 — `agent-svc`**: Stateful LangGraph workflow with deterministic PII sanitization, RAG legal citation matching, and human-in-the-loop (`interrupt()`) approval gates.
* **Phase 4 — Integration & Resilience**: Mock NCA (BaFin/EBA) supervisory endpoint integration with Resilience4j circuit breaking and chaos fault injection.

---

## System Architecture (Design Preview)

```mermaid
flowchart TD
    subgraph Intake ["Intake"]
        Alerts["Monitoring / SIEM Alerts"]
    end

    subgraph Service1 ["1. incident-svc (In Progress)"]
        RESTIntake["REST Ingestion API\n(HTTP Idempotency-Key)"]
        RulesEngine["RTS 2024/1772 Rules Engine\n(Deterministic Evaluation)"]
        FSM["Finite State Machine\n(@Version Optimistic Locking)"]
        Deadlines["Dual-Clock Deadline Engine\n(4h / 24h Timers)"]
    end

    subgraph PlannedServices ["Planned Architecture"]
        Compliance["compliance-svc\n(SHA-256 Hash Chain & Crypto-Shredder)"]
        Agent["agent-svc\n(LangGraph AI Copilot & HITL Gate)"]
        NCA["Supervisory Gateway\n(Mock BaFin / EBA)"]
    end

    Alerts --> RESTIntake
    RESTIntake --> FSM
    FSM --> RulesEngine
    FSM --> Deadlines
    FSM -.->|Events (Kafka)| Compliance
    FSM -.->|Draft Request| Agent
    Agent -.->|Approved Draft| FSM
    FSM -.->|Resilience4j| NCA
```

Full architecture specifications, database schemas, and event contracts are documented in [design/ARCHITECTURE.md](design/ARCHITECTURE.md).

---

## Current Repository Layout

```
dora-auditor/
├── design/                   # Microservices architecture & system design specs
│   └── ARCHITECTURE.md
├── incident-svc/             # Core incident service (Java 21 / Spring Boot 3)
│   ├── src/main/java/        # Application source code
│   └── src/main/resources/
│       ├── application.yaml  # Service configuration
│       └── db/migration/     # Flyway SQL migrations (V1__init.sql)
├── deploy/                   # Local infrastructure
│   └── docker-compose.yml    # PostgreSQL 16 container definition
└── README.md
```

---

## Getting Started (Local Development)

### Prerequisites
* **Java 21** (e.g. Eclipse Temurin / OpenJDK 21)
* **Docker** & **Docker Compose**
* **Maven** (or use included `./mvnw`)

### 1. Start Local Database
```bash
cd deploy
docker compose up -d
```
PostgreSQL will start on port `5432` with database `dora_db`.

### 2. Build & Run `incident-svc`
```bash
cd ../incident-svc
./mvnw clean compile
./mvnw spring-boot:run
```
Flyway will automatically apply migrations against the local PostgreSQL instance.

---

## Tech Stack

* **Core Platform**: Java 21, Spring Boot 3.x, Spring Data JPA, Flyway, PostgreSQL 16
* **Architecture & Standards**: DORA (EU 2022/2554), RTS (EU 2024/1772), APRA CPS 230
* **Testing & Quality**: Testcontainers, JUnit 5, Resilience4j (in progress)

---

## License

Apache-2.0
