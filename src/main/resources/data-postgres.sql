-- Seed data for local/manual testing.
-- Safe to rerun: ON CONFLICT (rule_code) DO NOTHING.

INSERT INTO simple_rule_engine.business_rules
    (rule_code, name, rule_type, condition_field, condition_operator, condition_value, action_type, action_field, action_value, priority, enabled)
VALUES
    ('ENRICH_INBOUND_STATUS',
     'Set status to RECEIVED for inbound transactions',
     'ENRICHMENT', 'direction', 'EQUALS', 'INBOUND', 'SET_VALUE', 'status', 'RECEIVED', 10, true),

    ('ENRICH_HIGH_VALUE_DESCRIPTION',
     'Flag high-value transactions in description',
     'ENRICHMENT', 'amount', 'GREATER_THAN', '1000', 'SET_VALUE', 'description', 'High value transaction', 20, true),

    ('ROUTE_HIGH_VALUE_TO_PRIORITY_QUEUE',
     'Route transactions above 5000 to high-value queue',
     'ROUTING', 'amount', 'GREATER_THAN', '5000', 'SET_VALUE', 'route', 'HIGH_VALUE_QUEUE', 100, true),

    ('ROUTE_USD_TO_USD_QUEUE',
     'Route USD transactions to USD queue',
     'ROUTING', 'currency', 'EQUALS', 'USD', 'SET_VALUE', 'route', 'USD_QUEUE', 50, true)

ON CONFLICT (rule_code) DO NOTHING;
