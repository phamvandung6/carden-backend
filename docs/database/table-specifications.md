# Đặc tả chi tiết các bảng - Carden Database

## 1. Bảng `users` - Người dùng

### Mô tả

Lưu trữ thông tin tài khoản người dùng, cài đặt cá nhân và preferences học tập.

### Cấu trúc bảng

| Cột                           | Kiểu dữ liệu     | Ràng buộc        | Mặc định          | Mô tả                                     |
| ----------------------------- | ---------------- | ---------------- | ----------------- | ----------------------------------------- |
| `id`                          | BIGSERIAL        | PRIMARY KEY      | AUTO              | ID duy nhất của người dùng                |
| `username`                    | VARCHAR(50)      | NOT NULL, UNIQUE | -                 | Tên đăng nhập (alphanumeric + underscore) |
| `email`                       | VARCHAR(100)     | NOT NULL, UNIQUE | -                 | Email đăng ký tài khoản                   |
| `password`                    | VARCHAR(255)     | NOT NULL         | -                 | Mật khẩu đã hash (BCrypt)                 |
| `display_name`                | VARCHAR(100)     | NULLABLE         | -                 | Tên hiển thị công khai                    |
| `profile_image_url`           | VARCHAR(500)     | NULLABLE         | -                 | URL ảnh đại diện (lưu trên Cloudflare R2) |
| `role`                        | VARCHAR(20)      | NOT NULL         | 'USER'            | Vai trò: USER, ADMIN                      |
| `is_active`                   | BOOLEAN          | -                | TRUE              | Trạng thái kích hoạt tài khoản            |
| `email_verified`              | BOOLEAN          | -                | FALSE             | Xác thực email                            |
| `last_login`                  | TIMESTAMP        | NULLABLE         | -                 | Lần đăng nhập cuối                        |
| **TTS Settings**              |
| `tts_enabled`                 | BOOLEAN          | -                | TRUE              | Bật/tắt Text-to-Speech                    |
| `preferred_voice`             | VARCHAR(255)     | NULLABLE         | -                 | Voice engine ưa thích                     |
| `speech_rate`                 | DOUBLE PRECISION | -                | 1.0               | Tốc độ đọc (0.5-2.0)                      |
| `speech_pitch`                | DOUBLE PRECISION | -                | 1.0               | Cao độ giọng (0.5-2.0)                    |
| `speech_volume`               | DOUBLE PRECISION | -                | 1.0               | Âm lượng (0.0-1.0)                        |
| **User Preferences**          |
| `timezone`                    | VARCHAR(32)      | -                | 'UTC'             | Múi giờ người dùng                        |
| `ui_language`                 | VARCHAR(10)      | -                | 'en'              | Ngôn ngữ giao diện                        |
| `learning_goal_cards_per_day` | INTEGER          | -                | 20                | Mục tiêu thẻ học/ngày                     |
| **Audit Fields**              |
| `created_at`                  | TIMESTAMP        | NOT NULL         | CURRENT_TIMESTAMP | Thời gian tạo                             |
| `updated_at`                  | TIMESTAMP        | NOT NULL         | CURRENT_TIMESTAMP | Thời gian cập nhật                        |
| `version`                     | BIGINT           | -                | 0                 | Version cho optimistic locking            |

### Indexes

- `idx_users_email` ON (email)
- `idx_users_username` ON (username)
- `idx_users_active` ON (is_active)
- `idx_users_role` ON (role)

---

## 2. Bảng `topics` - Chủ đề

### Mô tả

Phân loại thẻ học theo chủ đề, hỗ trợ cấu trúc phân cấp.

### Cấu trúc bảng

