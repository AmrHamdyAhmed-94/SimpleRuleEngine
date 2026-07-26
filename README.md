# SimpleRuleEngine

A Spring Boot rule engine for payment transaction processing. Rules evaluate transaction fields and either enrich or route the transaction based on configurable conditions.

## Tech Stack

- Java 17
- Spring Boot 4.0.7
- Spring Web MVC, Spring Data JPA, Spring AOP
- PostgreSQL (production), H2 (tests)
- Hibernate 6 / Jakarta Persistence
- Bean Validation (Jakarta)
- Lombok, MapStruct 1.6.0
- springdoc-openapi

## Database Setup

```sql
CREATE DATABASE simple_rule_engine;
\c simple_rule_engine
CREATE SCHEMA simple_rule_engine;
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_NAME` | `simple_rule_engine` | PostgreSQL database name |
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |

The app connects to `localhost:5432` by default. Override via standard Spring `SPRING_DATASOURCE_URL` if needed.

## Running Locally

**Default (PostgreSQL)** — requires a running PostgreSQL instance (see Database Setup above):

```bash
./mvnw spring-boot:run
```

**Quick evaluator mode (H2, no PostgreSQL needed)**:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

On first startup, Hibernate creates the schema and seeds four sample rules automatically. The H2 profile uses an in-memory database; all data is lost when the process stops.

### H2 Console (h2 profile only)

```
http://localhost:8080/h2-console
```

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:simple_rule_engine` |
| Username | `sa` |
| Password | _(empty)_ |

## Swagger UI

```
http://localhost:8080/swagger-ui.html
```

## Postman Collection

Import both files from `postman/`:

- `SimpleRuleEngine.postman_collection.json`
- `SimpleRuleEngine.postman_environment.json`

The collection is re-runnable. CREATE requests accept 201 or 409. DELETE requests accept 204 or 404.

## API Endpoints

### Business Rules

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/business-rules` | Create a rule |
| `PUT` | `/api/business-rules/{ruleCode}` | Update a rule |
| `GET` | `/api/business-rules` | List all rules |
| `GET` | `/api/business-rules/{ruleCode}` | Get rule by code |
| `DELETE` | `/api/business-rules/{ruleCode}` | Delete a rule |

### Rule Execution

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/rule-engine/execute?type=ENRICHMENT` | Execute enrichment rules |
| `POST` | `/api/rule-engine/execute?type=ROUTING` | Execute routing rules |

Request body: `{ "transaction": { ... } }`  
Optional header: `Idempotency-Key: <string>`

## Priority Convention

Higher number = higher priority.

- ENRICHMENT loads rules **ascending by priority** — a lower-priority rule runs first and a higher-priority rule can overwrite its output.
- ROUTING loads rules **descending by priority** — the highest-priority matching rule wins and no further rules are evaluated.

Example: a routing rule at priority 100 beats one at priority 50 when both match.

## Execution Behavior

### ENRICHMENT

All matching rules are applied in a single ascending pass. Each rule's condition is evaluated on the current (possibly already-mutated) transaction state, so a later rule can depend on changes made by an earlier rule.

### ROUTING

Rules are evaluated in descending priority order. The first matching rule is applied and execution stops. Only one route is ever assigned.

## Strategy Pattern

Each rule type has a dedicated strategy (`EnrichmentRuleStrategy`, `RoutingRuleStrategy`) registered in `RuleExecutionStrategyRegistry`. `RuleEngineService` resolves the correct strategy at execution time using the `type` query parameter.

## BeanWrapperImpl

`RuleConditionEvaluator` and `RuleActionExecutor` use Spring's `BeanWrapperImpl` to read and write `PaymentTransaction` fields by name at runtime, without reflection boilerplate. `RuleFieldValidator` uses the same mechanism at rule create/update time to reject rules that reference non-existent fields before they reach the database.

## Data Model Notes

**`PaymentTransaction`** is a request/response model only. It is never persisted as a JPA entity. It exists solely within the scope of a single execution request.

**`PaymentTransactionExecutionLog`** is an optional audit and idempotency table. Every successful or failed execution is logged here. The log stores the original transaction snapshot (pre-mutation), the final transaction state, the full response JSON, and a SHA-256 request fingerprint.

## Idempotency-Key Header

Passing `Idempotency-Key: <string>` on a `POST /api/rule-engine/execute` request enables idempotency:

- If the key is new, execution proceeds normally and the result is stored.
- If the key matches a previous **SUCCESS** with the same request fingerprint, the cached response is returned immediately — rules are not re-executed.
- If the key matches a previous **SUCCESS** with a **different** request fingerprint (same key, different transaction or type), `409 IDEMPOTENCY_CONFLICT` is returned.
- If the key matches a previous **FAILED** execution, `409 IDEMPOTENCY_CONFLICT` is returned — the client must use a new key to retry.

The request fingerprint is a SHA-256 hash of `ruleType + originalTransactionJSON`, computed before any rule mutations.
