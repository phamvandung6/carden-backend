"""
Configuration settings for bulk card generation module
"""

import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    # Google Gemini API Configuration
    GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
    GEMINI_MODEL = "gemini-2.0-flash-exp"  # Latest Gemini model for content generation

    # Database Configuration
    DATABASE_URL = os.getenv(
        "DATABASE_URL", "postgresql://user:password@localhost:5432/carden"
    )

    # Card Generation Settings
    MAX_CARDS_PER_BATCH = 15
    DEFAULT_CARD_COUNT = 10
    MAX_CARDS_PER_DECK = 30  # Maximum cards allowed per deck

    # Content Generation Prompts
    CARD_GENERATION_SYSTEM_PROMPT = """
Bạn là một chuyên gia sư phạm ngôn ngữ với kinh nghiệm tạo nội dung học tập hiệu quả.

## Nhiệm vụ
Tạo **10 thẻ học flashcards** chất lượng cao cho chủ đề được cung cấp, từ tiếng Anh sang tiếng Việt.

## Nguyên tắc chọn từ vựng
1. **Ưu tiên từ vựng cốt lõi**: Chọn những từ/cụm từ quan trọng và hay dùng nhất trong chủ đề
2. **Đa dạng loại từ**: Bao gồm danh từ, động từ, tính từ, và cụm từ thông dụng
3. **Phù hợp mức độ**: Tập trung vào từ vựng intermediate (B1-B2) trừ khi có yêu cầu khác
4. **Thực tế và hữu ích**: Chọn từ vựng người học thường gặp trong đời sống

## Yêu cầu cho mỗi thẻ
- **Front (mặt trước)**: Từ/cụm từ tiếng Anh chính xác, không viết hoa không cần thiết
- **Back (mặt sau)**: Bản dịch tiếng Việt chính xác nhất, tự nhiên
- **IPA**: Phiên âm chuẩn theo từ điển Cambridge hoặc Oxford
- **Examples**: 2 ví dụ thực tế, có ngữ cảnh rõ ràng, dễ hiểu
- **Synonyms/Antonyms**: Chỉ thêm khi thật sự hữu ích cho việc học
- **Tags**: Gắn nhãn phù hợp (ví dụ: "family", "relationships", "basic_vocabulary")
- **Difficulty**: EASY (A1-A2), NORMAL (B1-B2), hoặc HARD (C1-C2)

## Hướng dẫn tạo ví dụ tốt
- Sử dụng tình huống thực tế mà người học có thể gặp
- Tránh ví dụ quá đơn giản hoặc nhân tạo
- Đảm bảo ví dụ giúp hiểu rõ cách sử dụng từ
- Độ dài vừa phải (8-15 từ)

## Format trả về
```json
{
  "topic": "[chủ đề được cung cấp]",
  "target_level": "B1-B2",
  "cards": [
    {
      "front": "từ/cụm từ tiếng Anh",
      "back": "nghĩa tiếng Việt",
      "ipa_pronunciation": "/aɪˈpiːeɪ/",
      "examples": [
        "Ví dụ 1 có ngữ cảnh rõ ràng",
        "Ví dụ 2 thực tế và hữu ích"
      ],
      "synonyms": ["từ đồng nghĩa nếu có"],
      "antonyms": ["từ trái nghĩa nếu có"],
      "tags": ["tag1", "tag2"],
      "difficulty": "NORMAL"
    }
  ]
}
```
**Lưu ý quan trọng**: Tập trung vào chất lượng hơn số lượng. Mỗi thẻ phải thật sự hữu ích cho việc học.
    """

    # Language Configuration
    SUPPORTED_LANGUAGES = {
        "en": "English",
        "vi": "Vietnamese",
        "fr": "French",
        "de": "German",
        "es": "Spanish",
        "ja": "Japanese",
        "ko": "Korean",
        "zh": "Chinese",
    }


settings = Settings()
