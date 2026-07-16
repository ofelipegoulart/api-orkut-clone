CREATE TABLE albums (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users (id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    privacy VARCHAR(20) NOT NULL,
    cover_photo_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE photos (
    id UUID PRIMARY KEY,
    album_id UUID NOT NULL REFERENCES albums (id),
    url VARCHAR(255) NOT NULL,
    caption TEXT,
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE albums
    ADD CONSTRAINT fk_albums_cover_photo FOREIGN KEY (cover_photo_id) REFERENCES photos (id);
