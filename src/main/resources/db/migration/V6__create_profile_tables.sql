CREATE TABLE user_profile (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id)
);

CREATE TABLE user_profile_personal (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL UNIQUE REFERENCES user_profile (id),
    eye_color VARCHAR(255),
    hair_color VARCHAR(255),
    height VARCHAR(255),
    body_type VARCHAR(255),
    appearance VARCHAR(255),
    body_art VARCHAR(255),
    perfect_match TEXT,
    cant_stand TEXT,
    ideal_first_date TEXT,
    past_relationships_lessons TEXT,
    what_stands_out TEXT,
    favorite_body_part VARCHAR(255),
    five_essentials TEXT,
    in_my_room TEXT
);

CREATE TABLE user_profile_attractions (
    personal_id UUID NOT NULL REFERENCES user_profile_personal (id),
    attraction VARCHAR(255)
);

CREATE INDEX idx_user_profile_attractions_personal ON user_profile_attractions (personal_id);

CREATE TABLE user_profile_professional (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL UNIQUE REFERENCES user_profile (id),
    education VARCHAR(255),
    school VARCHAR(255),
    college VARCHAR(255),
    course VARCHAR(255),
    degree VARCHAR(255),
    graduation_year VARCHAR(255),
    profession VARCHAR(255),
    sector VARCHAR(255),
    company VARCHAR(255),
    job_description TEXT,
    work_phone VARCHAR(255),
    professional_skills TEXT,
    professional_interests TEXT
);

CREATE TABLE user_profile_contact (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL UNIQUE REFERENCES user_profile (id),
    primary_email VARCHAR(255),
    primary_email_privacy VARCHAR(20),
    im1 VARCHAR(255),
    im1privacy VARCHAR(20),
    im2 VARCHAR(255),
    im2privacy VARCHAR(20),
    home_phone VARCHAR(255),
    home_phone_privacy VARCHAR(20),
    mobile_phone VARCHAR(255),
    mobile_phone_privacy VARCHAR(20),
    address1 VARCHAR(255),
    address2 VARCHAR(255),
    address_city VARCHAR(255),
    address_state VARCHAR(255),
    address_zip_code VARCHAR(255),
    address_country VARCHAR(255)
);

CREATE TABLE user_profile_secondary_emails (
    contact_id UUID NOT NULL REFERENCES user_profile_contact (id),
    email VARCHAR(255),
    privacy VARCHAR(20)
);

CREATE INDEX idx_user_profile_secondary_emails_contact ON user_profile_secondary_emails (contact_id);

CREATE TABLE user_profile_social (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL UNIQUE REFERENCES user_profile (id),
    children VARCHAR(255),
    ethnicity VARCHAR(255),
    religion VARCHAR(255),
    political_view VARCHAR(255),
    sexual_orientation VARCHAR(255),
    sexual_orientation_privacy VARCHAR(20),
    smoking VARCHAR(255),
    drinking VARCHAR(255),
    pets VARCHAR(255),
    living_with VARCHAR(255),
    hometown VARCHAR(255),
    website VARCHAR(255),
    about_me TEXT,
    passions TEXT,
    sports TEXT,
    activities TEXT,
    books TEXT,
    music TEXT,
    tv_shows TEXT,
    movies TEXT,
    cuisines TEXT
);

CREATE TABLE user_profile_humor (
    social_id UUID NOT NULL REFERENCES user_profile_social (id),
    humor VARCHAR(255)
);

CREATE INDEX idx_user_profile_humor_social ON user_profile_humor (social_id);

CREATE TABLE user_profile_style (
    social_id UUID NOT NULL REFERENCES user_profile_social (id),
    style VARCHAR(255)
);

CREATE INDEX idx_user_profile_style_social ON user_profile_style (social_id);

CREATE TABLE user_profile_general (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL UNIQUE REFERENCES user_profile (id),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    gender VARCHAR(255),
    relationship_status VARCHAR(255),
    birth_month VARCHAR(255),
    birth_day VARCHAR(255),
    birth_date_privacy VARCHAR(20),
    birth_year VARCHAR(255),
    birth_year_privacy VARCHAR(20),
    city VARCHAR(255),
    state VARCHAR(255),
    zip_code VARCHAR(255),
    country VARCHAR(255),
    languages_privacy VARCHAR(20),
    high_school VARCHAR(255),
    high_school_privacy VARCHAR(20),
    college VARCHAR(255),
    college_privacy VARCHAR(20),
    company VARCHAR(255),
    company_privacy VARCHAR(20),
    dating_preference VARCHAR(255)
);

CREATE TABLE user_profile_languages (
    general_id UUID NOT NULL REFERENCES user_profile_general (id),
    language VARCHAR(255)
);

CREATE INDEX idx_user_profile_languages_general ON user_profile_languages (general_id);

CREATE TABLE user_profile_interested_in (
    general_id UUID NOT NULL REFERENCES user_profile_general (id),
    interest VARCHAR(255)
);

CREATE INDEX idx_user_profile_interested_in_general ON user_profile_interested_in (general_id);

CREATE TABLE profile_friends (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    friend_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_profile_friends_user_friend UNIQUE (user_id, friend_id)
);

CREATE TABLE profile_friend_requests (
    id UUID PRIMARY KEY,
    requester_id UUID NOT NULL REFERENCES users (id),
    receiver_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_profile_friend_requests_requester_receiver UNIQUE (requester_id, receiver_id)
);

CREATE TABLE profile_ratings (
    id UUID PRIMARY KEY,
    rater_id UUID NOT NULL REFERENCES users (id),
    target_id UUID NOT NULL REFERENCES users (id),
    legal_percentage DOUBLE PRECISION,
    trustworthy_percentage DOUBLE PRECISION,
    sexy_percentage DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_profile_ratings_rater_target UNIQUE (rater_id, target_id)
);

CREATE TABLE profile_statistics (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id),
    scraps_count BIGINT NOT NULL DEFAULT 0,
    friends_count BIGINT NOT NULL DEFAULT 0,
    communities_count BIGINT NOT NULL DEFAULT 0,
    testimonials_count BIGINT NOT NULL DEFAULT 0,
    fans_count BIGINT NOT NULL DEFAULT 0,
    photos_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE profile_testimonials (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES users (id),
    target_id UUID NOT NULL REFERENCES users (id),
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL
);
