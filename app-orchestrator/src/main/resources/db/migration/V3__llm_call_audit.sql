CREATE TABLE llm_call_audit (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    llm_task TEXT NOT NULL,
    provider TEXT NOT NULL,
    model TEXT NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    duration_ms BIGINT NOT NULL,
    ok BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX llm_call_audit_user_id_idx ON llm_call_audit (user_id);