| Cột               | Kiểu dữ liệu | Ràng buộc    | Mặc định          | Mô tả                           |
| ----------------- | ------------ | ------------ | ----------------- | ------------------------------- |
| `id`              | BIGSERIAL    | PRIMARY KEY  | AUTO              | ID duy nhất của chủ đề          |
| `name`            | VARCHAR(100) | NOT NULL     | -                 | Tên chủ đề                      |
| `description`     | VARCHAR(500) | NULLABLE     | -                 | Mô tả chi tiết chủ đề           |
| `is_system_topic` | BOOLEAN      | -            | FALSE             | Chủ đề hệ thống (không thể xóa) |
| `display_order`   | INTEGER      | -            | 0                 | Thứ tự hiển thị                 |
| `parent_topic_id` | BIGINT       | FK, NULLABLE | -                 | Chủ đề cha (self-reference)     |
| `created_at`      | TIMESTAMP    | NOT NULL     | CURRENT_TIMESTAMP | Thời gian tạo                   |
| `updated_at`      | TIMESTAMP    | NOT NULL     | CURRENT_TIMESTAMP | Thời gian cập nhật              |
| `version`         | BIGINT       | -            | 0                 | Version cho optimistic locking  |

### Indexes

- `idx_topics_parent` ON (parent_topic_id)
- `idx_topics_system` ON (is_system_topic)
- `idx_topics_display_order` ON (display_order)

---

## 3. Bảng `decks` - Bộ thẻ học

### Mô tả

Nhóm các thẻ học theo chủ đề, với các tính năng sharing và thống kê.

### Cấu trúc bảng

| Cột               | Kiểu dữ liệu  | Ràng buộc    | Mặc định          | Mô tả                               |
| ----------------- | ------------- | ------------ | ----------------- | ----------------------------------- |
| `id`              | BIGSERIAL     | PRIMARY KEY  | AUTO              | ID duy nhất của bộ thẻ              |
| `title`           | VARCHAR(200)  | NOT NULL     | -                 | Tiêu đề bộ thẻ                      |
| `description`     | VARCHAR(1000) | NULLABLE     | -                 | Mô tả chi tiết                      |
| `user_id`         | BIGINT        | FK, NOT NULL | -                 | Người tạo bộ thẻ                    |
| `topic_id`        | BIGINT        | FK, NULLABLE | -                 | Chủ đề của bộ thẻ                   |
| `visibility`      | VARCHAR(20)   | NOT NULL     | 'PRIVATE'         | PRIVATE, PUBLIC, UNLISTED           |
| `cefr_level`      | VARCHAR(2)    | NULLABLE     | -                 | Cấp độ CEFR: A1, A2, B1, B2, C1, C2 |
| `source_language` | VARCHAR(10)   | -            | 'en'              | Ngôn ngữ nguồn                      |
| `target_language` | VARCHAR(10)   | -            | 'vi'              | Ngôn ngữ đích                       |
| `cover_image_url` | VARCHAR(500)  | NULLABLE     | -                 | URL ảnh bìa                         |
| `tags`            | JSONB         | NULLABLE     | -                 | Tags phân loại (array of strings)   |
| `is_system_deck`  | BOOLEAN       | -            | FALSE             | Bộ thẻ hệ thống                     |
| `is_public`       | BOOLEAN       | -            | FALSE             | Công khai trong marketplace         |
| `download_count`  | BIGINT        | -            | 0                 | Số lượt tải về                      |
| `like_count`      | BIGINT        | -            | 0                 | Số lượt thích                       |
| `card_count`      | INTEGER       | -            | 0                 | Số thẻ trong bộ (denormalized)      |
| `deleted_at`      | TIMESTAMP     | NULLABLE     | -                 | Soft delete timestamp               |
| `created_at`      | TIMESTAMP     | NOT NULL     | CURRENT_TIMESTAMP | Thời gian tạo                       |
| `updated_at`      | TIMESTAMP     | NOT NULL     | CURRENT_TIMESTAMP | Thời gian cập nhật                  |
| `version`         | BIGINT        | -            | 0                 | Version cho optimistic locking      |

