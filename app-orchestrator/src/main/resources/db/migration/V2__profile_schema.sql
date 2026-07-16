CREATE TABLE profile (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    headline TEXT,
    summary TEXT
);

CREATE TABLE experience (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profile (id) ON DELETE CASCADE,
    company TEXT NOT NULL,
    title TEXT NOT NULL,
    start_date DATE,
    end_date DATE,
    bullets TEXT[] NOT NULL DEFAULT '{}'
);

CREATE INDEX experience_profile_id_idx ON experience (profile_id);

CREATE TABLE skill (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profile (id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    level TEXT
);

CREATE INDEX skill_profile_id_idx ON skill (profile_id);

CREATE TABLE education (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profile (id) ON DELETE CASCADE,
    institution TEXT NOT NULL,
    degree TEXT,
    field TEXT,
    start_date DATE,
    end_date DATE
);

CREATE INDEX education_profile_id_idx ON education (profile_id);
