create table import_batch (
    id bigserial primary key,
    file_name varchar(255) not null,
    academic_year varchar(50) not null,
    semester varchar(50) not null,
    status varchar(40) not null,
    created_at timestamptz not null default now(),
    total_rows integer not null default 0,
    valid_rows integer not null default 0,
    warning_rows integer not null default 0
);

create table class_record (
    id bigserial primary key,
    import_batch_id bigint not null references import_batch(id) on delete cascade,
    academic_year varchar(50),
    semester varchar(50),
    class_name varchar(500),
    subject_name varchar(500),
    credits double precision,
    department_hn varchar(255),
    department_ph varchar(255),
    student_count integer,
    teacher_original_name varchar(255),
    teacher_name varchar(255),
    position varchar(255),
    degree varchar(255),
    academic_title varchar(255),
    unit_name varchar(255),
    source_row_number integer,
    valid boolean not null default true,
    warning_message varchar(1000)
);

create table calculation_result (
    id bigserial primary key,
    class_record_id bigint not null references class_record(id) on delete cascade,
    import_batch_id bigint not null references import_batch(id) on delete cascade,
    rule_code varchar(80),
    rule_name varchar(255),
    coefficient_k double precision,
    coefficient_theory double precision,
    coefficient_practice double precision,
    standard_hours double precision not null,
    explanation varchar(1200),
    calculated_at timestamptz not null default now()
);

create index idx_class_record_batch on class_record(import_batch_id);
create index idx_class_record_teacher on class_record(teacher_name);
create index idx_class_record_department on class_record(department_ph);
create index idx_calculation_result_batch on calculation_result(import_batch_id);
create index idx_calculation_result_rule on calculation_result(rule_code);
