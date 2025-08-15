# Luồng Practice & SRS (Spaced Repetition System) - Carden API

## Tổng quan

Hệ thống học tập thông minh với thuật toán SRS dựa trên SM-2/Anki, theo dõi tiến độ và tối ưu hóa quá trình ghi nhớ. Hệ thống tự động tạo StudyState cho card mới khi được practice lần đầu.

## Workflow Cơ Bản

### 1. Bắt đầu Practice Session
**Endpoint:** `POST /api/v1/practice/sessions`

**Request Body:**
```json
{
  "studyMode": "FLIP",           // FLIP | TYPE_ANSWER | MULTIPLE_CHOICE  
  "deckId": 3,                   // Optional: null = all decks
  "maxNewCards": 10,             // Giới hạn card mới per session
  "maxReviewCards": 200,         // Giới hạn card review per session
  "includeNewCards": true,       // Bao gồm card mới
  "includeReviewCards": true,    // Bao gồm card cần review
  "includeLearningCards": true   // Bao gồm card đang học
}
```

### 2. Lấy Session Hiện Tại
**Endpoint:** `GET /api/v1/practice/sessions/current`

### 3. Lấy Card Tiếp Theo
**Endpoint:** `GET /api/v1/practice/next-card?deckId=3`

**Logic Ưu Tiên:**
1. **Learning cards** (due) - Card đang học và đến hạn
2. **Review cards** (due) - Card cần ôn tập  
3. **New cards** (StudyState = NEW) - Card đã có StudyState
4. **Cards without StudyState** - Card thực sự mới chưa học bao giờ

### 4. Submit Review (Đánh giá Card)
**Endpoint:** `POST /api/v1/practice/cards/{cardId}/review`

**Request Body:**
```json
{
  "grade": 2,                    // 0-3: Again, Hard, Good, Easy
  "responseTimeMs": 3000,        // Thời gian phản hồi
  "showAnswer": false,           // User có reveal answer sớm không
  "userAnswer": "Xin chào"       // Câu trả lời (cho TYPE_ANSWER mode)
}
```

**Grade System:**
- **0 (Again)**: Quên hoàn toàn → Reset learning, giảm ease factor
- **1 (Hard)**: Khó nhớ → Giữ learning step hoặc interval thấp  
- **2 (Good)**: Nhớ bình thường → Tiến tới step tiếp hoặc interval chuẩn
- **3 (Easy)**: Quá dễ → Graduate ngay hoặc interval dài, tăng ease factor

### 5. Hoàn Thành Session
**Endpoint:** `POST /api/v1/practice/sessions/{sessionId}/complete`

## Endpoints Truy Vấn

### 1. Due Cards
**Endpoint:** `GET /api/v1/practice/due-cards?deckId=3&page=0&size=20`

### 2. New Cards  
**Endpoint:** `GET /api/v1/practice/cards/new?deckId=3&page=0&size=20`

### 3. Learning Cards
**Endpoint:** `GET /api/v1/practice/cards/learning?deckId=3`

### 4. Due Cards Count
**Endpoint:** `GET /api/v1/practice/cards/due-count?deckId=3`

## Endpoints Analytics & Thống Kê

**Lưu ý:** Các endpoint thống kê đã được chuyển sang `/api/v1/analytics` để tổ chức tốt hơn.

### 1. User Statistics (Tổng Quan)
**Endpoint:** `GET /api/v1/analytics/overview`

**Thông tin trả về:**
- Total cards studied, sessions completed
- Overall accuracy, current streak  
- Card state distribution (NEW/LEARNING/REVIEW)
- Recent activity, study efficiency

### 2. Simplified Statistics (Dashboard)
**Endpoint:** `GET /api/v1/analytics/overview/simplified`

### 3. Performance Statistics
**Endpoint:** `GET /api/v1/analytics/performance?period=30d`

**Parameters:**
- `period`: 7d, 30d, 90d, 1y
- `startDate`, `endDate`: Custom date range (ISO format)

### 4. Study Streaks
**Endpoint:** `GET /api/v1/analytics/streaks`

### 5. Deck-Specific Statistics
**Endpoint:** `GET /api/v1/analytics/deck/{deckId}`

### 6. Analytics Summary (Dashboard)
**Endpoint:** `GET /api/v1/analytics/summary`

### 7. Study Insights
**Endpoint:** `GET /api/v1/analytics/insights`

**Trả về:** AI-generated insights và recommendations dựa trên study patterns