### Indexes

- `idx_decks_user_id` ON (user_id)
- `idx_decks_topic_id` ON (topic_id)
- `idx_decks_visibility` ON (visibility)
- `idx_decks_public` ON (is_public)
- `idx_decks_system` ON (is_system_deck)
- `idx_decks_deleted_at` ON (deleted_at)
- `idx_decks_user_visibility` ON (user_id, visibility)
- `idx_decks_public_visibility` ON (is_public, visibility) WHERE is_public = true
- `idx_decks_tags_gin` ON (tags) USING GIN
- `idx_decks_user_status` ON (user_id, deleted_at) WHERE deleted_at IS NULL

---

## 4. Bảng `cards` - Thẻ học

### Mô tả

Lưu trữ nội dung thẻ học với hỗ trợ multimedia và metadata phong phú.

### Cấu trúc bảng

| Cột                 | Kiểu dữ liệu  | Ràng buộc    | Mặc định          | Mô tả                             |
| ------------------- | ------------- | ------------ | ----------------- | --------------------------------- |
| `id`                | BIGSERIAL     | PRIMARY KEY  | AUTO              | ID duy nhất của thẻ học           |
| `deck_id`           | BIGINT        | FK, NOT NULL | -                 | Bộ thẻ chứa thẻ này               |
| `front`             | VARCHAR(500)  | NOT NULL     | -                 | Mặt trước thẻ (từ/câu hỏi)        |
| `back`              | VARCHAR(500)  | NOT NULL     | -                 | Mặt sau thẻ (nghĩa/đáp án)        |
| `ipa_pronunciation` | VARCHAR(200)  | NULLABLE     | -                 | Phiên âm IPA                      |
| `examples`          | JSONB         | NULLABLE     | -                 | Câu ví dụ (array of strings)      |
| `synonyms`          | JSONB         | NULLABLE     | -                 | Từ đồng nghĩa (array of strings)  |
| `antonyms`          | JSONB         | NULLABLE     | -                 | Từ trái nghĩa (array of strings)  |
| `tags`              | JSONB         | NULLABLE     | -                 | Tags phân loại (array of strings) |
| `image_url`         | VARCHAR(500)  | NULLABLE     | -                 | URL hình ảnh minh họa             |
| `audio_url`         | VARCHAR(500)  | NULLABLE     | -                 | URL file âm thanh                 |
| `unique_key`        | VARCHAR(1000) | NOT NULL     | -                 | Key để detect duplicate           |
| `difficulty`        | VARCHAR(10)   | NOT NULL     | 'NORMAL'          | EASY, NORMAL, HARD                |
| `display_order`     | INTEGER       | -            | 0                 | Thứ tự trong deck                 |
| `deleted_at`        | TIMESTAMP     | NULLABLE     | -                 | Soft delete timestamp             |
| `created_at`        | TIMESTAMP     | NOT NULL     | CURRENT_TIMESTAMP | Thời gian tạo                     |
| `updated_at`        | TIMESTAMP     | NOT NULL     | CURRENT_TIMESTAMP | Thời gian cập nhật                |
| `version`           | BIGINT        | -            | 0                 | Version cho optimistic locking    |

### Indexes

- `idx_cards_deck_id` ON (deck_id)
- `idx_cards_unique_key` ON (unique_key)
- `idx_cards_deleted_at` ON (deleted_at)
- `idx_cards_difficulty` ON (difficulty)
- `idx_cards_deck_order` ON (deck_id, display_order)
- `idx_cards_examples_gin` ON (examples) USING GIN
- `idx_cards_synonyms_gin` ON (synonyms) USING GIN
- `idx_cards_antonyms_gin` ON (antonyms) USING GIN
- `idx_cards_tags_gin` ON (tags) USING GIN
- `idx_cards_front_fts` ON (to_tsvector('english', front)) USING GIN
- `idx_cards_back_fts` ON (to_tsvector('english', back)) USING GIN
- `idx_cards_deck_status` ON (deck_id, deleted_at) WHERE deleted_at IS NULL

