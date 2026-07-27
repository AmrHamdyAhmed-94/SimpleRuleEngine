# SimpleRuleEngine

A Spring Boot rule engine for payment transaction processing. Rules evaluate transaction fields and either enrich or route the transaction based on configurable conditions.

## Tech Stack

- Java 17
- Spring Boot 4.0.7
- Spring Web MVC, Spring Data JPA, Spring AOP
- PostgreSQL (production), H2 (tests and quick-start profile)
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

## Execution Design

Rule type execution uses simple polymorphism. `EnrichmentRuleExecutor` and `RoutingRuleExecutor` both implement the `RuleExecutor` interface and encapsulate their own execution behavior.

`RuleEngineService` injects both executors directly and uses a plain switch on `RuleType` to select which one to call. There is no registry or factory.

`RuleConditionEvaluator` evaluates whether a rule's condition matches the current transaction.

`RuleActionExecutor` applies the rule's action. It switches on `ActionType` to decide what to do — currently only `SET_VALUE` is supported. Adding a new action type means adding a value to `ActionType` and a case in the switch, with no other changes required.

## ActionType

`actionType` defaults to `SET_VALUE` when not provided in a create request. It is stored on the rule and controls how the action is applied at execution time, making future actions easy to add without changing the rule table shape.

## BeanWrapperImpl

`RuleConditionEvaluator` and `RuleActionExecutor` use Spring's `BeanWrapperImpl` to read and write `PaymentTransaction` fields by name at runtime, without reflection boilerplate. `RuleFieldValidator` uses the same mechanism at rule create/update time to reject rules that reference non-existent fields before they reach the database.

## Data Model Notes

**`PaymentTransaction`** is a request/response model only. It is never persisted as a JPA entity. It exists solely within the scope of a single execution request.

**`BusinessRule`** fields:

| Field | Description |
|---|---|
| `ruleCode` | Public stable identifier (e.g. `HIGH_VALUE_ROUTE`) |
| `ruleType` | `ENRICHMENT` or `ROUTING` |
| `conditionField` / `conditionOperator` / `conditionValue` | Defines when the rule matches |
| `actionType` | How to act — currently `SET_VALUE` |
| `actionField` / `actionValue` | What to change on the transaction |
| `priority` | Higher number = higher precedence |
| `enabled` | Whether the rule participates in execution |
