create table incidents (
    id          bigint generated always as identity primary key,
    service_id  bigint       not null references services (id),
    severity    varchar(4)   not null check (severity in ('SEV1', 'SEV2', 'SEV3')),
    state       varchar(10)  not null check (state in ('OPEN', 'MITIGATED', 'RESOLVED')),
    summary     varchar(200) not null,
    opened_at   timestamptz  not null,
    resolved_at timestamptz
);

create index incidents_state_idx on incidents (state);
