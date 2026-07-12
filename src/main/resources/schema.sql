-- Risk AI RAgent schema (MySQL 8.x)

CREATE TABLE IF NOT EXISTS ragent_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    trace_id        VARCHAR(64)  NOT NULL                COMMENT 'Request trace id',
    question        TEXT         NOT NULL                COMMENT 'User question',
    answer          MEDIUMTEXT            DEFAULT NULL   COMMENT 'Model answer',
    reference_docs  MEDIUMTEXT            DEFAULT NULL   COMMENT 'Retrieved reference snippets (JSON)',
    matched_count   INT          NOT NULL DEFAULT 0      COMMENT 'Number of retrieved chunks',
    hit_cache       TINYINT      NOT NULL DEFAULT 0      COMMENT '1 = served from cache',
    degraded        TINYINT      NOT NULL DEFAULT 0      COMMENT '1 = degraded / fallback answer',
    status          VARCHAR(16)  NOT NULL DEFAULT 'OK'   COMMENT 'OK / FAILED',
    error_msg       VARCHAR(512)          DEFAULT NULL   COMMENT 'Error message when failed',
    cost_ms         BIGINT       NOT NULL DEFAULT 0      COMMENT 'Latency in ms',
    client_ip       VARCHAR(64)           DEFAULT NULL   COMMENT 'Client IP',
    deleted         TINYINT      NOT NULL DEFAULT 0      COMMENT 'Logic delete flag',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_trace (trace_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAgent interaction logs';

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL,
    password    VARCHAR(128) NOT NULL,
    nickname    VARCHAR(64)           DEFAULT NULL,
    email       VARCHAR(128)          DEFAULT NULL,
    role        VARCHAR(16)  NOT NULL DEFAULT 'USER',
    status      TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System users';

CREATE TABLE IF NOT EXISTS sys_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(512)          DEFAULT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Document categories';

CREATE TABLE IF NOT EXISTS sys_document (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    doc_id        VARCHAR(64)  NOT NULL COMMENT 'Vector store document id',
    file_name     VARCHAR(255) NOT NULL,
    category_id   BIGINT                DEFAULT NULL,
    file_size     BIGINT       NOT NULL DEFAULT 0,
    char_count    INT          NOT NULL DEFAULT 0,
    token_count   INT          NOT NULL DEFAULT 0,
    chunk_count   INT          NOT NULL DEFAULT 0,
    status        VARCHAR(32)  NOT NULL DEFAULT 'SUCCESS',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_category (category_id),
    KEY idx_doc_id (doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Uploaded documents';

CREATE TABLE IF NOT EXISTS chat_session (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL DEFAULT '新对话',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User chat sessions';

CREATE TABLE IF NOT EXISTS chat_message (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    session_id      BIGINT       NOT NULL,
    question        TEXT         NOT NULL,
    answer          MEDIUMTEXT            DEFAULT NULL,
    reference_docs  MEDIUMTEXT            DEFAULT NULL,
    from_cache      TINYINT      NOT NULL DEFAULT 0,
    degraded        TINYINT      NOT NULL DEFAULT 0,
    cost_ms         BIGINT       NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Chat messages';

CREATE TABLE IF NOT EXISTS a2a_agent (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    agent_id      VARCHAR(64)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(500)          DEFAULT NULL,
    capabilities  TEXT         NOT NULL,
    endpoint      VARCHAR(255) NOT NULL,
    status        VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A2A agents';

CREATE TABLE IF NOT EXISTS a2a_task (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    task_id          VARCHAR(64)  NOT NULL,
    from_agent_id    VARCHAR(64)  NOT NULL,
    to_agent_id      VARCHAR(64)  NOT NULL,
    message          TEXT         NOT NULL,
    status           VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    detail_message   VARCHAR(512)          DEFAULT NULL,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_id (task_id),
    KEY idx_from_agent (from_agent_id),
    KEY idx_to_agent (to_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A2A tasks';
