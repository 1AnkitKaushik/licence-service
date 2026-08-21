# License Service Management Platform

A Spring Boot backend application built to manage software licenses and tell a Kubernetes (K8s) cluster when to run or stop services based on their license status.

---

## 📌 What This Project Does

This project covers all the core requirements for managing service licenses:

1. **Tracks Service Licenses & 15-Day Grace Period**
   - Keeps track of each service, its license policy, and its current state.
   - If a license expires, the service gets a **15-day grace period** with a warning (`GRACE_PERIOD` / `WARN`).
   - If the license remains expired after 15 days, the system marks it for full shutdown (`EXPIRED` / `SHUTDOWN`).

2. **Saves Data to a File System Table**
   - Saves all service license data in a local JSON file located at `data/registry/service_registry.json`.
   - Stores fields like `serviceId`, `serviceName`, `licenseKey`, `policy`, `state`, `licenseState`, and `action`.

3. **Database Integration & Sync Engine**
   - Ingests licenses directly from MongoDB and synchronizes evaluation states across the file cache.
   - Provides on-demand sync endpoints alongside scheduled evaluation triggers.

4. **Runs an Hourly Background Check (Scheduler)**
   - Includes a background process (`LicenseScheduler.java`) that runs **every hour**.
   - It scans license states, evaluates active grace periods, updates status flags, and saves the changes to the persistent registry file.

5. **Provides an Endpoint for Kubernetes**
   - Exposes an API endpoint (`GET /api/v1/license/registry`) so Kubernetes cluster controllers can read the status and take action (`NONE`, `WARN`, `SHUTDOWN`).
   - Returns data as a JSON list:
[
  {
    "serviceId": "6a390254c1a4666cd69fb74f",
    "serviceName": "user1",
    "status": "EXPIRED",
    "action": "SHUTDOWN"
  }
]

6. **Easy Configuration**
   - All main settings (port number, database configurations, file paths, grace period length, and schedule time) are kept in `src/main/resources/application.properties`.

---

## 🛠️ Recent Developments & Enhancements (Past Week)

Key engineering updates and architectural implementations completed:

* **MongoDB to File-Cache Synchronization Pipeline**: Integrated Spring Data `LicenseRepository` with `LicenseRegistryService` to evaluate real MongoDB records and write structured JSON snapshots to `data/registry/service_registry.json`.
* **Dynamic License & Grace Period Evaluation**: Implemented business rules calculating whether a license is active, within its 15-day grace window, or expired, directly mapping these states to K8s container actions (`NONE`, `WARN`, `SHUTDOWN`).
* **Dedicated Kubernetes Polling Endpoint**: Designed `LicenseRegistryController` and `K8sServiceActionResponse` to deliver high-throughput, unauthenticated status feeds specifically for microservice sidecars.
* **On-Demand Manual Sync API**: Created the `POST /api/v1/license/registry/sync` endpoint allowing immediate re-evaluation and cache updates without waiting for the cron job.
* **Security & Auth Whitelisting**: Adjusted Spring Security (`SecurityConfig.java`) to maintain application-level password encoder and authentication manager beans while whitelisting `/api/v1/license/**` routes.
* **Build & Data Consistency Cleanup**: Removed background seed conflicts (`DataSeeder.java`), aligned constructor and DTO signatures, and resolved character encoding handling for Windows terminals.

---

## 📁 Project Folder Structure

src/main/java/
├── com/licensemanager/                  # Core Authentication & MongoDB Layer
│   ├── config/
│   │   ├── CorsConfig.java              # CORS filters
│   │   └── SecurityConfig.java          # Spring Security & route whitelisting
│   ├── controller/                      # Authentication & user controllers
│   ├── model/                           # License entity definitions
│   └── repository/                      # MongoDB repositories
└── com/licenseservice/                  # Service Registry & K8s Control Plane
    ├── controller/
    │   └── LicenseRegistryController.java # API endpoints for K8s & Sync
    ├── dto/
    │   └── K8sServiceActionResponse.java  # Data structure for K8s responses
    ├── model/
    │   ├── ActionRequired.java          # Enum: NONE, WARN, SHUTDOWN
    │   ├── LicenseState.java            # Enum: ACTIVE, GRACE_PERIOD, EXPIRED
    │   ├── ServiceRegistryEntry.java    # Main record model
    │   └── ServiceState.java            # Enum: RUNNING, DEGRADED, SHUTDOWN, UNAVAILABLE
    ├── repository/
    │   └── FileRegistryRepository.java  # Handles reading and writing to the JSON file
    ├── scheduler/
    │   └── LicenseScheduler.java        # Runs background checks every hour
    └── service/
        └── LicenseRegistryService.java  # Business logic for license checks and grace periods

---

## ⚙️ Configuration (application.properties)

server.port=8001

# MongoDB Configuration
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=license_management

# File storage location
registry.file-path=data/registry/service_registry.json

# Grace period days and hourly schedule setup
registry.grace-period-days=15
registry.scheduler.cron=0 0 * * * *

---

## 🚀 How to Run and Test

1. **Start MongoDB via Docker:**
   docker run -d --name mongodb -p 27017:27017 mongo:latest

2. **Start the application:**
   mvn spring-boot:run

3. **Trigger Registry Sync:**
   Invoke-RestMethod -Uri "http://localhost:8001/api/v1/license/registry/sync" -Method Post

4. **Test the Kubernetes Polling Endpoint:**
   Invoke-RestMethod -Uri "http://localhost:8001/api/v1/license/registry" -Method Get

5. **Inspect the Local Storage File:**
   Get-Content data/registry/service_registry.json | ConvertFrom-Json | ConvertTo-Json -Depth 5