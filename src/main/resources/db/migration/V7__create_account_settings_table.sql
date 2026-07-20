CREATE TABLE account_settings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id),
    language VARCHAR(20) NOT NULL DEFAULT 'pt-BR',
    birthday_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    content_filter VARCHAR(20) NOT NULL DEFAULT 'HIDE_IMPROPER',
    friend_updates_scope VARCHAR(20) NOT NULL DEFAULT 'ALL_FRIENDS',
    slow_internet_mode VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    suppress_slow_internet_warning BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE account_settings_profile_features (
    settings_id UUID NOT NULL REFERENCES account_settings (id),
    feature VARCHAR(255)
);

CREATE INDEX idx_account_settings_profile_features_settings ON account_settings_profile_features (settings_id);
