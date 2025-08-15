-- Initial schema creation for Carden Flashcards Application
-- Version: 1.0.0
-- Description: Create all core tables with relationships, indexes, and constraints

-- Create topics table
CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_system_topic BOOLEAN DEFAULT FALSE,
    display_order INTEGER DEFAULT 0,
    parent_topic_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT fk_topics_parent FOREIGN KEY (parent_topic_id) REFERENCES topics(id)
);

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    profile_image_url VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    last_login TIMESTAMP,
    
    -- TTS Settings
    tts_enabled BOOLEAN DEFAULT TRUE,
    preferred_voice VARCHAR(255),
    speech_rate DOUBLE PRECISION DEFAULT 1.0,
    speech_pitch DOUBLE PRECISION DEFAULT 1.0,
    speech_volume DOUBLE PRECISION DEFAULT 1.0,
    
    -- User Preferences
    timezone VARCHAR(32) DEFAULT 'UTC',
    ui_language VARCHAR(10) DEFAULT 'en',
    learning_goal_cards_per_day INTEGER DEFAULT 20,
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

-- Create decks table
CREATE TABLE decks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    user_id BIGINT NOT NULL,
    topic_id BIGINT,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    cefr_level VARCHAR(2),
    source_language VARCHAR(10) DEFAULT 'en',
    target_language VARCHAR(10) DEFAULT 'vi',
    cover_image_url VARCHAR(500),
    tags JSONB,
    is_system_deck BOOLEAN DEFAULT FALSE,
    download_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    card_count INTEGER DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT fk_decks_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_decks_topic FOREIGN KEY (topic_id) REFERENCES topics(id),
    CONSTRAINT chk_decks_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC', 'UNLISTED')),
    CONSTRAINT chk_decks_cefr_level CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2'))
);

-- Create cards table
CREATE TABLE cards (
    id BIGSERIAL PRIMARY KEY,
    deck_id BIGINT NOT NULL,
    front VARCHAR(500) NOT NULL,
    back VARCHAR(500) NOT NULL,
    ipa_pronunciation VARCHAR(200),
    examples JSONB,
    synonyms JSONB,
    antonyms JSONB,
    tags JSONB,
    image_url VARCHAR(500),
    audio_url VARCHAR(500),
    unique_key VARCHAR(1000) NOT NULL,
    difficulty VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    display_order INTEGER DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT fk_cards_deck FOREIGN KEY (deck_id) REFERENCES decks(id),
    CONSTRAINT chk_cards_difficulty CHECK (difficulty IN ('EASY', 'NORMAL', 'HARD'))
);

-- Create study_states table
CREATE TABLE study_states (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    deck_id BIGINT NOT NULL,
    repetition_count INTEGER NOT NULL DEFAULT 0,
    ease_factor DOUBLE PRECISION NOT NULL DEFAULT 2.5,
    interval_days INTEGER NOT NULL DEFAULT 1,
    due_date TIMESTAMP NOT NULL,
    card_state VARCHAR(15) NOT NULL DEFAULT 'NEW',
    last_review_date TIMESTAMP,
    last_score INTEGER,
    total_reviews INTEGER DEFAULT 0,
    correct_reviews INTEGER DEFAULT 0,
    accuracy_rate DOUBLE PRECISION DEFAULT 0.0,
    
    -- New SRS fields for enhanced algorithm
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    current_learning_step INTEGER DEFAULT NULL,
    is_leech BOOLEAN NOT NULL DEFAULT FALSE,
    graduated_at TIMESTAMP DEFAULT NULL,
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT fk_study_states_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_study_states_card FOREIGN KEY (card_id) REFERENCES cards(id),
    CONSTRAINT fk_study_states_deck FOREIGN KEY (deck_id) REFERENCES decks(id),
    CONSTRAINT chk_study_states_ease_factor CHECK (ease_factor >= 1.3 AND ease_factor <= 3.0),
    CONSTRAINT chk_study_states_card_state CHECK (card_state IN ('NEW', 'LEARNING', 'REVIEW', 'RELEARNING')),
    CONSTRAINT uk_study_states_user_card UNIQUE (user_id, card_id)
);

-- Create review_sessions table
CREATE TABLE review_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    deck_id BIGINT,
    session_date TIMESTAMP NOT NULL,
    duration_minutes INTEGER,
    cards_studied INTEGER DEFAULT 0,
    cards_correct INTEGER DEFAULT 0,
    new_cards INTEGER DEFAULT 0,
    review_cards INTEGER DEFAULT 0,
    relearning_cards INTEGER DEFAULT 0,
    accuracy_rate DOUBLE PRECISION DEFAULT 0.0,
    study_mode VARCHAR(20) NOT NULL,
    session_status VARCHAR(15) DEFAULT 'IN_PROGRESS',
    session_stats JSONB,
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT fk_review_sessions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_review_sessions_deck FOREIGN KEY (deck_id) REFERENCES decks(id),
    CONSTRAINT chk_review_sessions_study_mode CHECK (study_mode IN ('FLIP', 'TYPE_ANSWER', 'MULTIPLE_CHOICE')),
    CONSTRAINT chk_review_sessions_status CHECK (session_status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED'))
);

