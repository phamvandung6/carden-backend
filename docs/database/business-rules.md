# Quy tắc nghiệp vụ - Carden Database

## 1. Quản lý User và Authentication

### 1.1 Đăng ký và Xác thực
- **Username**: Duy nhất, 3-50 ký tự, chỉ chứa alphanumeric và underscore
- **Email**: Duy nhất, phải valid format, required verification
- **Password**: Minimum 8 ký tự, hash bằng BCrypt với strength 12
- **Email Verification**: User chỉ có thể login sau khi verify email
- **Account Activation**: `is_active = false` sẽ disable toàn bộ access

### 1.2 Roles và Permissions
- **USER Role**: 
  - Tạo/edit/delete deck của mình
  - Study tất cả public decks
  - Report nội dung vi phạm
  - Không thể access admin endpoints
- **ADMIN Role**:
  - Tất cả permissions của USER
  - Quản lý system decks và topics
  - Review và xử lý reports
  - Xem thống kê system-wide
  - Manage user accounts (suspend/activate)

### 1.3 User Preferences
- **TTS Settings**: Individual voice preferences, speed, pitch, volume
- **Learning Goals**: Default 20 cards/day, có thể customize
- **UI Language**: Support EN/VI, ảnh hưởng đến full-text search
- **Timezone**: Quan trọng cho due date calculations; hỗ trợ timezone names như "Asia/Ho_Chi_Minh" (VARCHAR(32))
- **Profile Image**: Ảnh đại diện lưu trên Cloudflare R2; chỉ chấp nhận PNG/JPEG/GIF, max 5MB; sử dụng presigned URL cho upload từ frontend

---

## 2. Deck và Card Management

### 2.1 Deck Ownership và Visibility
- **Private Decks**: Chỉ owner mới access được
- **Public Decks**: Visible trong marketplace, có thể copy/download
- **Unlisted Decks**: Access qua direct link, không show trong search
- **System Decks**: Tạo bởi admin, không thể delete, visible cho all users

### 2.2 Deck Operations
- **Creation**: User chỉ có thể tạo deck với `user_id = current_user.id`
- **Soft Delete**: `deleted_at IS NOT NULL`, deck vẫn tồn tại nhưng hidden
- **Card Count**: Auto-update khi add/remove cards
- **Topic Assignment**: Optional, có thể null

### 2.3 Card Unique Key Generation
```
unique_key = normalize(front) + ":" + normalize(back)
normalize(text) = lowercase(trim(replace_multiple_spaces(text)))
```
- **Purpose**: Detect duplicates trong cùng deck
- **Update**: Auto-generate trong @PrePersist và @PreUpdate
- **Conflict Resolution**: Manual review cho duplicates

### 2.4 Card Difficulty và Metadata
- **Difficulty Levels**: EASY, NORMAL, HARD (ảnh hưởng đến SRS algorithm)
- **IPA Pronunciation**: Optional, format validation
- **JSONB Arrays**: examples, synonyms, antonyms, tags
- **Multimedia**: image_url và audio_url với file size limits

---

## 3. Spaced Repetition System (SRS) Algorithm

### 3.1 Study State Lifecycle
```
NEW → LEARNING → REVIEW
      ↑         ↓
   RELEARNING ←─┘
```

### 3.2 Scoring System (0-3 Scale)
- **0 (Again)**: Completely forgot, back to LEARNING
- **1 (Hard)**: Remembered with difficulty, shorter interval
- **2 (Good)**: Remembered correctly, normal interval
- **3 (Easy)**: Too easy, longer interval, boost ease factor

### 3.3 Interval Calculation
```
NEW → LEARNING: 1 day
LEARNING:
  - Score 0,1: Repeat in 10 minutes
  - Score 2,3: Move to REVIEW with 1 day interval

REVIEW:
  - Score 0: Back to RELEARNING, interval = 1 day
  - Score 1: interval = interval * ease_factor * 0.85
  - Score 2: interval = interval * ease_factor
  - Score 3: interval = interval * ease_factor * 1.15
```

### 3.4 Ease Factor Management
- **Initial**: 2.5 for new cards
- **Range**: 1.3 - 2.5 (capped)
- **Adjustment**:
  - Score 0: ease_factor -= 0.2
  - Score 1: ease_factor -= 0.15
  - Score 2: No change
  - Score 3: ease_factor += 0.1

### 3.5 Due Date Calculation
```
due_date = last_review_date + (interval_days * 24 hours)
```
- **Timezone**: Use user's timezone for due date display (e.g., "Asia/Ho_Chi_Minh", "America/New_York")
- **Storage**: Timezone names stored as VARCHAR(32) to accommodate full IANA timezone identifiers
- **Calculation**: Due dates computed in user's local timezone for accurate "today"/"tomorrow" logic
- **Batch Learning**: Cards due in same hour grouped together

---

## 4. Review Sessions và Analytics

### 4.1 Session Management
- **Start Session**: Create với `session_status = 'IN_PROGRESS'`
- **End Session**: Update status to 'COMPLETED', calculate final stats
- **Abandon Session**: Status 'ABANDONED' if inactive > 30 minutes
- **Mixed Deck Sessions**: `deck_id = NULL` for cross-deck study

