create table services (
    id         bigint generated always as identity primary key,
    name       varchar(100) not null unique,
    owner_team varchar(100) not null,
    stage      varchar(10)  not null check (stage in ('DEV', 'PROD')),
    status     varchar(10)  not null check (status in ('UP', 'DEGRADED', 'DOWN'))
);
