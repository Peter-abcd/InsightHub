CREATE DATABASE IF NOT EXISTS insighthub;

CREATE TABLE IF NOT EXISTS insighthub.behavior_event
(
    event_id String,
    user_id UInt32,
    event_type LowCardinality(String),

    entity_type Int32,
    entity_id UInt32,
    entity_user_id UInt32,
    target_id UInt32,
    post_id UInt32,

    keyword String,
    ip String,

    event_time DateTime,
    event_date Date DEFAULT toDate(event_time),

    data String
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, event_type, post_id, user_id, event_time);