## SRS Algorithm Chi Tiết

### Card States
- **NEW**: Card chưa học lần nào, StudyState được tạo khi review lần đầu
- **LEARNING**: Card đang trong quá trình học (new hoặc failed)
- **REVIEW**: Card đã "graduate", ôn tập theo schedule
- **RELEARNING**: Card review bị failed, học lại

### Ease Factor (Hệ số dễ nhớ)
- **Range**: 1.3 - 3.0 (verified qua testing)
- **Initial**: 2.5 cho card mới
- **Adjustment**:
  - Grade 0: -0.2
  - Grade 1: -0.15  
  - Grade 2: Không đổi
  - Grade 3: +0.15

### Learning Steps
**Progression:** 1 phút → 10 phút → 1 ngày

- **Grade 0**: Reset về step đầu
- **Grade 1**: Lặp lại step hiện tại
- **Grade 2**: Tiến lên step tiếp theo
- **Grade 3**: Graduate ngay lập tức

### Interval Calculation

**NEW → LEARNING:**
- Bắt đầu với learning steps

**LEARNING:**
- Grade 0,1: Theo learning steps (1min, 10min)  
- Grade 2: Graduate với 1 ngày interval
- Grade 3: Graduate với 4 ngày interval

**REVIEW:**
- Grade 0: → RELEARNING, interval = 1 ngày
- Grade 1: interval × ease_factor × 1.2
- Grade 2: interval × ease_factor  
- Grade 3: interval × ease_factor × 1.3

### Leech Detection
- Card failed 8+ lần liên tiếp
- Tự động flag `isLeech = true`
- Cần xử lý đặc biệt (suspend, reset, etc.)

## Nghiệp Vụ Quan Trọng

### 1. StudyState Creation
- **Lazy Creation**: StudyState chỉ được tạo khi card được review lần đầu
- **findCardsWithoutStudyState()**: Query tìm card chưa có StudyState
- **Auto StudyState**: Tạo temporary StudyState cho cards mới trong getNextCard()

### 2. Session Management
- Chỉ 1 active session per user
- Session tracking: cards studied, accuracy, duration
- Auto-abandon after timeout (có thể configure)

### 3. Priority Algorithm
```
getNextCard() priority:
1. Learning cards (isDue = true)
2. Review cards (isDue = true)  
3. Existing NEW cards (StudyState exists)
4. Cards without StudyState (thực sự mới)
```

### 4. Interval Fuzz
- **5% randomization** để tránh synchronization
- Giúp spread review load

## Error Cases

### No Cards Available
```json
{
  "success": false,
  "message": "No cards available for practice"
}
```

### No Active Session
```json
{
  "success": false, 
  "message": "No active practice session found"
}
```

### Invalid Grade
```json
{
  "success": false,
  "message": "Grade must be between 0 and 3"
}
```

### Card Not Found
```json
{
  "success": false,
  "message": "Card not found with id: {cardId}"
}
```

## Test Cases Đã Verify

### 1. Card Workflow
✅ NEW card → Review với grade 2 → LEARNING state  
✅ NEW card → Review với grade 3 → REVIEW state (graduate ngay)  
✅ LEARNING card → Review với grade 2 → Tiến lên learning step tiếp theo
✅ LEARNING card → Review với grade 3 → REVIEW state (graduate)

### 2. Session Flow  
✅ Start session → Practice cards → Complete session  
✅ Session statistics: accuracy, duration, card counts
✅ Session completion với summary đầy đủ

### 3. Analytics & Statistics
✅ User statistics overview: total cards, accuracy, streaks  
✅ Simplified overview cho dashboard
✅ Performance statistics với period presets
✅ Study streaks tracking
✅ Deck-specific statistics
✅ Analytics summary tổng hợp
✅ AI-generated study insights

### 4. Edge Cases
✅ Cards without StudyState được handle đúng  
✅ Ease factor không vượt quá 3.0  
✅ SQL recursive query hoạt động (study streak)
✅ Validation grade 0-3
✅ Error handling cho card không tồn tại
✅ Next card hoạt động ngay cả khi không có session active

### 5. SRS Algorithm Verification
✅ Learning steps progression: 1min → 10min → 1day
✅ Grade 0 (Again): Reset về step đầu
✅ Grade 1 (Hard): Lặp lại step hiện tại  
✅ Grade 2 (Good): Tiến lên step tiếp theo
✅ Grade 3 (Easy): Graduate ngay lập tức
✅ Interval calculation chính xác
✅ Card state transitions đúng

