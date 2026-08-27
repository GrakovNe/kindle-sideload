-- Snapshot of the database schema (state of the Flyway migrations in db/migration)
-- used only by the jOOQ codegen task. The source of truth is db/migration;
-- keep this file in sync when a new migration is added.
create table "user"
(
    id                           varchar not null primary key,
    language                     text,
    type                         text,
    last_activity_timestamp      timestamp
);

create table user_message_report
(
    id                            uuid not null primary key,
    user_id                       text not null,
    created_at                    timestamp not null,
    text                          text
);

create table converter_binary_reference
(
    id                           uuid not null primary key,
    published_at                 timestamp
);

create table user_activity_state
(
    id uuid primary key,
    user_id varchar not null,
    activity_state varchar not null,
    created_at timestamp not null
);

create table convertation_task
(
    id uuid primary key,
    user_id varchar not null,
    source_file_url varchar not null,
    created_at timestamp not null,
    fail_reason varchar,
    status varchar not null,
    file_name text
);

create table message_reference
(
    id varchar primary key,
    status varchar not null
);

create table user_preferences
(
    id uuid primary key,
    user_id text not null,
    output_format text,
    debug_mode boolean,
    email text,
    automatic_stk boolean not null default false
);

create table transfer_email_task
(
    id uuid primary key,
    user_id varchar not null,
    environment_id varchar not null,
    created_at timestamp not null,
    fail_reason varchar,
    status varchar not null
);

create table shelf_reference
(
    id uuid primary key,
    short_id varchar not null unique,
    user_id varchar not null unique
);

create table shelf_item
(
    id uuid primary key,
    shelf_id uuid not null,
    environment_id varchar not null,
    created_at timestamp not null,
    status varchar not null
);
