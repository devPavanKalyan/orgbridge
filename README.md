# OrgBridge Orchestrator

OrgBridge uses Spring Boot WebFlux for Salesforce Metadata API
orchestration and PostgreSQL for durable state. Salesforce calls the
integration API through Apex and a Named Credential.

## Persistence

- PostgreSQL: clients, verification tokens, Salesforce organization
  profiles, operation history, and asynchronous operation jobs.
- Flyway: versioned PostgreSQL schema migrations at application startup.
- Redis: short-lived Salesforce sessions only. Sessions are stored
  in expiring hashes and are not durable application records.

MongoDB is not used.

## Run locally

Start PostgreSQL and Redis:

```powershell
docker compose up -d postgres redis
```

Set the required development environment:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DATABASE_R2DBC_URL = "r2dbc:postgresql://localhost:5433/orgbridge"
$env:DATABASE_JDBC_URL = "jdbc:postgresql://localhost:5433/orgbridge"
$env:DATABASE_USERNAME = "orgbridge"
$env:DATABASE_PASSWORD = "orgbridge"
$env:REDIS_URL = "redis://localhost:6379"
$env:ORGBRIDGE_INTEGRATION_API_KEY = "<at-least-32-random-characters>"
$env:SALESFORCE_PRODUCTION = "https://login.salesforce.com/services/Soap/u/67.0"
$env:SALESFORCE_SANDBOX = "https://test.salesforce.com/services/Soap/u/67.0"
```

The development profile includes local-only defaults for JWT timing and
signing. Override `JWT_SECRET` whenever tokens need to survive across local
environments. Mail variables are only required when exercising email flows.
Then run:

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway creates the schema automatically. R2DBC handles non-blocking
application queries; Flyway deliberately uses the JDBC URL because schema
migration is startup work rather than request-path work.

## Salesforce integration API

Base path: `/api/v1/integration`

Every request requires:

- `X-OrgBridge-Integration-Key`
- `X-Salesforce-Org-Id`
- `X-Salesforce-User-Id`
- optional `X-Correlation-Id`

Available routes:

- `GET /dashboard`
- `GET /organizations`
- `POST /organizations`
- `PUT /organizations/{id}`
- `DELETE /organizations/{id}`
- `POST /organizations/{id}/activate`
- `POST /organizations/{id}/test`
- `GET /metadata/types?organizationId=...`
- `POST /metadata/components`
- `POST /csv/validate`
- `POST /operations`
- `GET /operations`
- `GET /operations/{id}`

Operations are accepted with HTTP `202`, stored in PostgreSQL, claimed
atomically with `FOR UPDATE SKIP LOCKED`, and executed by the reactive
worker. Supported operation types are:

- `COMPONENT_RETRIEVE`
- `COMPONENT_VALIDATE`
- `COMPONENT_DEPLOY`
- `CSV_VALIDATE`
- `CSV_DEPLOY`

Example:

```json
{
  "type": "COMPONENT_DEPLOY",
  "sourceOrganizationId": "source-org",
  "targetOrganizationId": "target-org",
  "components": [
    {
      "type": "CustomObject",
      "name": "Invoice__c"
    }
  ],
  "options": {
    "rollbackOnError": true,
    "testLevel": "NoTestRun"
  }
}
```

## Tests

```powershell
.\mvnw.cmd test
```

The suite covers Spring context startup, CSV rules, tenant-scoped operation
creation, Salesforce integration authentication, and safe problem responses.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the Apex/WebFlux
boundary and deployment setup.
