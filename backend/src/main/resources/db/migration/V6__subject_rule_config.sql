create table subject_rule_config (
    id bigserial primary key,
    rule_code varchar(120) not null unique,
    subject_name varchar(500) not null,
    formula varchar(2000) not null,
    note varchar(1000),
    updated_at timestamptz not null default now()
);

create index idx_subject_rule_config_subject on subject_rule_config(subject_name);