### 4.2 Study Modes
- **FLIP Mode**: Self-graded, traditional flashcard
- **TYPE_ANSWER Mode**: Fuzzy string matching với 85% threshold
- **MULTIPLE_CHOICE Mode**: Auto-generate distractors from deck

### 4.3 Session Statistics
```json
{
  "averageResponseTime": 1500, // milliseconds per card
  "scoreDistribution": [2, 5, 8, 3], // [again, hard, good, easy] counts
  "totalTimeSpent": 1800, // seconds including pauses
  "pauseCount": 3, // number of pauses > 30 seconds
  "difficultyRating": 7.5 // user subjective rating 1-10
}
```

### 4.4 Performance Analytics
- **Accuracy Rate**: `correct_reviews / total_reviews * 100`
- **Study Streak**: Consecutive days with completed sessions
- **Cards Per Day**: Average from last 30 days
- **Weak Cards**: Cards with accuracy < 60% and > 5 reviews

---

## 5. Background Jobs và Processing

### 5.1 Job Types
- **AI_DECK_GENERATION**: OpenAI-powered deck creation
- **DATA_IMPORT**: CSV/JSON import với validation
- **DATA_EXPORT**: User data export compliance
- **ANALYTICS_CALCULATION**: Heavy analytics processing
- **EMAIL_NOTIFICATION**: Batch email sending

### 5.2 Job Lifecycle
```
PENDING → RUNNING → COMPLETED
    ↓         ↓
  CANCELLED  FAILED → RETRY (if retry_count < max_retries)
```

### 5.3 Error Handling và Retry
- **Max Retries**: 3 attempts with exponential backoff
- **Retry Delay**: 2^retry_count minutes
- **Dead Letter Queue**: Failed jobs after max retries
- **User Notification**: Email on completion/failure

---

## 6. Content Moderation và Reports

### 6.1 Report Types và Workflow
- **INAPPROPRIATE_CONTENT**: Adult content, violence, etc.
- **COPYRIGHT_VIOLATION**: Copyrighted material usage
- **SPAM**: Irrelevant content, promotional spam
- **HARASSMENT**: Personal attacks, bullying
- **MISINFORMATION**: False information, conspiracy theories

### 6.2 Report Processing
```
User Report → PENDING → UNDER_REVIEW → RESOLVED/DISMISSED
                ↓            ↓
             Auto-filter   Manual Review
```

### 6.3 Moderation Actions
- **Content Removal**: Soft delete reported content
- **User Warning**: Email notification về violation
- **Temporary Suspension**: `is_active = false` for period
- **Permanent Ban**: Account deletion với data retention
- **False Report**: Warning cho reporter về abuse

### 6.4 Automated Moderation
- **Duplicate Reports**: Auto-escalate nếu > 3 reports cho same content
- **Keyword Filter**: Auto-flag nội dung có banned keywords
- **Image Recognition**: AI scan cho inappropriate images
- **Rate Limiting**: Max 5 reports/user/day để prevent abuse

---

## 7. Data Integrity và Consistency

### 7.1 Referential Integrity
- **Cascade Delete Rules**:
  - User deleted → Soft delete all decks
  - Deck deleted → Soft delete all cards
  - Card deleted → Keep study states for analytics
- **Orphan Prevention**: Foreign keys với proper constraints

### 7.2 Data Validation
- **Email Format**: RFC 5322 compliant
- **URL Validation**: image_url và audio_url must be valid URLs
- **JSONB Schema**: Validate array structure cho examples, tags, etc.
- **Enum Values**: Strict validation cho status fields

### 7.3 Concurrency Control
- **Optimistic Locking**: Version field cho conflict detection
- **Study State Updates**: Atomic updates để prevent race conditions
- **Session Management**: Handle concurrent sessions properly

### 7.4 Data Retention
- **Soft Deletes**: Retain for 90 days before hard delete
- **Session Data**: Keep indefinitely for analytics
- **Job Logs**: Retain for 30 days
- **Report Data**: Retain for 1 year for compliance

---

## 8. Performance và Scaling Rules

### 8.1 Query Optimization
- **Index Usage**: Mandatory cho all foreign keys và search fields
- **Pagination**: Maximum 100 records per request
- **N+1 Prevention**: Use JOIN FETCH cho related entities
- **Query Timeout**: 30 seconds maximum per query

### 8.2 Caching Strategy
- **Redis Cache**: User sessions, frequently accessed decks
- **Application Cache**: Static data như topics, system decks
- **CDN**: Images và audio files
- **Cache Invalidation**: Event-driven cho data consistency

### 8.3 Rate Limiting
- **API Endpoints**: 100 requests/minute per user
- **Study Sessions**: Max 1 concurrent session per user
- **Report Submission**: Max 5 reports/day per user
- **Deck Creation**: Max 10 decks/day for new users

---

*Tài liệu này định nghĩa business rules để ensure data consistency và proper application behavior*
