# Tài liệu API Flows - Carden Flashcards

## Tổng quan

Tài liệu này mô tả chi tiết các luồng nghiệp vụ chính trong hệ thống Carden Flashcards API. Mỗi luồng bao gồm các endpoint, request/response examples, validation rules, và test cases để sử dụng với Swagger UI.

## Danh sách API Flows

### 1. [Authentication Flow](./01-authentication-flow.md)
- Đăng ký tài khoản mới
- Đăng nhập với username/email
- Đăng xuất
- JWT token management
- Role-based authentication

### 2. [User Management Flow](./02-user-management-flow.md)
- Lấy và cập nhật profile
- Quản lý TTS settings
- Upload avatar (server-side & presigned URL)
- Timezone và language preferences

### 3. [Deck Management Flow](./03-deck-management-flow.md)
- Tạo bộ thẻ mới
- Tìm kiếm public decks (marketplace)
- Quản lý my decks
- Cập nhật và xóa decks
- Visibility settings: PRIVATE, PUBLIC, UNLISTED

### 4. [Card Management Flow](./04-card-management-flow.md)
- Tạo thẻ học trong deck
- Tìm kiếm và lọc cards
- Cập nhật thông tin thẻ
- Bulk operations
- Multimedia support (images, audio)

### 5. [Practice Flow](./05-practice-flow.md)
- Hệ thống SRS (Spaced Repetition System)
- Bắt đầu practice sessions
- Submit reviews và scoring
- Thống kê học tập
- Card states và algorithms

### 6. [Topic Management Flow](./06-topic-management-flow.md)
- Danh sách topics phân cấp
- Sử dụng topics để categorize decks
- System topics vs user topics
- Hierarchical navigation

### 7. [Health Check Flow](./07-health-check-flow.md)
- Health monitoring endpoint
- System status information
- Monitoring integration

## Quick Start Guide

### 1. Thiết lập Environment

#### Base URL:
```
Local Development: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui/index.html
API Docs: http://localhost:8080/v3/api-docs
```

#### Authentication Token:
Sau khi đăng nhập, sử dụng access token để truy cập các protected endpoints:
```
Authorization: Bearer <your_access_token>
```

### 2. Luồng cơ bản để test

#### Step 1: Health Check
```bash
GET /public/health
```

#### Step 2: Đăng ký tài khoản
```json
POST /v1/auth/register
{
  "username": "testuser123",
  "email": "test@example.com",
  "password": "SecurePass123!",
  "displayName": "Test User"
}
```

#### Step 3: Đăng nhập
```json
POST /v1/auth/login
{
  "usernameOrEmail": "testuser123",
  "password": "SecurePass123!"
}
```

#### Step 4: Tạo deck
```json
POST /v1/decks
{
  "title": "My First Deck",
  "description": "Learning basic vocabulary",
  "topicId": 1,
  "cefrLevel": "A1"
}
```

#### Step 5: Thêm cards vào deck
```json
POST /v1/decks/{deckId}/cards
{
  "front": "Hello",
  "back": "Xin chào",
  "difficulty": "EASY"
}
```

#### Step 6: Bắt đầu practice
```json
POST /v1/practice/sessions
{
  "studyMode": "FLIP",
  "deckId": {deckId},
  "maxCards": 10
}
```

## Test Data Examples

### Sample Users:
```json
[
  {
    "username": "student1",
    "email": "student1@example.com",
    "password": "Password123!",
    "displayName": "Student One"
  },
  {
    "username": "teacher1",
    "email": "teacher1@example.com",
    "password": "Password123!",
    "displayName": "Teacher One"
  }
]
```

### Sample Decks:
```json
[
  {
    "title": "Basic English Greetings",
    "description": "Common greetings and polite expressions",
    "topicId": 1,
    "cefrLevel": "A1",
    "tags": ["greetings", "basic", "polite"]
  },
  {
    "title": "Business Meeting Vocabulary",
    "description": "Essential vocabulary for business meetings",
    "topicId": 3,
    "cefrLevel": "B2",
    "tags": ["business", "meeting", "professional"]
  }
]
```

### Sample Cards:
```json
[
  {
    "front": "Good morning",
    "back": "Chào buổi sáng",
    "ipaPronunciation": "/ɡʊd ˈmɔːr.nɪŋ/",
    "examples": ["Good morning, everyone!", "Good morning, how are you today?"],
    "tags": ["greeting", "time"],
    "difficulty": "EASY"
  },
  {
    "front": "Presentation",
    "back": "Bài thuyết trình",
    "examples": ["I will give a presentation tomorrow.", "The presentation was very informative."],
    "synonyms": ["Speech", "Talk"],
    "tags": ["business", "speaking"],
    "difficulty": "NORMAL"
  }
]
```

## Error Handling

### Common HTTP Status Codes:
- **200 OK**: Request thành công
- **201 Created**: Tạo resource thành công
- **400 Bad Request**: Request không hợp lệ
- **401 Unauthorized**: Thiếu hoặc sai authentication
- **403 Forbidden**: Không có quyền truy cập
- **404 Not Found**: Resource không tồn tại
- **422 Unprocessable Entity**: Validation errors
- **500 Internal Server Error**: Lỗi server

### Standard Error Response Format:
```json
{
  "success": false,
  "message": "Error description",
  "errors": [
    {
      "field": "fieldName",
      "message": "Field-specific error message"
    }
  ],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Best Practices khi Test

### 1. Authentication:
- Luôn đăng nhập trước khi test protected endpoints
- Copy access token từ login response
- Paste vào Authorization header trong Swagger UI

### 2. Data Dependencies:
- Tạo topics trước khi tạo decks
- Tạo decks trước khi tạo cards
- Tạo cards trước khi practice

### 3. Validation Testing:
- Test với cả valid và invalid data
- Kiểm tra field length limits
- Test required fields

### 4. Pagination:
- Test với different page sizes
- Verify total counts
- Test edge cases (empty results)

### 5. Search & Filtering:
- Test với different search terms
- Combine multiple filters
- Test case sensitivity

## Tools và Resources

### Swagger UI:
- Interactive API documentation
- Built-in request/response testing
- Authentication support

### Postman Collection:
Có thể import API documentation từ:
```
http://localhost:8080/v3/api-docs
```

### Database Browser:
Có thể connect đến PostgreSQL để verify data:
```
Host: localhost
Port: 5432
Database: carden
Username: carden_user
Password: carden_password
```

## Support và Feedback

Nếu có vấn đề hoặc câu hỏi về API:
1. Kiểm tra error responses và status codes
2. Verify authentication tokens
3. Check request body format
4. Review validation rules trong tài liệu
5. Xem logs trong console của application

---

*Tài liệu này được cập nhật thường xuyên theo phát triển của API. Phiên bản hiện tại: v1.0.0*
