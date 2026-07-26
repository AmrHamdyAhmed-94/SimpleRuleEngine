# Coding Guidelines

## General Style

Keep the code clean, direct, and easy to review. Prefer simple Spring Boot patterns over custom abstractions.

Use meaningful names and avoid abbreviations unless they are standard in the project.

## Lombok

Use Lombok to reduce boilerplate where it improves readability:

- `@Getter`
- `@Setter`
- `@Builder`
- `@NoArgsConstructor`
- `@AllArgsConstructor`
- `@RequiredArgsConstructor`

For services and controllers, prefer constructor injection with `@RequiredArgsConstructor`.

## Validation

Use Bean Validation annotations on request DTOs:

- `@NotBlank`
- `@NotNull`
- numeric validations if needed later

Use wrapper types such as `Boolean` in request DTOs when null should mean "use a default".

Use `StringUtils.hasText` when checking strings manually in service or utility code.

## DTO Mapping

Use MapStruct for entity/DTO mapping.

Keep mappers small and explicit. Do not hide business logic inside mappers.

Recommended package:

```text
mapper
```

Recommended style:

- `componentModel = "spring"`
- mapper interfaces only
- service classes call mappers
- service classes keep business decisions, defaults, and validation

## Exceptions

Use clear runtime exceptions for predictable application errors, such as:

- resource not found
- invalid rule definition
- unsupported rule type
- invalid transaction field

If exception handling is added, keep a single `GlobalExceptionHandler` and return simple error responses.

## Utilities

Only create a `util` package when shared helper logic is used in more than one place or makes the code meaningfully clearer.

Good candidates:

- field validation helpers
- type conversion helpers
- text normalization helpers

Do not create utilities for one-line logic used once.

## Rule Engine Code

Use Spring `BeanWrapper` for dynamic field reads and writes against `PaymentTransaction`.

Keep condition evaluation and action execution separate from strategy selection:

- strategies decide execution flow
- evaluator decides whether a rule matches
- executor applies the rule action

## Controller Style

Use RESTful endpoints and proper response codes:

- `201 Created` for create
- `200 OK` for reads and updates
- `204 No Content` for delete

Use `@Valid` on request bodies.

Use `@RestController` for API controllers. `@RestController` already includes `@ResponseBody`; do not switch to plain `@Controller` unless there is a specific reason.

Keep OpenAPI annotations light. Swagger can infer most details from controllers, DTOs, and validation annotations.

## Testing

Add focused tests for behavior that can break:

- CRUD service behavior
- condition matching
- priority order
- enrichment applies all matching rules
- routing applies only the highest-priority matching rule
- invalid fields
- value conversion

Prefer targeted unit tests for engine logic and only add integration tests where they provide real confidence.
