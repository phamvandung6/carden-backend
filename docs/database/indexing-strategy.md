# Chiến lược Indexing - Carden Database

## 1. Tổng quan Indexing Strategy

### 1.1 Mục tiêu
- **Performance**: Tối ưu hóa các query phổ biến
- **Scalability**: Đảm bảo hiệu suất khi data tăng
- **Storage**: Cân bằng giữa query speed và disk usage
- **Maintenance**: Minimize index maintenance overhead

### 1.2 Index Types được sử dụng
- **B-Tree Indexes**: Default cho most columns
- **GIN Indexes**: Cho JSONB columns và full-text search
- **Partial Indexes**: Cho filtered queries
- **Composite Indexes**: Cho multi-column queries
- **Unique Indexes**: Đảm bảo data integrity

---

## 2. Primary Key Indexes

### 2.1 Auto-generated Primary Keys
Tất cả tables sử dụng `BIGSERIAL` primary keys với B-Tree indexes:

```sql
-- Auto-created với PRIMARY KEY constraint
CREATE UNIQUE INDEX pk_users ON users(id);
CREATE UNIQUE INDEX pk_decks ON decks(id);
CREATE UNIQUE INDEX pk_cards ON cards(id);
-- ... tương tự cho all tables
```

### 2.2 Performance Characteristics
- **Lookup Time**: O(log n) cho exact matches
- **Range Queries**: Efficient cho pagination
- **Join Performance**: Optimal cho foreign key joins
- **Space Usage**: ~8 bytes per row cho BIGINT

---

## 3. Foreign Key Indexes

### 3.1 Mandatory FK Indexes
Tất cả foreign keys đều có corresponding indexes:

```sql
-- User relationships
CREATE INDEX idx_decks_user_id ON decks(user_id);
CREATE INDEX idx_study_states_user_id ON study_states(user_id);
CREATE INDEX idx_review_sessions_user ON review_sessions(user_id);
CREATE INDEX idx_jobs_user ON jobs(user_id);
CREATE INDEX idx_reports_reporter ON reports(reporter_id);

-- Deck relationships  
CREATE INDEX idx_cards_deck_id ON cards(deck_id);
CREATE INDEX idx_study_states_deck_id ON study_states(deck_id);
CREATE INDEX idx_review_sessions_deck ON review_sessions(deck_id);

-- Topic relationships
CREATE INDEX idx_decks_topic_id ON decks(topic_id);
CREATE INDEX idx_topics_parent ON topics(parent_topic_id);
```

### 3.2 Query Patterns Optimized
- **User's Decks**: `SELECT * FROM decks WHERE user_id = ?`
- **Deck's Cards**: `SELECT * FROM cards WHERE deck_id = ?`
- **User's Progress**: `SELECT * FROM study_states WHERE user_id = ?`
- **Topic Hierarchy**: `SELECT * FROM topics WHERE parent_topic_id = ?`

---

## 4. Unique Constraint Indexes

### 4.1 Business Unique Constraints
```sql
-- User uniqueness
CREATE UNIQUE INDEX uk_users_username ON users(username);
CREATE UNIQUE INDEX uk_users_email ON users(email);

-- Study state uniqueness (one per user per card)
CREATE UNIQUE INDEX uk_study_states_user_card ON study_states(user_id, card_id);
```

### 4.2 Duplicate Prevention
- **Username/Email**: Prevent duplicate accounts
- **Study States**: One progress record per user/card combination
- **Performance**: Unique indexes also serve as lookup indexes

---

## 5. Composite Indexes cho Complex Queries

### 5.1 User-scoped Queries
```sql
-- User's active decks
CREATE INDEX idx_decks_user_visibility ON decks(user_id, visibility);

-- User's due cards
CREATE INDEX idx_study_states_user_due ON study_states(user_id, due_date);

-- User's study sessions by date
CREATE INDEX idx_review_sessions_user_date ON review_sessions(user_id, session_date);

-- User's jobs by status
CREATE INDEX idx_jobs_user_status ON jobs(user_id, status);
```

