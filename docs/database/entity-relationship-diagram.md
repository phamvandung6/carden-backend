# Sơ đồ quan hệ thực thể (ERD) - Carden Database

## Sơ đồ tổng quan

```mermaid
erDiagram
    users {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password
        varchar display_name
         varchar profile_image_url
        varchar role
        boolean is_active
        boolean email_verified
        timestamp last_login
        boolean tts_enabled
        varchar preferred_voice
        double speech_rate
        double speech_pitch
        double speech_volume
        varchar timezone
        varchar ui_language
        integer learning_goal_cards_per_day
        timestamp created_at
        timestamp updated_at
        bigint version
    }

    topics {
        bigint id PK
        varchar name
        varchar description
        boolean is_system_topic
        integer display_order
        bigint parent_topic_id FK
        timestamp created_at
        timestamp updated_at
        bigint version
    }

    decks {
        bigint id PK
        varchar title
        varchar description
        bigint user_id FK
        bigint topic_id FK
        varchar visibility
        varchar cefr_level
        varchar source_language
        varchar target_language
        varchar cover_image_url
        jsonb tags
        boolean is_system_deck
        bigint download_count
        bigint like_count
        integer card_count
        boolean deleted
        timestamp deleted_at
        timestamp created_at
        timestamp updated_at
        bigint version
    }

    cards {
        bigint id PK
        bigint deck_id FK
        varchar front
        varchar back
        varchar ipa_pronunciation
        jsonb examples
        jsonb synonyms
        jsonb antonyms
        jsonb tags
        varchar image_url
        varchar audio_url
        varchar unique_key
        varchar difficulty
        integer display_order
        boolean deleted
        timestamp deleted_at
        timestamp created_at
        timestamp updated_at
        bigint version
    }

    study_states {
        bigint id PK
        bigint user_id FK
        bigint card_id FK
        bigint deck_id FK
        integer repetition_count
        double ease_factor
        integer interval_days
        timestamp due_date
        varchar card_state
        timestamp last_review_date
        integer last_score
        integer total_reviews
        integer correct_reviews
        double accuracy_rate
        integer consecutive_failures
        integer current_learning_step
        boolean is_leech
        timestamp graduated_at
        timestamp created_at
        timestamp updated_at
        bigint version
    }

    review_sessions {
        bigint id PK
        bigint user_id FK
        bigint deck_id FK
        timestamp session_date
        integer duration_minutes
        integer cards_studied
        integer cards_correct
        integer new_cards
        integer review_cards
        integer relearning_cards
        double accuracy_rate
        varchar study_mode
        varchar session_status
        jsonb session_stats
        timestamp created_at
        timestamp updated_at
        bigint version
    }

    jobs {
        bigint id PK
        bigint user_id FK
        varchar job_type
        varchar status
        jsonb job_data
        jsonb result_data
        integer progress_percentage
        varchar status_message
        text error_message
        timestamp started_at
        timestamp completed_at
        integer retry_count
        integer max_retries
        timestamp created_at
        timestamp updated_at
        bigint version
    }

    reports {
        bigint id PK
        bigint reporter_id FK
        bigint reported_user_id FK
        bigint reported_deck_id FK
        bigint reported_card_id FK
        varchar report_type
        text reason
        varchar status
        jsonb additional_data
        bigint reviewed_by_id FK
        timestamp reviewed_at
        text admin_notes
        timestamp created_at
        timestamp updated_at
        bigint version
    }

    %% Relationships
    users ||--o{ decks : "owns"
    users ||--o{ study_states : "has"
    users ||--o{ review_sessions : "participates"
    users ||--o{ jobs : "requests"
    users ||--o{ reports : "reports"
    users ||--o{ reports : "is_reported"
    users ||--o{ reports : "reviews"
    
    topics ||--o{ topics : "parent_child"
    topics ||--o{ decks : "categorizes"
    
    decks ||--o{ cards : "contains"
    decks ||--o{ study_states : "tracks"
    decks ||--o{ review_sessions : "studied_in"
    decks ||--o{ reports : "is_reported"
    
    cards ||--o{ study_states : "is_studied"
    cards ||--o{ reports : "is_reported"
```

## Mô tả các mối quan hệ

### 1. User - Deck (1:N)
- **Mối quan hệ**: Một user có thể tạo nhiều deck
- **Foreign Key**: `decks.user_id -> users.id`
- **Ràng buộc**: NOT NULL (deck phải có owner)
- **Cascade**: Khi user bị xóa, các deck của user sẽ được xử lý theo business rule

### 2. Topic - Topic (1:N) - Self Reference
- **Mối quan hệ**: Một topic có thể có nhiều sub-topics
- **Foreign Key**: `topics.parent_topic_id -> topics.id`
- **Ràng buộc**: NULLABLE (topic gốc không có parent)
- **Cascade**: Khi parent topic bị xóa, sub-topics có thể được reassign hoặc xóa

