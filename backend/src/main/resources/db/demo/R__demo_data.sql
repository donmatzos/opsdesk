-- Repeatable and idempotent: safe to run on every start of the demo profile.
insert into services (name, owner_team, stage, status) values
    ('billing-api',     'team-payments', 'PROD', 'UP'),
    ('customer-portal', 'team-web',      'PROD', 'DEGRADED'),
    ('order-service',   'team-orders',   'PROD', 'UP'),
    ('report-batch',    'team-data',     'DEV',  'DOWN')
on conflict (name) do nothing;