### 5.2 Query Optimization Examples
```sql
-- Optimized by idx_decks_user_visibility
SELECT * FROM decks 
WHERE user_id = ? AND visibility = 'PUBLIC';

-- Optimized by idx_study_states_user_due  
SELECT * FROM study_states 
WHERE user_id = ? AND due_date <= NOW();

-- Optimized by idx_review_sessions_user_date
SELECT * FROM review_sessions 
WHERE user_id = ? AND session_date >= '2024-01-01';
```

---

## 6. Partial Indexes cho Performance

### 6.1 Soft Delete Optimization
```sql
-- Active decks only (excluding soft deleted)
CREATE INDEX idx_decks_user_status ON decks(user_id, deleted_at) 
WHERE deleted_at IS NULL;

-- Active cards only
CREATE INDEX idx_cards_deck_status ON cards(deck_id, deleted_at) 
WHERE deleted_at IS NULL;

-- Public decks for marketplace
CREATE INDEX idx_decks_public_only ON decks(visibility) 
WHERE visibility = 'PUBLIC';
```

### 6.2 Due Cards Optimization
```sql
-- Cards that are currently due
CREATE INDEX idx_study_states_due_cards ON study_states(user_id, due_date, card_state) 
WHERE due_date <= CURRENT_TIMESTAMP;

-- Leech cards optimization
CREATE INDEX idx_study_states_is_leech ON study_states(user_id, is_leech) 
WHERE is_leech = TRUE;

-- Learning step tracking
CREATE INDEX idx_study_states_learning_step ON study_states(user_id, current_learning_step) 
WHERE current_learning_step IS NOT NULL;
```

### 6.3 Benefits
- **Smaller Index Size**: Chỉ index relevant rows
- **Faster Updates**: Ít maintenance khi non-matching rows change
- **Better Cache**: More relevant data fits in memory

---

## 7. JSONB Indexes với GIN

### 7.1 JSONB Column Indexes
```sql
-- Deck tags searching
CREATE INDEX idx_decks_tags_gin ON decks USING GIN(tags);

-- Card metadata searching  
CREATE INDEX idx_cards_examples_gin ON cards USING GIN(examples);
CREATE INDEX idx_cards_synonyms_gin ON cards USING GIN(synonyms);
CREATE INDEX idx_cards_antonyms_gin ON cards USING GIN(antonyms);
CREATE INDEX idx_cards_tags_gin ON cards USING GIN(tags);
```

### 7.2 JSONB Query Patterns
```sql
-- Tag containment queries (optimized by GIN)
SELECT * FROM decks WHERE tags @> '["vocabulary"]';
SELECT * FROM cards WHERE examples @> '["example sentence"]';

-- Key existence queries
SELECT * FROM cards WHERE examples ? 'pronunciation';

-- JSON path queries
SELECT * FROM review_sessions 
WHERE session_stats @> '{"difficultyRating": 8}';
```

### 7.3 GIN Index Performance
- **Query Types**: Containment (@>), existence (?), overlap (&&)
- **Update Cost**: Higher than B-Tree, but acceptable for read-heavy workload
- **Storage**: Typically 2-3x the column size

---

## 8. Full-Text Search Indexes

### 8.1 Card Content Search
```sql
-- English full-text search
CREATE INDEX idx_cards_front_fts ON cards 
USING GIN(to_tsvector('english', front));

CREATE INDEX idx_cards_back_fts ON cards 
USING GIN(to_tsvector('english', back));
```

### 8.2 Search Query Optimization
```sql
-- Front content search
SELECT * FROM cards 
WHERE to_tsvector('english', front) @@ plainto_tsquery('english', 'vocabulary');

-- Back content search  
SELECT * FROM cards
WHERE to_tsvector('english', back) @@ plainto_tsquery('english', 'definition');

-- Combined search với ranking
SELECT *, ts_rank(to_tsvector('english', front || ' ' || back), query) as rank
FROM cards, plainto_tsquery('english', 'search term') query
WHERE to_tsvector('english', front || ' ' || back) @@ query
ORDER BY rank DESC;
```

