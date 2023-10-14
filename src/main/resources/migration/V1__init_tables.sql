create table user_reference
(
    id     varchar(255) not null primary key,
    source varchar(255),
    language text,
    type text,
    last_activity_timestamp timestamp
);

create table user_message_report
(
    id                            uuid not null primary key,
    user_reference_id             text not null,
    created_at                    timestamp not null,
    text                          text
);