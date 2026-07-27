# Architecture

## Goal

Build a small Spring Boot application that manages database-backed business rules and executes them against a payment transaction.

The solution must stay simple and focused on the evaluator requirements. It is not intended to become a generic rules platform.

## Technology Choices

- Java 17
- Spring Boot 4.0.7
- Maven
- PostgreSQL
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- Lombok
- Springdoc OpenAPI

## Package Structure

Use the existing package root:

```text
com.simpleRuleEngine
```

Main packages:

```text
controller
dto.request
dto.response
entity
enums
mapper
repository
service
```

Allowed supporting packages when needed:

```text
exception
model
engine
util
```

Do not create extra layers unless there is a clear need.

## Core Domain

This is a transaction rule engine. The engine executes rules against a payment transaction model.

Do not expose JPA entities directly from controllers. API requests and responses must use DTOs or request/response models.

Entity database IDs may be used internally. Public APIs should use readable business codes such as `HIGH_VALUE_ROUTE` or `INBOUND_STATUS` instead of exposing persistence details.

### BusinessRule

Rules are stored as structured database records, not executable scripts.

Each rule has:

- `id`
- `ruleCode`
- `name`
- `ruleType`
- `conditionField`
- `conditionOperator`
- `conditionValue`
- `actionType`
- `actionField`
- `actionValue`
- `priority`
- `enabled`

`id` is the database primary key.

`ruleCode` is the public stable identifier for API responses and future API references.

Each rule means:

```text
When conditionField conditionOperator conditionValue,
perform actionType on actionField using actionValue.
```

### Rule Types

Supported rule types:

- `ENRICHMENT`
- `ROUTING`

### Condition Operators

Supported condition operators:

- `EQUALS`
- `NOT_EQUALS`
- `GREATER_THAN`
- `GREATER_THAN_OR_EQUALS`
- `LESS_THAN`
- `LESS_THAN_OR_EQUALS`

### Action Model

Actions are scalable through `ActionType`. Each rule carries an `actionType` that tells the action executor how to apply the action.

Currently supported action types:

- `SET_VALUE` — writes `actionValue` into `actionField` on the transaction using `BeanWrapperImpl`

To add a new action type, add the value to `ActionType` and add a `case` in `RuleActionExecutor.execute()`. No table changes are needed.

## Execution Design

Rule type execution uses simple polymorphism through the `RuleExecutor` interface in `service/executor`.

`EnrichmentRuleExecutor` and `RoutingRuleExecutor` each implement `RuleExecutor` with their own execution behavior:

- `EnrichmentRuleExecutor` evaluates every rule and applies all that match, in the order they are received.
- `RoutingRuleExecutor` evaluates rules and applies only the first match, then stops.

`RuleEngineService` injects both executors directly and selects which one to call based on `RuleType`:

```
RuleEngineService.execute()
  ├── ENRICHMENT → enrichmentRuleExecutor.execute()
  └── ROUTING   → routingRuleExecutor.execute()
```

There is no registry or factory. The selection is a plain switch on `RuleType`.

`RuleConditionEvaluator` evaluates whether a rule's condition matches the current transaction state.

`RuleActionExecutor` applies the rule's action. It switches on `ActionType` to dispatch to the correct logic.

Shared rule logic stays in dedicated components rather than duplicated across execution paths:

- condition evaluation — `RuleConditionEvaluator`
- action execution — `RuleActionExecutor`
- field existence validation at create/update time — `RuleFieldValidator`

## Priority Convention

Higher number means higher priority.

For `ENRICHMENT`:

- load enabled enrichment rules ordered by priority ascending
- evaluate every rule against the transaction
- apply every matching rule — higher-priority rules run later and win if they write the same field
- a rule can see changes made by an earlier rule in the same pass

For `ROUTING`:

- load enabled routing rules ordered by priority descending
- apply the first matching rule and stop
- only one route is ever assigned per execution

## APIs

Business rule CRUD:

```text
POST   /api/business-rules
PUT    /api/business-rules/{ruleCode}
GET    /api/business-rules
GET    /api/business-rules/{ruleCode}
DELETE /api/business-rules/{ruleCode}
```

CRUD request bodies must not contain the database entity id.

Create requests should contain `ruleCode`, because it is a readable business/public code chosen by the API user.

Update requests should not require `ruleCode` in the body. The rule being updated is already identified by the path variable.

CRUD responses must be DTOs, not entities. Prefer exposing `ruleCode` instead of the database `id`.

Rule execution:

```text
POST /api/rule-engine/execute?type=ENRICHMENT
POST /api/rule-engine/execute?type=ROUTING
```

Execution response must include:

- modified transaction
- applied rule count
- applied rule names/details

The transaction sent to the execution endpoint should be a model/request object, not a persisted transaction entity unless the evaluator explicitly asks for transaction persistence.

## Deliberate Non-Goals

Do not add:

- Drools
- SpEL
- MVEL
- JavaScript or scripting
- dynamic Java compilation
- Kafka or event-driven processing
- microservices
- Docker requirement
- authentication or authorization
- rule versioning
- approval workflows
- audit tables
- caching
- nested condition groups
- multiple actions per rule
- generic rule framework abstractions
