# Evaluator Requirements Checklist

- Persist business rules in the database.
- Provide CRUD APIs for business rules.
- Support predefined rule types: ENRICHMENT and ROUTING.
- Dynamically inspect PaymentTransaction attributes.
- Dynamically update PaymentTransaction attributes.
- ENRICHMENT applies all matching rules according to priority.
- ROUTING applies only the highest-priority matching rule.
- Execution API returns the modified transaction, applied rule count, and applied rule names.
- Include Postman collection.
- Keep the solution simple and avoid unnecessary rule-engine frameworks.

