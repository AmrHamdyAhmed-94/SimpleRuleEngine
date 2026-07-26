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
strategy
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
- `actionField`
- `actionValue`
- `priority`
- `enabled`

`id` is the database primary key.

`ruleCode` is the public stable identifier for API responses and future API references.

Each rule means:

```text
When conditionField conditionOperator conditionValue,
set actionField to actionValue.
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

For the first version, every action is a simple SET action:

```text
transaction[actionField] = actionValue
```

Do not add `ActionType` unless the requirement changes.

## Execution Design

Use the Strategy Pattern for rule execution behavior:

```text
RuleExecutionStrategy
├── EnrichmentRuleStrategy
└── RoutingRuleStrategy
```

Use a Spring-managed registry/factory to select the correct strategy based on `RuleType`.

Shared rule logic belongs outside the individual strategies when possible:

- condition evaluation
- action execution
- value conversion
- applied rule response mapping

## Priority Convention

Higher number means higher priority.

For `ENRICHMENT`:

- load enabled enrichment rules
- sort by priority ascending
- evaluate every rule
- apply every matching rule
- higher-priority rules run later and win if they update the same field

For `ROUTING`:

- load enabled routing rules
- sort by priority descending
- apply the first matching rule
- stop immediately

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