---

## 5. Bảng `study_states` - Trạng thái học tập

### Mô tả

Theo dõi tiến độ học của từng user cho từng thẻ, triển khai thuật toán SRS.

### Cấu trúc bảng

| Cột                | Kiểu dữ liệu     | Ràng buộc    | Mặc định          | Mô tả                             |
| ------------------ | ---------------- | ------------ | ----------------- | --------------------------------- |
| `id`               | BIGSERIAL        | PRIMARY KEY  | AUTO              | ID duy nhất                       |
| `user_id`          | BIGINT           | FK, NOT NULL | -                 | Người học                         |
| `card_id`          | BIGINT           | FK, NOT NULL | -                 | Thẻ được học                      |
| `deck_id`          | BIGINT           | FK, NOT NULL | -                 | Bộ thẻ (denormalized)             |
| `repetition_count` | INTEGER          | NOT NULL     | 0                 | Số lần đã ôn                      |
| `ease_factor`      | DOUBLE PRECISION | NOT NULL     | 2.5               | Hệ số độ dễ (1.3-2.5)             |
| `interval_days`    | INTEGER          | NOT NULL     | 1                 | Khoảng cách ôn tập (ngày)         |
| `due_date`         | TIMESTAMP        | NOT NULL     | -                 | Thời gian đến hạn ôn              |
| `card_state`       | VARCHAR(15)      | NOT NULL     | 'NEW'             | NEW, LEARNING, REVIEW, RELEARNING |
| `last_review_date` | TIMESTAMP        | NULLABLE     | -                 | Lần ôn cuối                       |
| `last_score`       | INTEGER          | NULLABLE     | -                 | Điểm lần ôn cuối (0-3)            |
| `total_reviews`    | INTEGER          | -            | 0                 | Tổng số lần ôn                    |
| `correct_reviews`  | INTEGER          | -            | 0                 | Số lần ôn đúng                    |
| `accuracy_rate`    | DOUBLE PRECISION | -            | 0.0               | Tỷ lệ chính xác (%)               |
| `created_at`       | TIMESTAMP        | NOT NULL     | CURRENT_TIMESTAMP | Thời gian tạo                     |
| `updated_at`       | TIMESTAMP        | NOT NULL     | CURRENT_TIMESTAMP | Thời gian cập nhật                |
| `version`          | BIGINT           | -            | 0                 | Version cho optimistic locking    |

### Constraints

- **Unique**: (user_id, card_id) - mỗi user chỉ có 1 study state per card
- **Check**: ease_factor BETWEEN 1.3 AND 2.5

### Indexes

- `idx_study_states_user_card` ON (user_id, card_id)
- `idx_study_states_due_date` ON (due_date)
- `idx_study_states_user_due` ON (user_id, due_date)
- `idx_study_states_deck_id` ON (deck_id)
- `idx_study_states_card_state` ON (card_state)
- `idx_study_states_user_state` ON (user_id, card_state)
- `idx_study_states_due_cards` ON (user_id, due_date, card_state) WHERE due_date <= CURRENT_TIMESTAMP

---

## 6. Bảng `review_sessions` - Phiên học tập

### Mô tả

Ghi lại thông tin chi tiết về mỗi session học của user.

### Cấu trúc bảng