### 8.3 Multi-language Considerations
- **Current**: English-only với 'english' dictionary
- **Future**: Language-specific dictionaries based on `source_language`
- **Fallback**: Simple ILIKE patterns for unsupported languages

---

## 9. Status và Enum Indexes

### 9.1 High-Cardinality Status Fields
```sql
-- Job processing queries
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_type ON jobs(job_type);

-- Report moderation
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_type ON reports(report_type);

-- Study state tracking
CREATE INDEX idx_study_states_card_state ON study_states(card_state);

-- Session status
CREATE INDEX idx_review_sessions_status ON review_sessions(session_status);
CREATE INDEX idx_review_sessions_study_mode ON review_sessions(study_mode);
```

### 9.2 Query Patterns
```sql
-- Admin dashboard queries
SELECT * FROM jobs WHERE status = 'FAILED';
SELECT * FROM reports WHERE status = 'PENDING';

-- Analytics queries
SELECT COUNT(*) FROM study_states WHERE card_state = 'REVIEW';
SELECT * FROM review_sessions WHERE study_mode = 'FLIP';
```

---

## 10. Date/Time Indexes

### 10.1 Temporal Queries
```sql
-- Due date tracking
CREATE INDEX idx_study_states_due_date ON study_states(due_date);

-- Session history
CREATE INDEX idx_review_sessions_date ON review_sessions(session_date);

-- Job queue processing
CREATE INDEX idx_jobs_created_at ON jobs(created_at);

-- Audit trails
CREATE INDEX idx_decks_deleted_at ON decks(deleted_at);
CREATE INDEX idx_cards_deleted_at ON cards(deleted_at);
```

### 10.2 Range Query Optimization
```sql
-- Due cards in next 24 hours
SELECT * FROM study_states 
WHERE due_date BETWEEN NOW() AND NOW() + INTERVAL '1 day';

-- Sessions in date range
SELECT * FROM review_sessions 
WHERE session_date >= '2024-01-01' AND session_date < '2024-02-01';

-- Recent jobs
SELECT * FROM jobs 
WHERE created_at >= NOW() - INTERVAL '1 hour';
```

---

## 11. Index Maintenance Strategy

### 11.1 Monitoring Index Health
```sql
-- Index usage statistics
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes 
ORDER BY idx_scan DESC;

-- Index size monitoring
SELECT indexname, pg_size_pretty(pg_relation_size(indexname::regclass)) as size
FROM pg_indexes 
WHERE schemaname = 'public'
ORDER BY pg_relation_size(indexname::regclass) DESC;

-- Unused indexes detection
SELECT schemaname, tablename, indexname
FROM pg_stat_user_indexes 
WHERE idx_scan = 0 AND idx_tup_read = 0;
```

### 11.2 Maintenance Tasks
- **REINDEX**: Monthly cho high-write tables
- **VACUUM**: Daily cho tables với frequent updates
- **ANALYZE**: After significant data changes
- **Monitor Bloat**: Weekly index bloat checks

### 11.3 Performance Monitoring
- **Query Plans**: Regular EXPLAIN ANALYZE cho slow queries
- **Index Hit Ratio**: Target > 95% cache hit rate
- **Lock Monitoring**: Watch for index lock contention
- **Disk Usage**: Monitor index size growth

---

## 12. Future Indexing Considerations

### 12.1 Scalability Improvements
- **Partitioning**: Time-based partitioning cho review_sessions table
- **Sharding**: User-based sharding khi reach millions of users
- **Materialized Views**: Cho complex analytics queries
- **Covering Indexes**: Include frequently accessed columns

### 12.2 Advanced Features
- **Expression Indexes**: Cho computed fields
- **Conditional Indexes**: More complex WHERE clauses
- **Hash Indexes**: Cho equality-only queries
- **BRIN Indexes**: Cho very large time-series data

---

*Index strategy này optimize cho current query patterns và sẽ evolve based on application usage analytics*
