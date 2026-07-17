CREATE TABLE polls (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities (id),
    question VARCHAR(280) NOT NULL,
    description TEXT,
    image_url VARCHAR(512),
    creator_id UUID NOT NULL REFERENCES users (id),
    closes_at TIMESTAMP,
    anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    multiple_choice BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_polls_community ON polls (community_id);

CREATE TABLE poll_options (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL REFERENCES polls (id),
    text VARCHAR(140) NOT NULL,
    order_index INTEGER NOT NULL
);

CREATE INDEX idx_poll_options_poll ON poll_options (poll_id);

CREATE TABLE poll_votes (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL REFERENCES polls (id),
    option_id UUID NOT NULL REFERENCES poll_options (id),
    voter_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_poll_votes_poll_voter_option UNIQUE (poll_id, voter_id, option_id)
);

CREATE TABLE poll_comments (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL REFERENCES polls (id),
    author_id UUID NOT NULL REFERENCES users (id),
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_poll_comments_poll_created ON poll_comments (poll_id, created_at);
