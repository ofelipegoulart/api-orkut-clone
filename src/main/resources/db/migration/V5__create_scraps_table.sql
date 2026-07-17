CREATE TABLE scraps (
    id UUID PRIMARY KEY,
    content VARCHAR(255) NOT NULL,
    author_id UUID NOT NULL REFERENCES users (id),
    owner_id UUID NOT NULL REFERENCES users (id),
    parent_id UUID REFERENCES scraps (id),
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_scrap_owner_read ON scraps (owner_id, read_at);