### 3. Topic - Deck (1:N)
- **Mối quan hệ**: Một topic có thể chứa nhiều deck
- **Foreign Key**: `decks.topic_id -> topics.id`
- **Ràng buộc**: NULLABLE (deck có thể không có topic)
- **Cascade**: Khi topic bị xóa, deck vẫn tồn tại với topic_id = NULL
- **Visibility**: Deck có thể có visibility PRIVATE, PUBLIC, hoặc UNLISTED

### 4. Deck - Card (1:N)
- **Mối quan hệ**: Một deck chứa nhiều card
- **Foreign Key**: `cards.deck_id -> decks.id`
- **Ràng buộc**: NOT NULL (card phải thuộc về một deck)
- **Cascade**: Khi deck bị soft delete, cards cũng bị soft delete

### 5. User - StudyState (1:N)
- **Mối quan hệ**: Một user có nhiều study state cho các cards khác nhau
- **Foreign Key**: `study_states.user_id -> users.id`
- **Ràng buộc**: NOT NULL
- **Unique Constraint**: (user_id, card_id) - mỗi user chỉ có 1 study state per card

### 6. Card - StudyState (1:N)
- **Mối quan hệ**: Một card có nhiều study state từ các users khác nhau
- **Foreign Key**: `study_states.card_id -> cards.id`
- **Ràng buộc**: NOT NULL
- **Business Rule**: Study state chỉ tồn tại khi user đã học card đó

### 7. Deck - StudyState (1:N)
- **Mối quan hệ**: Để denormalize và tối ưu queries
- **Foreign Key**: `study_states.deck_id -> decks.id`
- **Ràng buộc**: NOT NULL
- **Consistency**: Phải match với deck_id của card tương ứng

### 8. User - ReviewSession (1:N)
- **Mối quan hệ**: Một user có nhiều review sessions
- **Foreign Key**: `review_sessions.user_id -> users.id`
- **Ràng buộc**: NOT NULL
- **Timeline**: Ordered by session_date cho historical tracking

### 9. Deck - ReviewSession (1:N)
- **Mối quan hệ**: Một deck có thể được study trong nhiều sessions
- **Foreign Key**: `review_sessions.deck_id -> decks.id`
- **Ràng buộc**: NULLABLE (mixed-deck sessions)
- **Analytics**: Cho deck-specific performance tracking

### 10. User - Job (1:N)
- **Mối quan hệ**: Một user có thể có nhiều background jobs
- **Foreign Key**: `jobs.user_id -> users.id`
- **Ràng buộc**: NOT NULL
- **Queue**: Jobs được process theo creation order và priority

### 11. User - Report (1:N) - Reporter
- **Mối quan hệ**: Một user có thể tạo nhiều reports
- **Foreign Key**: `reports.reporter_id -> users.id`
- **Ràng buộc**: NOT NULL
- **Business Rule**: User không thể report chính mình

### 12. User - Report (1:N) - Reported User
- **Mối quan hệ**: Một user có thể bị nhiều users report
- **Foreign Key**: `reports.reported_user_id -> users.id`
- **Ràng buộc**: NULLABLE
- **Conflict**: Check constraint để ensure reporter != reported_user

### 13. User - Report (1:N) - Reviewer
- **Mối quan hệ**: Một admin user có thể review nhiều reports
- **Foreign Key**: `reports.reviewed_by_id -> users.id`
- **Ràng buộc**: NULLABLE (pending reports)
- **Authorization**: Chỉ ADMIN role mới có thể review

### 14. Deck - Report (1:N)
- **Mối quan hệ**: Một deck có thể bị report nhiều lần
- **Foreign Key**: `reports.reported_deck_id -> decks.id`
- **Ràng buộc**: NULLABLE
- **Business Rule**: Chỉ public decks mới có thể bị report

### 15. Card - Report (1:N)
- **Mối quan hệ**: Một card có thể bị report về nội dung không phù hợp
- **Foreign Key**: `reports.reported_card_id -> cards.id`
- **Ràng buộc**: NULLABLE
- **Context**: Report card thường đi kèm với deck context

## Ràng buộc toàn vẹn dữ liệu

### Check Constraints
- **users.role**: IN ('USER', 'ADMIN')
- **decks.visibility**: IN ('PRIVATE', 'PUBLIC', 'UNLISTED')
- **decks.cefr_level**: IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')
- **cards.difficulty**: IN ('EASY', 'NORMAL', 'HARD')
- **study_states.ease_factor**: BETWEEN 1.3 AND 3.0
- **study_states.card_state**: IN ('NEW', 'LEARNING', 'REVIEW', 'RELEARNING')
- **review_sessions.study_mode**: IN ('FLIP', 'TYPE_ANSWER', 'MULTIPLE_CHOICE')
- **review_sessions.session_status**: IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')
- **jobs.status**: IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')
- **reports.report_type**: IN ('INAPPROPRIATE_CONTENT', 'COPYRIGHT_VIOLATION', 'SPAM', 'HARASSMENT', 'MISINFORMATION', 'OTHER')
- **reports.status**: IN ('PENDING', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')

### Unique Constraints
- **users**: (username), (email)
- **study_states**: (user_id, card_id)

---

*Sơ đồ này được generate từ database schema V1 và sẽ được cập nhật theo các migrations tiếp theo*