| Cột                | Kiểu dữ liệu     | Ràng buộc    | Mặc định          | Mô tả                              |
| ------------------ | ---------------- | ------------ | ----------------- | ---------------------------------- |
| `id`               | BIGSERIAL        | PRIMARY KEY  | AUTO              | ID duy nhất session                |
| `user_id`          | BIGINT           | FK, NOT NULL | -                 | Người học                          |
| `deck_id`          | BIGINT           | FK, NULLABLE | -                 | Bộ thẻ được học                    |
| `session_date`     | TIMESTAMP        | NOT NULL     | -                 | Thời gian bắt đầu session          |
| `duration_minutes` | INTEGER          | NULLABLE     | -                 | Thời lượng session (phút)          |
| `cards_studied`    | INTEGER          | -            | 0                 | Số thẻ đã học                      |
| `cards_correct`    | INTEGER          | -            | 0                 | Số thẻ trả lời đúng                |
| `new_cards`        | INTEGER          | -            | 0                 | Số thẻ mới                         |
| `review_cards`     | INTEGER          | -            | 0                 | Số thẻ ôn tập                      |
| `relearning_cards` | INTEGER          | -            | 0                 | Số thẻ học lại                     |
| `accuracy_rate`    | DOUBLE PRECISION | -            | 0.0               | Tỷ lệ chính xác session (%)        |
| `study_mode`       | VARCHAR(20)      | NOT NULL     | -                 | FLIP, TYPE_ANSWER, MULTIPLE_CHOICE |
| `session_status`   | VARCHAR(15)      | -            | 'IN_PROGRESS'     | IN_PROGRESS, COMPLETED, ABANDONED  |
| `session_stats`    | JSONB            | NULLABLE     | -                 | Thống kê chi tiết session          |
| `created_at`       | TIMESTAMP        | NOT NULL     | CURRENT_TIMESTAMP | Thời gian tạo                      |
| `updated_at`       | TIMESTAMP        | NOT NULL     | CURRENT_TIMESTAMP | Thời gian cập nhật                 |
| `version`          | BIGINT           | -            | 0                 | Version cho optimistic locking     |

### Session Stats JSONB Structure

```json
{
  "averageResponseTime": 1500, // milliseconds
  "scoreDistribution": [2, 5, 8, 3], // [again, hard, good, easy]
  "totalTimeSpent": 1800, // seconds
  "pauseCount": 3,
  "difficultyRating": 7.5 // user subjective rating 1-10
}
```

### Indexes

- `idx_review_sessions_user` ON (user_id)
- `idx_review_sessions_date` ON (session_date)
- `idx_review_sessions_deck` ON (deck_id)
- `idx_review_sessions_status` ON (session_status)
- `idx_review_sessions_study_mode` ON (study_mode)
- `idx_review_sessions_user_date` ON (user_id, session_date)

---

## 7. Bảng `jobs` - Background Jobs

### Mô tả

Quản lý các tác vụ chạy nền như AI generation, import/export.

### Cấu trúc bảng

| Cột                   | Kiểu dữ liệu | Ràng buộc    | Mặc định          | Mô tả                                           |
| --------------------- | ------------ | ------------ | ----------------- | ----------------------------------------------- |
| `id`                  | BIGSERIAL    | PRIMARY KEY  | AUTO              | ID duy nhất job                                 |
| `user_id`             | BIGINT       | FK, NOT NULL | -                 | User yêu cầu job                                |
| `job_type`            | VARCHAR(50)  | NOT NULL     | -                 | Loại job: AI_DECK_GENERATION, DATA_IMPORT, etc. |
| `status`              | VARCHAR(20)  | NOT NULL     | 'PENDING'         | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED  |
| `job_data`            | JSONB        | NULLABLE     | -                 | Input parameters                                |
| `result_data`         | JSONB        | NULLABLE     | -                 | Output results                                  |
| `progress_percentage` | INTEGER      | -            | 0                 | Tiến độ thực hiện (0-100)                       |
| `status_message`      | VARCHAR(500) | NULLABLE     | -                 | Thông báo trạng thái                            |
| `error_message`       | TEXT         | NULLABLE     | -                 | Chi tiết lỗi nếu có                             |
| `started_at`          | TIMESTAMP    | NULLABLE     | -                 | Thời gian bắt đầu                               |
| `completed_at`        | TIMESTAMP    | NULLABLE     | -                 | Thời gian hoàn thành                            |
| `retry_count`         | INTEGER      | -            | 0                 | Số lần retry                                    |
| `max_retries`         | INTEGER      | -            | 3                 | Số lần retry tối đa                             |
| `created_at`          | TIMESTAMP    | NOT NULL     | CURRENT_TIMESTAMP | Thời gian tạo                                   |
| `updated_at`          | TIMESTAMP    | NOT NULL     | CURRENT_TIMESTAMP | Thời gian cập nhật                              |
| `version`             | BIGINT       | -            | 0                 | Version cho optimistic locking                  |

