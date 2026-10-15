# SMART SHIELD — EXTERNAL APIS & INTEGRATIONS GUIDE

## 1. Executive Summary & Overview
Smart Shield is a Public Grievance & Citizen Safety System with a multi-tiered architecture:
- **Frontend**: Citizen Portal (`Smart_Shield_Public_Grievance_Frontend/smart_shield_frontend`) and Admin Operations Console (`index.html`).
- **Backend**: Spring Boot 3.3.4 (Java 17/25, port 8082) with Spring Data JPA.
- **Database**: MySQL 8.x (`smart_shield_db`).

This document details the architecture, configuration, fallback strategies, and verification procedures for the 6 external integrations:
1. **Google Maps JavaScript API** (Browser interactive mapping)
2. **Google Maps Routes API** (Server-side route calculation & polyline generation)
3. **Google Places API (New)** (Server-side destination search & autocomplete)
4. **Google Gemini AI API** (Civic & Legal Chat Assistant + Multimodal Form Scanning)
5. **Telegram Bot API** (Real-time operational alerts for SOS, Grievance Registration & Status Updates)
6. **SMTP Email Transport** (Citizen confirmations and status updates via JavaMail)

---

## 2. Integration Status Matrix

| Integration | Initial Status | Implementation Changes | Test Performed | Final Status | Remaining Manual Action |
|---|---|---|---|---|---|
| **Google Maps JS API** | `PARTIALLY_IMPLEMENTED` | Added `#googleMap` container, responsive CSS, dual-mode dynamic JS loader, polyline decoder (`decodePolyline`), key unification in `ConfigController`, and automatic Leaflet fallback. | Mocked key fallback validation, dynamic script loader checks, Leaflet preservation tests. | `IMPLEMENTED_BUT_UNVERIFIED` | Configure `GOOGLE_MAPS_BROWSER_KEY` in Google Cloud Console with HTTP referrer restrictions. |
| **Google Routes API** | `IMPLEMENTED_BUT_UNVERIFIED` | Added 8-second HTTP connection & read timeouts, safe exception logging, and preserved internal fallback distance/bearing algorithm. | Unit tests in `GoogleServicesAndSafeRouteTest`, live backend route calculation via `/api/safety/routes/calculate`. | `IMPLEMENTED_BUT_UNVERIFIED` | Enable Routes API in GCP Console and set `GOOGLE_ROUTES_API_KEY`. Billing required by Google. |
| **Google Places API (New)** | `IMPLEMENTED_BUT_UNVERIFIED` | Added 8-second HTTP connection & read timeouts, defensive error logging, and preserved municipal city boundary fallback. | Tested destination search endpoint `/api/safety/routes/destinations` with local fallback. | `IMPLEMENTED_BUT_UNVERIFIED` | Enable Places API (New) in GCP Console and set `GOOGLE_PLACES_API_KEY`. |
| **Google Gemini AI API** | `PARTIALLY_IMPLEMENTED` | Migrated API key from URL parameter (`?key=...`) to secure `x-goog-api-key` HTTP header; added HTTP error handling; verified conversational and multimodal endpoints. | Verified `/api/ai/status`, `/api/forms/status`, and executed authenticated `/api/ai/chat` which cleanly fell back to `LOCAL_KB`. | `IMPLEMENTED_BUT_UNVERIFIED` | Obtain API key from Google AI Studio and configure `GEMINI_API_KEY` (and `GEMINI_ENABLED=true`). |
| **Telegram Bot API** | `PARTIALLY_IMPLEMENTED` | Mapped `TELEGRAM_CHAT_ID`, added grievance received/status update notifications, bounded 5-minute de-duplication cache, safe Telegram response parsing (`"ok":true`), and created test endpoint `/api/public/telegram/test-notification`. | Unit tests in `ExternalIntegrationsTest`, `/api/public/telegram/test-notification` live check (proper 400 when unconfigured). | `BLOCKED_BY_CREDENTIALS` | Create bot with `@BotFather`, obtain token, set `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`, and `TELEGRAM_ENABLED=true`. |
| **SMTP Email Transport** | `PARTIALLY_IMPLEMENTED` | Added 5-second socket/connect/write timeouts, implemented grievance confirmation and status update dispatchers, linked with `GrievanceService`. | Unit tests in `ExternalIntegrationsTest`, end-to-end grievance creation and status update on live server. | `BLOCKED_BY_CREDENTIALS` | Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (App Password for Gmail). |

---

## 3. Retained Architectural Baselines & Fallbacks
1. **OpenStreetMap & Leaflet**: Remains the default mapping engine when Google Maps API keys are empty or restricted. If Google Maps script fails (`gm_authFailure`), the UI cleanly reverts to Leaflet without breaking.
2. **Local Civic Knowledge Base**: The AI Assistant retains full curated knowledge for RTI, FIRs, Consumer Complaints, Municipal Grievances, and Streetlight/Road safety. If Gemini is unavailable, citizens receive accurate, instant local guidance.
3. **Internal Safe Route Algorithm**: Evaluates crime density, lighting conditions, and road hazards based on verified regional boundaries even without external Google Routes keys.
4. **Non-Blocking Notifications**: Failures in SMTP or Telegram do not break core citizen transactions. Grievances and SOS records are reliably committed to MySQL regardless of external network state.

---

## 4. Configuration Specification

### Environment Variables & Application Properties
All external integrations are controlled through environment variables with safe defaults:

```properties
# ---- Database Connection ----
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:smart_shield_db}?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:your_mysql_password_here}

# ---- Security Signing Secrets ----
smartshield.security.admin-token-secret=${ADMIN_TOKEN_SECRET:smartshield-local-admin-token-secret-key-32chars}
smartshield.security.citizen-token-secret=${CITIZEN_TOKEN_SECRET:smartshield-local-citizen-token-secret-key-32chars}

# ---- SMTP Email Configuration ----
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
spring.mail.from=${MAIL_FROM:noreply@smartshield.gov.in}

# ---- Telegram Bot Configuration ----
smartshield.telegram.enabled=${TELEGRAM_ENABLED:false}
smartshield.telegram.bot-token=${TELEGRAM_BOT_TOKEN:}
smartshield.telegram.bot-username=${TELEGRAM_BOT_USERNAME:SmartShieldAlertBot}
smartshield.telegram.chat-id=${TELEGRAM_CHAT_ID:}

# ---- Google Maps Platform ----
maps.api-key=${GOOGLE_MAPS_BROWSER_KEY:${smartshield.google.maps.browser-api-key:}}
smartshield.google.maps.browser-api-key=${GOOGLE_MAPS_BROWSER_KEY:}
smartshield.google.routes.enabled=${GOOGLE_ROUTES_ENABLED:true}
smartshield.google.routes.api-key=${GOOGLE_ROUTES_API_KEY:}
smartshield.google.places.enabled=${GOOGLE_PLACES_ENABLED:true}
smartshield.google.places.api-key=${GOOGLE_PLACES_API_KEY:}

# ---- Google Gemini AI ----
smartshield.gemini.enabled=${GEMINI_ENABLED:false}
smartshield.gemini.api-key=${GEMINI_API_KEY:}
smartshield.gemini.model=${GEMINI_MODEL:gemini-1.5-flash}
smartshield.gemini.timeout-ms=${GEMINI_TIMEOUT_MS:10000}
```

---

## 5. Step-by-Step Provider Setup Checklist

### A. Google Maps Platform (Maps JS, Routes, Places)
1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create or select a GCP project (e.g. `smart-shield-project`).
3. Enable Billing on the project (required by Google Maps Platform).
4. Navigate to **APIs & Services > Library** and enable:
   - **Maps JavaScript API**
   - **Routes API**
   - **Places API (New)**
5. Navigate to **Credentials > Create Credentials > API Key**:
   - **Browser Key**: Restrict by Application Restriction -> **Websites / HTTP referrers** (e.g., `http://localhost:*/*`, `http://127.0.0.1:*/*`, or production domain). Restrict API access to *Maps JavaScript API*. Set as `GOOGLE_MAPS_BROWSER_KEY`.
   - **Server Key**: Restrict by Application Restriction -> **IP addresses** (your server IP) or leave unrestricted for dev. Restrict API access to *Routes API* and *Places API (New)*. Set as `GOOGLE_ROUTES_API_KEY` and `GOOGLE_PLACES_API_KEY`.

### B. Google Gemini AI API
1. Visit [Google AI Studio](https://aistudio.google.com/).
2. Sign in with your Google Account and click **Get API Key**.
3. Create a key in your GCP project.
4. Set `GEMINI_API_KEY` and set `GEMINI_ENABLED=true`.
5. Supported default model: `gemini-1.5-flash`.

### C. Telegram Bot Setup
1. Open Telegram and search for `@BotFather`.
2. Send `/newbot` and follow instructions to name your bot (e.g., `SmartShieldAlertBot`).
3. Copy the HTTP API token provided by BotFather. Set as `TELEGRAM_BOT_TOKEN`.
4. Create a private channel/group or open a chat with your bot, then send a message.
5. Determine your chat ID:
   - For user chat: Send a message to `@userinfobot` or check `https://api.telegram.org/bot<TOKEN>/getUpdates`.
   - For channel/group: Forward a message to `@JsonDumpBot` or fetch updates via API.
6. Set `TELEGRAM_CHAT_ID` and set `TELEGRAM_ENABLED=true`.
7. Test using non-destructive endpoint:
   ```bash
   curl -X POST http://localhost:8082/api/public/telegram/test-notification \
        -H "Content-Type: application/json" -d '{}'
   ```

### D. SMTP Email Setup (Gmail Example)
1. Log into your Google Account and go to **Security > 2-Step Verification**.
2. Scroll to the bottom and select **App Passwords**.
3. Create an app password named "SmartShield".
4. Copy the 16-character generated string.
5. Set the environment variables:
   - `MAIL_HOST=smtp.gmail.com`
   - `MAIL_PORT=587`
   - `MAIL_USERNAME=your-email@gmail.com`
   - `MAIL_PASSWORD=your-16-char-app-password`
   - `MAIL_FROM=your-email@gmail.com`

---

## 6. Verification and Testing

### Build & Unit Tests
Run Maven test suite from `smart-shield-backend/smart-shield-backend`:
```bash
mvn test
```
Result: **28 tests run, 0 failures, 0 errors, 0 skipped**.

### Verification Script
An automated end-to-end PowerShell script is provided to verify all public and authenticated endpoints:
```powershell
powershell -ExecutionPolicy Bypass -File scratch/test_live_api.ps1
```
Verified workflows:
- Public configurations (`/api/config`, `/api/safety/routes/config`, `/api/ai/status`, `/api/forms/status`)
- Citizen registration & authentication (`/api/auth/signup`, `/api/auth/login`)
- Civic AI assistant conversation (`/api/ai/chat`) with fallback
- Safe route calculation (`/api/safety/routes/calculate`) with safety scoring
- Citizen grievance submission (`/api/grievances`) with automatic notification triggers
- Admin authentication & grievance status update (`/api/admin/grievances/{trackingId}/status`)
- Telegram test notification endpoint (`/api/public/telegram/test-notification`)
