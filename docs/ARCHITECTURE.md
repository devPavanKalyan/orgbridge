# OrgBridge backend architecture

## Responsibility boundary

| Concern | Owner | Reason |
|---|---|---|
| Current Salesforce org and user identity | Apex | Salesforce is the trust source for the active LWC user. |
| LWC-callable facade | Apex | Prevents browser-held integration secrets and keeps Salesforce authorization enforceable. |
| Per-user UI preferences | Apex hierarchy custom setting | Native, low-latency Salesforce configuration scoped to the current user. |
| External Salesforce session orchestration | WebFlux | Centralized integration lifecycle and non-blocking Salesforce calls. |
| Metadata list, retrieve, validate, and deploy | WebFlux | Long-running remote I/O and cross-org orchestration do not fit Apex governor limits. |
| CSV validation and metadata ZIP construction | WebFlux | Reusable server validation and bounded worker execution. |
| Durable jobs, users, and operation history | PostgreSQL | Transactional relational state, indexing, retention, and reporting. |
| Short-lived Salesforce sessions | Redis | Expiring secrets must not be mixed with durable business records. |

## Request flow

1. LWC invokes `OrgBridgeBackendController`.
2. Apex derives the Salesforce org/user IDs from `UserInfo`.
3. `OrgBridgeBackendGateway` calls the `OrgBridge_Backend` Named Credential.
4. WebFlux validates the integration key and Salesforce IDs.
5. Read requests execute reactively; write operations are persisted as
   `QUEUED` PostgreSQL jobs and return HTTP `202`.
6. Workers atomically claim jobs, call Salesforce, and persist the final
   status and sanitized result.

The tenant key is the calling Salesforce org ID plus user ID. Job reads are
always constrained by this key.

## PostgreSQL

Flyway migrations:

- `V1__create_core_schema.sql`: clients, verification tokens, and operation
  history.
- `V2__create_operation_jobs.sql`: tenant-scoped asynchronous jobs and
  queue indexes.
- `V3__create_salesforce_organizations.sql`: tenant-scoped Salesforce
  organization profiles and active-organization constraints.

Application access uses Spring Data R2DBC. The worker's queue claim is a
PostgreSQL CTE with `FOR UPDATE SKIP LOCKED`, allowing multiple instances to
process jobs without claiming the same row.

## Salesforce setup

Deploy the Apex classes, `OrgBridge_Settings__c`, and the
`OrgBridge_User` permission set from the Salesforce project.

Create an enhanced Named Credential with API name
`OrgBridge_Backend`:

1. Set its endpoint to the HTTPS URL of the WebFlux service.
2. Store the same random value used by
   `ORGBRIDGE_INTEGRATION_API_KEY` as the protected custom header
   `X-OrgBridge-Integration-Key`.
3. Do not place this value in Apex, LWC, custom settings, or source control.
4. Grant Named Credential access and assign the `OrgBridge_User`
   permission set to application users.

The Apex gateway supplies the Salesforce org ID, user ID, and a correlation
ID for each request. The backend never trusts browser-supplied tenant
identity.

## Deployment variables

Required PostgreSQL variables:

- `DATABASE_R2DBC_URL`
- `DATABASE_JDBC_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`

Required integration variables:

- `ORGBRIDGE_INTEGRATION_API_KEY`
- `REDIS_URL`
- `REDIS_PREFIX`
- `REDIS_SESSION_TTL_SECONDS`

Required application variables:

- `FRONTEND_URL`
- `JWT_SECRET`
- `JWT_EXPIRATION`
- `JWT_REFRESH_EXPIRATION`
- `SALESFORCE_PRODUCTION`
- `SALESFORCE_SANDBOX`

Optional API tuning:

- `SALESFORCE_API_VERSION` (defaults to `67.0`)

Worker tuning:

- `OPERATIONS_WORKER_ENABLED`
- `OPERATIONS_WORKER_CONCURRENCY`

Render waits for GitHub CI checks before deploying and verifies
`/actuator/health` before replacing the live instance. On an existing Render
Blueprint, add newly introduced `sync: false` values in the dashboard because
Blueprint updates do not populate them automatically.

Use independent, rotated secrets per environment. PostgreSQL and Redis
must require TLS in production.

## Operational safeguards

- Integration API keys are compared in constant time.
- Salesforce IDs and request paths are validated at both boundaries.
- SOAP XML values are escaped and XML parsing disables external entities.
- Salesforce session IDs are never logged.
- Internal exception details are hidden from API callers.
- Correlation IDs are returned and persisted with jobs.
- Session cache failures are surfaced instead of being misreported as
  missing organizations.