## Notes cho Developer

1. **Database**: StudyState table quan trọng nhất, chứa toàn bộ SRS state
2. **Performance**: Index trên (user_id, due_date) cho due cards query
3. **Migration**: V2 migration update ease_factor constraint 1.3-3.0
4. **Testing**: Đã verify end-to-end flow từ create card → practice → analytics
5. **API Organization**: Analytics endpoints đã được tách riêng khỏi practice endpoints
6. **Session Management**: getNextCard() hoạt động độc lập với session state
7. **Error Handling**: Validation và error messages đã được implement đầy đủ

## Luồng Practice - Biểu Đồ Mermaid

### 1. Overall Practice Workflow
```mermaid
graph TD
    A[User khởi tạo Practice] --> B{Có session active?}
    B -->|Có| C[Lấy session hiện tại]
    B -->|Không| D[Tạo session mới]
    
    C --> E[Lấy card tiếp theo]
    D --> E
    
    E --> F{Có card available?}
    F -->|Không| G[No cards available]
    F -->|Có| H[Hiển thị card]
    
    H --> I[User review card]
    I --> J[Submit grade 0-3]
    J --> K[Cập nhật StudyState]
    K --> L{Còn cards?}
    
    L -->|Có| E
    L -->|Không| M[Complete session]
    M --> N[Thống kê session]
```

### 2. Card State Transitions
```mermaid
stateDiagram-v2
    [*] --> NoStudyState: Card mới tạo
    
    NoStudyState --> NEW: First review (auto-create StudyState)
    
    NEW --> LEARNING: Grade 0,1,2,3
    
    LEARNING --> LEARNING: Grade 0,1 (repeat steps)
    LEARNING --> REVIEW: Grade 2,3 (graduate)
    
    REVIEW --> REVIEW: Grade 1,2,3 (continue reviewing)
    REVIEW --> RELEARNING: Grade 0 (failed)
    
    RELEARNING --> RELEARNING: Grade 0,1 (repeat)
    RELEARNING --> REVIEW: Grade 2,3 (re-graduate)
    
    LEARNING --> LEECH: 8+ consecutive failures
    REVIEW --> LEECH: 8+ consecutive failures
    RELEARNING --> LEECH: 8+ consecutive failures
```

### 3. Next Card Selection Algorithm
```mermaid
flowchart TD
    A[getNextCard] --> B{Learning cards due?}
    B -->|Có| C[Return learning card]
    
    B -->|Không| D{Review cards due?}
    D -->|Có| E[Return review card]
    
    D -->|Không| F{NEW StudyState cards?}
    F -->|Có| G[Return NEW card]
    
    F -->|Không| H{Cards without StudyState?}
    H -->|Có| I[Create temp StudyState<br/>Return card]
    
    H -->|Không| J[No cards available]
```

### 4. SRS Interval Calculation
```mermaid
graph LR
    A[Card Review] --> B{Current State?}
    
    B -->|NEW| C[Go to LEARNING<br/>Steps: 1min → 10min → 1day]
    
    B -->|LEARNING| D{Grade?}
    D -->|0,1| E[Repeat step]
    D -->|2| F[Next step or Graduate]
    D -->|3| G[Graduate immediately]
    
    B -->|REVIEW| H{Grade?}
    H -->|0| I[→ RELEARNING<br/>interval = 1 day]
    H -->|1| J[interval × ease × 1.2]
    H -->|2| K[interval × ease]
    H -->|3| L[interval × ease × 1.3]
    
    B -->|RELEARNING| M[Same as LEARNING]
```

### 5. Analytics & Statistics Flow
```mermaid
graph TD
    A[Practice Session] --> B[StudyState Updates]
    B --> C[Statistics Calculation]
    C --> D[Analytics Endpoints]
    
    D --> E[User Overview]
    D --> F[Performance Stats]
    D --> G[Study Streaks]
    D --> H[Deck Statistics]
    D --> I[Study Insights]
    
    E --> J[Dashboard Display]
    F --> J
    G --> J
    H --> J
    I --> J
```

Hệ thống Practice SRS và Analytics đã được test hoàn chỉnh và sẵn sàng production! 🚀

**Key Features Verified:**
- ✅ SRS Algorithm hoạt động chính xác
- ✅ Practice Session Management
- ✅ Card State Transitions
- ✅ Analytics & Statistics
- ✅ Error Handling & Validation
- ✅ Performance & Scalability