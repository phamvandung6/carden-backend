# Tổng quan Cơ sở dữ liệu - Carden Flashcards

## Giới thiệu

Carden Flashcards là một hệ thống học từ vựng thông minh sử dụng thuật toán Spaced Repetition System (SRS) để tối ưu hóa quá trình ghi nhớ. Cơ sở dữ liệu được thiết kế để hỗ trợ một hệ thống học tập hiện đại với các tính năng cao cấp.

## Kiến trúc tổng quan

Hệ thống sử dụng **PostgreSQL** làm cơ sở dữ liệu chính với các tính năng nâng cao:
- **JSONB**: Lưu trữ dữ liệu phi cấu trúc (tags, examples, synonyms)
- **Full-text Search**: Tìm kiếm nội dung thẻ học
- **GIN Indexes**: Tối ưu hiệu suất cho JSONB và tìm kiếm
- **Soft Delete**: Xóa mềm cho Deck và Card
- **Audit Trail**: Theo dõi thời gian tạo/cập nhật

## Các entity chính

### 1. **User** - Người dùng
- Quản lý thông tin tài khoản và xác thực
- Cài đặt TTS (Text-to-Speech) cá nhân  
- Preferences học tập và giao diện
- Hỗ trợ vai trò USER và ADMIN

### 2. **Topic** - Chủ đề học tập
- Phân loại theo chủ đề (có thể phân cấp)
- Hỗ trợ system topics (mặc định) và user topics
- Cấu trúc cây cho việc tổ chức phân cấp

### 3. **Deck** - Bộ thẻ học
- Nhóm các thẻ học theo chủ đề
- Hỗ trợ CEFR levels (A1-C2)
- Chế độ public/private/unlisted
- Thống kê downloads và likes
- Soft delete với khôi phục

### 4. **Card** - Thẻ học từ vựng
- Front/Back content với IPA pronunciation
- Mảng examples, synonyms, antonyms (JSONB)
- Unique key để phát hiện trùng lặp
- Difficulty levels và multimedia support
- Soft delete với khôi phục

### 5. **StudyState** - Trạng thái học tập
- Theo dõi tiến độ học cho từng thẻ/người dùng
- Triển khai thuật toán SRS (ease factor, intervals)
- Trạng thái: NEW, LEARNING, REVIEW, RELEARNING
- Thống kê accuracy và performance

### 6. **ReviewSession** - Phiên học tập
- Ghi lại mỗi session học của user
- Hỗ trợ các mode: FLIP, TYPE_ANSWER, MULTIPLE_CHOICE
- Thống kê real-time và session analytics
- JSONB cho dữ liệu session phức tạp

### 7. **Job** - Background Jobs
- Quản lý các tác vụ nền (AI generation, import/export)
- Progress tracking và error handling
- Retry mechanism với max retries
- JSONB cho job data linh hoạt

### 8. **Report** - Báo cáo vi phạm
- Hệ thống moderation nội dung
- Báo cáo user, deck, card vi phạm
- Workflow admin review và xử lý
- Tracking reviewer và admin notes

## Mối quan hệ chính

```
User (1) ───── (N) Deck ───── (N) Card
  │                │              │
  │                └─ Topic       │
  │                               │
  └─ StudyState (N) ─────────────┘
  │
  ├─ ReviewSession (N)
  ├─ Job (N)
  └─ Report (N)
```

## Đặc điểm kỹ thuật

### Performance Optimization
- **Composite Indexes**: Cho các query phức tạp thường dùng
- **Partial Indexes**: Cho soft-deleted records
- **GIN Indexes**: Cho JSONB và full-text search
- **Foreign Key Indexes**: Tối ưu JOIN operations

### Data Integrity
- **Foreign Key Constraints**: Đảm bảo tính toàn vẹn tham chiếu
- **Check Constraints**: Validation ở database level
- **Unique Constraints**: Ngăn chặn duplicate data
- **NOT NULL Constraints**: Đảm bảo required fields

### Scalability Features
- **Connection Pooling**: HikariCP cho performance
- **Flyway Migrations**: Quản lý schema versions
- **Audit Fields**: Tracking changes với timestamps
- **JSONB Storage**: Linh hoạt cho future extensions

## Migration Strategy

Sử dụng **Flyway** cho quản lý database migrations:
- **Development**: Clean enabled cho flexibility
- **Production**: Validate-only cho safety
- **Versioning**: Sequential numbering (V1, V2, ...)
- **Rollback**: Manual scripts cho critical changes

## Monitoring và Maintenance

### Query Performance
- Monitor slow queries với `pg_stat_statements`
- Regular VACUUM và ANALYZE
- Index usage monitoring
- Connection pool metrics

### Data Backup
- Daily automated backups
- Point-in-time recovery capability
- Cross-region backup replication
- Regular restore testing

---

*Tài liệu này được cập nhật tương ứng với migration V1 - Schema ban đầu với đầy đủ indexes*
