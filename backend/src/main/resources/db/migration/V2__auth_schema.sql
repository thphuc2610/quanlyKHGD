create table app_user (
    id bigserial primary key,
    username varchar(100) not null unique,
    password_hash varchar(255) not null,
    full_name varchar(255) not null,
    teacher_name varchar(255),
    enabled boolean not null default true,
    created_at timestamptz not null default now()
);

create table user_role (
    user_id bigint not null references app_user(id) on delete cascade,
    role varchar(40) not null,
    primary key (user_id, role)
);
