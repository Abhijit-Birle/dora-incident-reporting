# DORA Auditor — Incident Service (`incident-svc`)

> **Automated ICT Incident Classification & Statutory Deadline Tracking Service**  
> *Deterministic rules engine and state machine aligned with EU DORA and APRA CPS 230.*

---

## Overview

**`incident-svc`** is a backend service designed to automate major ICT incident classification and statutory deadline tracking for financial entities under:
* **EU DORA (Regulation (EU) 2022/2554)** — Articles 17 to 23
* **Commission Delegated Regulation (EU) 2024/1772** (Criteria & Thresholds for Major Incident Classification)
* **Implementing Standards (EU 2025/301 & EU 2025/302)** (Staged Regulatory Notification Timelines)
* **Australian APRA CPS 230** (Operational Risk Management)

The service enforces strict deterministic execution for legal classification thresholds and coordinates incident lifecycle transitions using an orchestrated finite state machine.

---

## Key Capabilities

* **RTS (EU 2024/1772) Rules Engine**:
  - Deterministic evaluation across the 7 regulatory criteria (critical services, clients/transactions affected, economic impact, duration, data integrity, reputational impact, geographic spread).
* **Orchestrated Finite State Machine**:
  - Linear lifecycle transitions (`RECEIVED` → `CLASSIFIED` → `DRAFTING` → `HITL_PENDING` → `APPROVED` → `SUBMITTED` → `RESOLVED` → `CLOSED`).
  - JPA `@Version` optimistic concurrency control to prevent state anomalies across concurrent workers and human reviews.
* **Dual-Clock Statutory Deadline Engine**:
  - Simultaneously tracks the **4-hour initial notification clock** (from official classification) and the **24-hour awareness window** (from initial detection/awareness).
* **Idempotent Ingestion**:
  - HTTP `Idempotency-Key` header validation with SHA-256 request payload hashing to safely handle network retries and duplicate alerts.

---

## Project Structure

```
dora-auditor/
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

## Current Status

- [x] **Service Foundation**: Java 21, Spring Boot 3.x, Spring Data JPA, and Flyway migration setup.
- [x] **Database Schema**: Flyway migration (`V1__init.sql`) establishing:
  - `incident.incident` with statutory deadline tracking (`awareness_at`, `classified_at`, `classification_deadline`) and `@Version` concurrency support.
  - `incident.idempotency_key` for deterministic request deduplication.
- [x] **Local Infrastructure**: Docker Compose container configuration for PostgreSQL 16.
- [x] **RTS (EU 2024/1772) Rules Engine**: Specification pattern evaluating all 7 criteria and Art. 9 major incident formula with JUnit 5 boundary tests.
- [x] **Finite State Machine & Concurrency**: JPA `@Version` optimistic locking and transition matrix (`IncidentStatus`).
- [x] **Statutory Dual-Clock Deadlines**: Simultaneous tracking of 4h classification timer and 24h awareness window (`min(clock1, clock2)`).
- [x] **Idempotent Ingestion**: HTTP `Idempotency-Key` validation with SHA-256 payload hashing, conflict detection (HTTP 422), and cached response replay.

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

* **Language**: Java 21 (Virtual Threads)
* **Framework**: Spring Boot 3.x, Spring Data JPA, Flyway
* **Database**: PostgreSQL 16
* **Testing**: Testcontainers, JUnit 5

---

## License

Apache-2.0
