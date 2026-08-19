CREATE TABLE entries (
    id BIGINT NOT NULL PRIMARY KEY,
    content varchar(2048),
    description varchar(255)
);

CREATE TABLE entry_id_generator (
    sequence_name varchar(255) NOT NULL PRIMARY KEY,
    next_id BIGINT NOT NULL
);

INSERT INTO entry_id_generator (sequence_name, next_id) VALUES ('entries', 0);
