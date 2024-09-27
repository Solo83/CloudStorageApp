CREATE TABLE users
(
    id       BIGINT AUTO_INCREMENT NOT NULL,
    name     VARCHAR(255)          NULL,
    password VARCHAR(255)          NULL,
    `role`   VARCHAR(255)          NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
    );

ALTER TABLE users
    ADD CONSTRAINT uc_users_name UNIQUE (name);