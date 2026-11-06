-- Features computed from the user's history at the time of the login, and the model's verdict.
-- Features are stored so every score can be explained later and audited against the model version.
ALTER TABLE login_event
    ADD COLUMN failed_attempts_1h     INT CHECK (failed_attempts_1h >= 0),
    ADD COLUMN new_device             BOOLEAN,
    ADD COLUMN new_country            BOOLEAN,
    ADD COLUMN rapid_country_change   BOOLEAN,
    ADD COLUMN night_time             BOOLEAN,
    ADD COLUMN hours_since_last_login DOUBLE PRECISION CHECK (hours_since_last_login >= 0),
    ADD COLUMN risk_score             DOUBLE PRECISION CHECK (risk_score BETWEEN 0 AND 1),
    ADD COLUMN risk_level             VARCHAR(10) CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    ADD COLUMN model_version          VARCHAR(20);

-- The analysts' work queue: high-risk events still pending review
CREATE INDEX login_event_risk_review ON login_event (risk_level, review_status);
