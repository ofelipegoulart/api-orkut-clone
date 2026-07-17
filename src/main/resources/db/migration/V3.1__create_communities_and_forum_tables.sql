CREATE TABLE communities (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(255),
    language VARCHAR(255),
    category VARCHAR(50),
    type VARCHAR(20) NOT NULL,
    content_privacy VARCHAR(30) NOT NULL,
    location_city VARCHAR(255),
    location_state VARCHAR(255),
    location_zip_code VARCHAR(255),
    location_country VARCHAR(255),
    forum_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    forum_on_homepage BOOLEAN NOT NULL DEFAULT TRUE,
    forum_no_anonymous_posts BOOLEAN NOT NULL DEFAULT TRUE,
    polls_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    polls_on_homepage BOOLEAN NOT NULL DEFAULT TRUE,
    events_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    events_on_homepage BOOLEAN NOT NULL DEFAULT FALSE,
    custom_news_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    owner_id UUID REFERENCES users (id),
    members_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE community_memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    community_id UUID NOT NULL REFERENCES communities (id),
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_community_memberships_user_community UNIQUE (user_id, community_id)
);

CREATE TABLE community_topics (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities (id),
    title VARCHAR(255) NOT NULL,
    author_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_community_topics_community ON community_topics (community_id);

CREATE TABLE topic_messages (
    id UUID PRIMARY KEY,
    topic_id UUID NOT NULL REFERENCES community_topics (id),
    author_id UUID NOT NULL REFERENCES users (id),
    subject VARCHAR(255) NOT NULL,
    message VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_topic_messages_topic_created ON topic_messages (topic_id, created_at);
