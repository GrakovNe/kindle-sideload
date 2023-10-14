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