-- Create jobs table
CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    job_data JSONB,
    result_data JSONB,
    progress_percentage INTEGER DEFAULT 0,
    status_message VARCHAR(500),
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT fk_jobs_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_jobs_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- Create reports table
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    reported_user_id BIGINT,
    reported_deck_id BIGINT,
    reported_card_id BIGINT,
    report_type VARCHAR(25) NOT NULL,
    reason TEXT,
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    additional_data JSONB,
    reviewed_by_id BIGINT,
    reviewed_at TIMESTAMP,
    admin_notes TEXT,
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_reports_reported_user FOREIGN KEY (reported_user_id) REFERENCES users(id),
    CONSTRAINT fk_reports_reported_deck FOREIGN KEY (reported_deck_id) REFERENCES decks(id),
    CONSTRAINT fk_reports_reported_card FOREIGN KEY (reported_card_id) REFERENCES cards(id),
    CONSTRAINT fk_reports_reviewed_by FOREIGN KEY (reviewed_by_id) REFERENCES users(id),
    CONSTRAINT chk_reports_type CHECK (report_type IN ('INAPPROPRIATE_CONTENT', 'COPYRIGHT_VIOLATION', 'SPAM', 'HARASSMENT', 'MISINFORMATION', 'OTHER')),
    CONSTRAINT chk_reports_status CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED'))
);

-- ========================================
-- INDEXES AND PERFORMANCE OPTIMIZATIONS
-- ========================================

-- Users table indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_active ON users(is_active);
CREATE INDEX idx_users_role ON users(role);

-- Topics table indexes
CREATE INDEX idx_topics_parent ON topics(parent_topic_id);
CREATE INDEX idx_topics_system ON topics(is_system_topic);
CREATE INDEX idx_topics_display_order ON topics(display_order);

-- Decks table indexes
CREATE INDEX idx_decks_user_id ON decks(user_id);
CREATE INDEX idx_decks_topic_id ON decks(topic_id);
CREATE INDEX idx_decks_visibility ON decks(visibility);
CREATE INDEX idx_decks_system ON decks(is_system_deck);
CREATE INDEX idx_decks_deleted ON decks(deleted);
CREATE INDEX idx_decks_deleted_at ON decks(deleted_at);
CREATE INDEX idx_decks_user_visibility ON decks(user_id, visibility);
CREATE INDEX idx_decks_public_only ON decks(visibility) WHERE visibility = 'PUBLIC';

-- JSONB indexes for decks tags
CREATE INDEX idx_decks_tags_gin ON decks USING GIN(tags);

-- Cards table indexes
CREATE INDEX idx_cards_deck_id ON cards(deck_id);
CREATE INDEX idx_cards_unique_key ON cards(unique_key);
CREATE INDEX idx_cards_deleted ON cards(deleted);
CREATE INDEX idx_cards_deleted_at ON cards(deleted_at);
CREATE INDEX idx_cards_difficulty ON cards(difficulty);
CREATE INDEX idx_cards_deck_order ON cards(deck_id, display_order);

-- JSONB indexes for cards
CREATE INDEX idx_cards_examples_gin ON cards USING GIN(examples);
CREATE INDEX idx_cards_synonyms_gin ON cards USING GIN(synonyms);
CREATE INDEX idx_cards_antonyms_gin ON cards USING GIN(antonyms);
CREATE INDEX idx_cards_tags_gin ON cards USING GIN(tags);

-- Full-text search indexes for cards
CREATE INDEX idx_cards_front_fts ON cards USING GIN(to_tsvector('english', front));
CREATE INDEX idx_cards_back_fts ON cards USING GIN(to_tsvector('english', back));

-- Study states table indexes
CREATE INDEX idx_study_states_user_card ON study_states(user_id, card_id);
CREATE INDEX idx_study_states_due_date ON study_states(due_date);
CREATE INDEX idx_study_states_user_due ON study_states(user_id, due_date);
CREATE INDEX idx_study_states_deck_id ON study_states(deck_id);
CREATE INDEX idx_study_states_card_state ON study_states(card_state);
CREATE INDEX idx_study_states_user_state ON study_states(user_id, card_state);

-- Indexes for new SRS fields
CREATE INDEX idx_study_states_is_leech ON study_states(user_id, is_leech) WHERE is_leech = TRUE;
CREATE INDEX idx_study_states_learning_step ON study_states(user_id, current_learning_step) WHERE current_learning_step IS NOT NULL;

-- Review sessions table indexes
CREATE INDEX idx_review_sessions_user ON review_sessions(user_id);
CREATE INDEX idx_review_sessions_date ON review_sessions(session_date);
CREATE INDEX idx_review_sessions_deck ON review_sessions(deck_id);
CREATE INDEX idx_review_sessions_status ON review_sessions(session_status);
CREATE INDEX idx_review_sessions_study_mode ON review_sessions(study_mode);
CREATE INDEX idx_review_sessions_user_date ON review_sessions(user_id, session_date);

-- Jobs table indexes
CREATE INDEX idx_jobs_user ON jobs(user_id);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_type ON jobs(job_type);
CREATE INDEX idx_jobs_created_at ON jobs(created_at);
CREATE INDEX idx_jobs_user_status ON jobs(user_id, status);

-- Reports table indexes
CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_type ON reports(report_type);
CREATE INDEX idx_reports_reported_user ON reports(reported_user_id);
CREATE INDEX idx_reports_reported_deck ON reports(reported_deck_id);
CREATE INDEX idx_reports_reported_card ON reports(reported_card_id);
CREATE INDEX idx_reports_reviewed_by ON reports(reviewed_by_id);

-- Composite indexes for common queries
CREATE INDEX idx_decks_user_status ON decks(user_id, deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_cards_deck_status ON cards(deck_id, deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_study_states_due_cards ON study_states(user_id, due_date, card_state);
