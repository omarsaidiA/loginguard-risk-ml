-- Operators: the analysts and administrators who use LoginGuard
CREATE TABLE operator (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'ANALYST')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_logins INT          NOT NULL DEFAULT 0,
    locked_until  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- The accounts whose logins are monitored
CREATE TABLE monitored_user (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(254) NOT NULL UNIQUE,
    department VARCHAR(50),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE login_event (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES monitored_user (id) ON DELETE CASCADE,
    occurred_at   TIMESTAMPTZ NOT NULL,
    ip_address    VARCHAR(45) NOT NULL,
    -- Validated by the API too; the database is the last line of defense
    country_code  VARCHAR(2)  NOT NULL CHECK (country_code ~ '^[A-Z]{2}$'),
    device_id     VARCHAR(64) NOT NULL,
    successful    BOOLEAN     NOT NULL,
    review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (review_status IN ('PENDING', 'CONFIRMED_FRAUD', 'LEGITIMATE')),
    reviewed_by   VARCHAR(50),
    reviewed_at   TIMESTAMPTZ
);

-- Risk features look at a user's recent history: serve those queries from an index
CREATE INDEX login_event_user_time ON login_event (user_id, occurred_at DESC);
