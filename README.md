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
   - Stores fields like `serviceId`, `serviceName`, `policy`, `state`, `licenseState`, and `action`.

3. **Runs an Hourly Background Check (Scheduler)**
   - Includes a background process (`LicenseScheduler.java`) that runs **every hour**.
   - It scans the stored file, checks if any license has expired, updates the status flags, and saves the changes.

4. **Provides an Endpoint for Kubernetes**
   - Exposes an API endpoint (`GET /api/v1/license/registry`) so Kubernetes cluster controllers can read the status and take action (`NONE`, `WARN`, `SHUTDOWN`).
   - Returns data as a JSON list:
```json
[
  {
    "serviceId": "svc-001",
    "serviceName": "as1",
    "status": "ACTIVE",
    "action": "NONE"
  },
  {
    "serviceId": "svc-002",
    "serviceName": "as2",
    "status": "GRACE_PERIOD",
    "action": "WARN"
  },
  {
    "serviceId": "svc-003",
    "serviceName": "IS1",
    "status": "EXPIRED",
    "action": "SHUTDOWN"
  }
]
```

5. **Easy Configuration**
   - All main settings (port number, file paths, grace period length, and schedule time) are kept in `src/main/resources/application.properties`.

---

## 📁 Project Folder Structure

```text
src/main/java/com/licenseservice/
├── controller/
│   └── LicenseRegistryController.java   # API endpoint for Kubernetes
├── model/
│   ├── ActionRequired.java               # Enum: NONE, WARN, SHUTDOWN
│   ├── K8sServiceActionResponse.java    # Data structure for Kubernetes responses
│   ├── LicenseState.java                 # Enum: ACTIVE, GRACE_PERIOD, EXPIRED
│   ├── ServiceRegistryEntry.java        # Main record model
│   └── ServiceState.java                 # Enum: RUNNING, DEGRADED, SHUTDOWN, UNAVAILABLE
├── repository/
│   └── FileRegistryRepository.java       # Handles reading and writing to the JSON file
├── scheduler/
│   └── LicenseScheduler.java            # Runs background checks every hour
└── service/
    └── LicenseRegistryService.java       # Business logic for license checks and grace periods
```

---

## ⚙️ Configuration (`application.properties`)

```properties
server.port=8001

# File storage location
registry.storage.file-path=data/registry/service_registry.json

# Grace period days and hourly schedule setup
registry.grace-period-days=15
registry.scheduler.cron=0 0 * * * *
```

---

## 🚀 How to Run and Test

1. **Start the app:**
   ```powershell
   mvn spring-boot:run
   ```

2. **Test the API endpoint in PowerShell:**
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8001/api/v1/license/registry" -Method Get
   ```