### Indexes

- `idx_jobs_user` ON (user_id)
- `idx_jobs_status` ON (status)
- `idx_jobs_type` ON (job_type)
- `idx_jobs_created_at` ON (created_at)
- `idx_jobs_user_status` ON (user_id, status)

---

## 8. Bảng `reports` - Báo cáo vi phạm

### Mô tả

Hệ thống báo cáo nội dung vi phạm và workflow moderation.

### Cấu trúc bảng

| Cột                | Kiểu dữ liệu | Ràng buộc    | Mặc định          | Mô tả                                      |
| ------------------ | ------------ | ------------ | ----------------- | ------------------------------------------ |
| `id`               | BIGSERIAL    | PRIMARY KEY  | AUTO              | ID duy nhất report                         |
| `reporter_id`      | BIGINT       | FK, NOT NULL | -                 | Người báo cáo                              |
| `reported_user_id` | BIGINT       | FK, NULLABLE | -                 | User bị báo cáo                            |
| `reported_deck_id` | BIGINT       | FK, NULLABLE | -                 | Deck bị báo cáo                            |
| `reported_card_id` | BIGINT       | FK, NULLABLE | -                 | Card bị báo cáo                            |
| `report_type`      | VARCHAR(25)  | NOT NULL     | -                 | Loại vi phạm                               |
| `reason`           | TEXT         | NULLABLE     | -                 | Lý do chi tiết                             |
| `status`           | VARCHAR(15)  | NOT NULL     | 'PENDING'         | PENDING, UNDER_REVIEW, RESOLVED, DISMISSED |
| `additional_data`  | JSONB        | NULLABLE     | -                 | Dữ liệu bổ sung                            |
| `reviewed_by_id`   | BIGINT       | FK, NULLABLE | -                 | Admin xử lý                                |
| `reviewed_at`      | TIMESTAMP    | NULLABLE     | -                 | Thời gian xử lý                            |
| `admin_notes`      | TEXT         | NULLABLE     | -                 | Ghi chú của admin                          |
| `created_at`       | TIMESTAMP    | NOT NULL     | CURRENT_TIMESTAMP | Thời gian tạo                              |
| `updated_at`       | TIMESTAMP    | NOT NULL     | CURRENT_TIMESTAMP | Thời gian cập nhật                         |
| `version`          | BIGINT       | -            | 0                 | Version cho optimistic locking             |

### Report Types

- `INAPPROPRIATE_CONTENT`: Nội dung không phù hợp
- `COPYRIGHT_VIOLATION`: Vi phạm bản quyền
- `SPAM`: Spam/rác
- `HARASSMENT`: Quấy rối
- `MISINFORMATION`: Thông tin sai lệch
- `OTHER`: Khác

### Indexes

- `idx_reports_reporter` ON (reporter_id)
- `idx_reports_status` ON (status)
- `idx_reports_type` ON (report_type)
- `idx_reports_reported_user` ON (reported_user_id)
- `idx_reports_reported_deck` ON (reported_deck_id)
- `idx_reports_reported_card` ON (reported_card_id)
- `idx_reports_reviewed_by` ON (reviewed_by_id)

---

_Tài liệu này được cập nhật theo migration V1 và sẽ được maintain theo các thay đổi schema_
