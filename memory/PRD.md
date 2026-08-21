# License Key Management System - PRD

## Project Overview
A full-stack License Key Management System built with Java Spring Boot 3.2.2 and React, designed to control application access based on valid subscriptions. Includes client SDKs for Java, Next.js, and Python applications.

## Tech Stack
- **Backend**: Java 17, Spring Boot 3.2.2, Spring Security, Spring Data MongoDB, Spring Mail
- **Database**: MongoDB
- **Frontend**: React 19, Tailwind CSS, React Router
- **Authentication**: JWT-based (jjwt 0.12.5)
- **SDKs**: Java, TypeScript/Next.js, Python

## Core Requirements

### Functional Requirements (Implemented)
1. ✅ Generate unique license keys (XXXX-XXXX-XXXX-XXXX format)
2. ✅ License contains: Key, Start Date, Expiry Date, Subscription Type, Grace Period
3. ✅ License validation on application start (online + offline)
4. ✅ Expiry notification (7 days before) via SMTP email
5. ✅ Grace period with limited access (read-only, core features only)
6. ✅ Access restriction after grace period ends
7. ✅ Offline license validation using signed license files
8. ✅ Online server verification with access level response
9. ✅ License renewal with expiry date update
10. ✅ Daily email notifications during grace period

### Non-Functional Requirements (Implemented)
- ✅ Security: HMAC-SHA256 signed license files, JWT auth
- ✅ Reliability: Offline mode with signed license files
- ✅ Performance: Quick validation with MongoDB indexes
- ✅ Scalability: Stateless JWT, horizontal scaling ready

## Access Levels
| Level | Description | Read | Write | Premium |
|-------|-------------|------|-------|---------|
| FULL | Active license | ✅ | ✅ | ✅ |
| LIMITED | Grace period | ✅ | ❌ | ❌ |
| NONE | Expired/Invalid | ❌ | ❌ | ❌ |

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `GET /api/auth/me` - Get current user

### License Management
- `POST /api/license/generate` - Generate new license (with gracePeriodDays)
- `POST /api/license/validate` - Online validation (returns accessLevel)
- `POST /api/license/validate-offline` - Offline key validation
- `POST /api/license/validate-file` - Validate signed license file
- `POST /api/license/renew` - Renew license
- `GET /api/license/{key}/download` - Download license file for offline use
- `PATCH /api/license/{key}/grace-period` - Update grace period
- `GET /api/license/all` - List all licenses
- `POST /api/license/{key}/suspend` - Suspend license
- `POST /api/license/{key}/activate` - Activate license
- `POST /api/license/{key}/revoke` - Revoke license
- `DELETE /api/license/{key}` - Delete license
- `GET /api/license/stats` - Dashboard statistics (includes gracePeriod count)
- `GET /api/license/validation-logs` - Validation history

## SDK Documentation
Located in `/app/sdks/`:
- **Java SDK**: `/sdks/java/LicenseVaultClient.java` + Spring Boot example + tests
- **Next.js SDK**: `/sdks/nextjs/licensevault-sdk.ts` + React example + tests
- **Python SDK**: `/sdks/python/licensevault_sdk.py` + Flask/FastAPI example + tests (20/20 passing)

**Complete SDK Documentation**: `/sdks/SDK_DOCUMENTATION.md`

## Email Notifications
- **Expiry Warning**: 7 days before expiry (once)
- **Grace Period**: Daily emails during grace period
- **Final Expiry**: When grace period ends (once)

## What's Been Implemented (March 17, 2026)

### Phase 1 - MVP
- Complete Spring Boot application with MVC architecture
- MongoDB integration with repositories
- JWT authentication with Spring Security
- License CRUD operations
- Online/Offline validation endpoints
- React frontend with dashboard

### Phase 2 - Grace Period & SDKs (Current)
- Configurable grace period per license
- Access levels: FULL, LIMITED, NONE
- Feature flags: premiumFeaturesEnabled, writeAccessEnabled
- License file generation with HMAC-SHA256 signing
- Offline license file validation endpoint
- SMTP email service (configurable)
- Daily notification scheduler
- Java SDK with Spring Boot example
- Next.js/TypeScript SDK with React example
- Python SDK with Flask/FastAPI example
- Updated frontend with Grace Period stats

## Environment Variables
```env
# Backend
MONGO_URL=mongodb://localhost:27017
DB_NAME=license_management
JWT_SECRET=your-jwt-secret
LICENSE_SIGNING_SECRET=your-signing-secret
GRACE_PERIOD_DAYS=7
EMAIL_ENABLED=false
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=
SMTP_PASSWORD=
EMAIL_FROM=noreply@licensevault.com
```

## Prioritized Backlog

### P0 (Critical) - Completed
- ✅ License generation and validation
- ✅ Grace period with limited access
- ✅ Offline validation support
- ✅ Client SDKs (Java, Next.js, Python)

### P1 (High Priority) - Completed
- ✅ Email notifications (SMTP)
- ✅ License file download
- ✅ Access level enforcement
- ✅ Configurable grace period

### P2 (Medium Priority) - Future
- [ ] Machine ID binding and activation limits
- [ ] Bulk license generation
- [ ] License export (CSV/PDF)
- [ ] Webhook integrations for notifications

### P3 (Low Priority) - Future
- [ ] Analytics dashboard with charts
- [ ] Multi-tenant support
- [ ] API rate limiting
- [ ] Mobile SDK (iOS/Android)

## Next Steps
1. Enable SMTP email notifications in production
2. Add machine ID binding for device-specific licenses
3. Create admin role with elevated permissions
4. Add Swagger/OpenAPI documentation
